/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.AttachmentUtils;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.MessageList;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Handles the details of bi-directional transfer of files between a client and server.
 *
 * @author Craig Cavanaugh
 */
@ChannelHandler.Sharable
public class TransferHandler extends SimpleChannelInboundHandler<String> {

    public static final String FILE_REQUEST = "<FILE_REQUEST>";
    public static final String FILE_STARTS = "<FILE_STARTS>";
    public static final String FILE_ENDS = "<FILE_ENDS>";
    public static final String FILE_CHUNK = "<FILE_CHUNK>";
    public static final String ERROR = "<ERROR>";

    private static final Logger logger = Logger.getLogger(TransferHandler.class.getName());

    private Map<String, Attachment> fileMap = new ConcurrentHashMap<>();

    @Override
    protected void messageReceived(final ChannelHandlerContext ctx, final String msg) throws Exception {

        if (msg.startsWith(FILE_REQUEST)) {
            sendFile(ctx, msg.substring(FILE_REQUEST.length()));
        } else if (msg.startsWith(FILE_STARTS)) {
            openOutputStream(msg.substring(FILE_STARTS.length()));
        } else if (msg.startsWith(FILE_CHUNK)) {
            writeOutputStream(msg.substring(FILE_CHUNK.length()));
        } else if (msg.startsWith(FILE_ENDS)) {
            closeOutputStream(msg.substring(FILE_ENDS.length()));
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        for (Attachment object : fileMap.values()) {
            try {
                object.fileOutputStream.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
        }

        ctx.fireChannelInactive();    // forward to the next handler in the pipeline
    }

    private void sendFile(final ChannelHandlerContext ctx, final String msg) {
        File file = new File(msg);
        if (file.exists()) {

            if (!file.isFile()) {
                ctx.write(ERROR + "Not a file: " + file + '\n');
                return;
            }
            ctx.write(FILE_STARTS + file.getName() + ":" + file.length() + '\n');

            MessageList<String> out = MessageList.newInstance();

            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] bytes = new byte[4096];  // leave room for base 64 expansion

                int bytesRead;

                while ((bytesRead = fileInputStream.read(bytes)) != -1) {
                    if (bytesRead > 0) {
                        out.add(FILE_CHUNK + file.getName() + ':');
                        out.add(new String(bytes, 0, bytesRead) + '\n');
                    }
                }
                out.add(FILE_ENDS + file.getName() + '\n');
            } catch (IOException  e ) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }

            ctx.write(out);
        } else {
            ctx.write(ERROR + "File not found: " + file + '\n');
        }
    }

    private void closeOutputStream(final String msg) {
        Attachment attachment = fileMap.get(msg);

        try {
            attachment.fileOutputStream.close();
            fileMap.remove(msg);
        } catch (final IOException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        if (attachment.file.length() != attachment.fileSize) {
            logger.severe("Invalid file length");
        }
    }

    private void writeOutputStream(final String msg) {
        String[] msgParts = msg.split(":");

        Attachment attachment = fileMap.get(msgParts[0]);

        if (attachment != null) {
            try {
                attachment.fileOutputStream.write(msgParts[1].getBytes());
            } catch (final IOException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
        }
    }

    private void openOutputStream(final String msg) {
        String[] msgParts = msg.split(":");

        final String fileName = msgParts[0];
        final long fileLength = Long.parseLong(msgParts[1]);

        if (AttachmentUtils.createAttachmentDirectory()) {
            final File baseFile = new File(AttachmentUtils.getAttachmentDirectory().toString() + File.separator + fileName);
            try {
                fileMap.put(fileName, new Attachment(baseFile, fileLength));
            } catch (FileNotFoundException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
        } else {
            logger.severe("Could not create attachment directory");
        }

    }

    private static class Attachment {
        final File file;

        final FileOutputStream fileOutputStream;

        final long fileSize;

        private Attachment(final File file, long fileSize) throws FileNotFoundException {
            this.file = file;
            this.fileOutputStream = new FileOutputStream(file);
            this.fileSize = fileSize;
        }
    }
}
