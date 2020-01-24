/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.engine.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.AttachmentUtils;
import jgnash.util.EncryptionManager;
import jgnash.util.FileUtils;
import jgnash.util.Nullable;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Handles the details of bi-directional transfer of files between a client and server.
 *
 * @author Craig Cavanaugh
 */
@ChannelHandler.Sharable
class NettyTransferHandler extends SimpleChannelInboundHandler<String> {

    static final String FILE_REQUEST = "<FILE_REQUEST>";

    static final String DELETE = "<DELETE>";

    static final String EOL_DELIMITER = "\r\n";

    private static final String FILE_STARTS = "<FILE_STARTS>";

    private static final String FILE_ENDS = "<FILE_ENDS>";

    private static final String FILE_CHUNK = "<FILE_CHUNK>";

    private static final String ERROR = "<ERROR>";

    private static final Logger logger = Logger.getLogger(NettyTransferHandler.class.getName());

    static final int TRANSFER_BUFFER_SIZE = 1024; // too large can break netty... bug?

    static final int PATH_MAX = 4096;

    private final Map<String, Attachment> fileMap = new ConcurrentHashMap<>();

    private final Path attachmentPath;

    private final EncryptionManager encryptionManager;

    /**
     * Netty Handler.  The specified path may be a temporary location for clients or a persistent location for servers.
     *
     * @param attachmentPath Path for attachments.
     * @param encryptionManager encryption manager instance
     */
    NettyTransferHandler(final Path attachmentPath, @Nullable final EncryptionManager encryptionManager) {
        Objects.requireNonNull(attachmentPath);

        this.attachmentPath = attachmentPath;
        this.encryptionManager = encryptionManager;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final String msg) {

        final String plainMessage;

        if (encryptionManager != null) {
            plainMessage = encryptionManager.decrypt(msg);
        } else {
            plainMessage = msg;
        }

        if (plainMessage.startsWith(FILE_REQUEST)) {
            sendFile(ctx, attachmentPath + FileUtils.SEPARATOR + plainMessage.substring(FILE_REQUEST.length()));
        } else if (plainMessage.startsWith(FILE_STARTS)) {
            openOutputStream(plainMessage.substring(FILE_STARTS.length()));
        } else if (plainMessage.startsWith(FILE_CHUNK)) {
            writeOutputStream(plainMessage.substring(FILE_CHUNK.length()));
        } else if (plainMessage.startsWith(FILE_ENDS)) {
            closeOutputStream(plainMessage.substring(FILE_ENDS.length()));
        } else if (plainMessage.startsWith(DELETE)) {
            deleteFile(plainMessage.substring(FILE_ENDS.length()));
        }
    }

    private void deleteFile(final String fileName) {
        Path path = Paths.get(attachmentPath + FileUtils.SEPARATOR + fileName);

        if (Files.exists(path)) {
            try {
                Files.delete(path);
            } catch (final IOException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        for (Attachment object : fileMap.values()) {
            try {
                object.outputStream.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
        }

        ctx.fireChannelInactive();    // forward to the next handler in the pipeline
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        logger.log(Level.WARNING, "Unexpected exception from downstream.", cause);
        ctx.close();
    }

    private String encrypt(final String message) {
        if (encryptionManager != null) {
            return encryptionManager.encrypt(message);
        }
        return message;
    }

    /**
     * Sends a file across the channel. The Fu
     * @param channel  Channel to send file through
     * @param fileName the file name
     * @return the future of the potentially asynchronous send is returned. A null value is returned if fileName is a path.
     */
    Future<Void> sendFile(final Channel channel, final String fileName) {
        Future<Void> future = null;

        Path path = Paths.get(fileName);

        if (Files.exists(path)) {

            if (Files.isDirectory(path)) {
                channel.writeAndFlush(encrypt(ERROR + "Not a file: " + path) + EOL_DELIMITER);
                return null;
            }

            try (final InputStream inputStream = Files.newInputStream(path)) {
                channel.writeAndFlush(encrypt(FILE_STARTS + path.getFileName() + ":" + Files.size(path)) + EOL_DELIMITER);

                byte[] bytes = new byte[TRANSFER_BUFFER_SIZE];  // leave room for base 64 expansion

                int bytesRead;

                while ((bytesRead = inputStream.read(bytes)) != -1) {
                    if (bytesRead > 0) {
                        channel.write(encrypt(FILE_CHUNK + path.getFileName() + ':' +
                                Base64.getEncoder().encodeToString(Arrays.copyOfRange(bytes, 0, bytesRead)))
                                + EOL_DELIMITER);
                    }
                }
                future = channel.writeAndFlush(encrypt(FILE_ENDS + path.getFileName()) + EOL_DELIMITER).sync();

            } catch (IOException | InterruptedException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                Thread.currentThread().interrupt();
            }
        } else {
            try {
                future = channel.writeAndFlush(encrypt(ERROR + "File not found: " + path) + EOL_DELIMITER).sync();
            } catch (final InterruptedException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                Thread.currentThread().interrupt();
            }
            logger.log(Level.WARNING, "File not found: {0}", path);
        }

        return future;
    }

    private void sendFile(final ChannelHandlerContext ctx, final String msg) {
        sendFile(ctx.channel(), msg);
    }

    private void closeOutputStream(final String msg) {
        Attachment attachment = fileMap.get(msg);

        try {
            attachment.outputStream.close();
            fileMap.remove(msg);
        } catch (final IOException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        if (attachment.path.toFile().length() != attachment.fileSize) {
            logger.severe("Invalid file length");
        }
    }

    private void writeOutputStream(final String msg) {
        String[] msgParts = msg.split(":");

        Attachment attachment = fileMap.get(msgParts[0]);

        if (attachment != null) {
            try {
                attachment.outputStream.write(Base64.getDecoder().decode(msgParts[1]));
            } catch (final IOException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
        }
    }

    private void openOutputStream(final String msg) {
        String[] msgParts = msg.split(":");

        final String fileName = msgParts[0];
        final long fileLength = Long.parseLong(msgParts[1]);

        final Path filePath = Paths.get(attachmentPath + FileUtils.SEPARATOR + fileName);

        // Lazy creation of the attachment path if needed
        if (!AttachmentUtils.createAttachmentDirectory(attachmentPath)) {
            logger.severe("Unable to find or create the attachment directory");
            return;
        }

        try {
            fileMap.put(fileName, new Attachment(filePath, fileLength));
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private static class Attachment {
        final Path path;

        final OutputStream outputStream;

        final long fileSize;

        private Attachment(final Path path, long fileSize) throws IOException {
            this.path = path;
            outputStream = Files.newOutputStream(path);

            this.fileSize = fileSize;
        }
    }
}
