/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.base64.Base64Decoder;
import io.netty.handler.codec.base64.Base64Encoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.net.ConnectionFactory;
import jgnash.util.EncryptionManager;

import static jgnash.engine.attachment.NettyTransferHandler.*;

/**
 * Client for sending and receiving files
 *
 * @author Craig Cavanaugh
 */
class AttachmentTransferClient {
    private static final Logger logger = Logger.getLogger(AttachmentTransferClient.class.getName());

    private final Path tempDirectory;

    private NioEventLoopGroup eventLoopGroup;

    private Channel channel;

    private NettyTransferHandler transferHandler;

    private EncryptionManager encryptionManager = null;

    public AttachmentTransferClient(final Path tempPath) {
        tempDirectory = tempPath;
    }

    /**
     * Starts the connection with the lock server.
     *
     * @param host remote host
     * @param port connection port
     * @param password connection password
     * @return {@code true} if successful
     */
    public boolean connectToServer(final String host, final int port, final char[] password) {
        boolean result = false;

        // If a password has been specified, create an EncryptionManager
        if (password != null && password.length > 0) {
            encryptionManager = new EncryptionManager(password);
        }

        final Bootstrap bootstrap = new Bootstrap();

        eventLoopGroup = new NioEventLoopGroup();

        transferHandler = new NettyTransferHandler(tempDirectory, encryptionManager);

        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new Initializer())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ConnectionFactory.getConnectionTimeout() * 1000)
                .option(ChannelOption.SO_KEEPALIVE, true);

        try {
            // Start the connection attempt.
            channel = bootstrap.connect(host, port).sync().channel();

            result = true;
            logger.info("Connection made with File Transfer Server");
        } catch (final InterruptedException e) {
            logger.log(Level.SEVERE, "Failed to connect to the File Transfer Server", e);
            disconnectFromServer();
        }

        return result;
    }

    private String encrypt(final String message) {
        if (encryptionManager != null) {
            return encryptionManager.encrypt(message);
        }
        return message;
    }

    public void requestFile(final Path file) {
        try {
            channel.writeAndFlush(encrypt(FILE_REQUEST + file) + EOL_DELIMITER).sync();
        } catch (final InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    public void deleteFile(final String attachment) {
        try {
            channel.writeAndFlush(encrypt(DELETE + Paths.get(attachment).getFileName()) + EOL_DELIMITER).sync();
        } catch (final InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    public Future<Void> sendFile(final File file) {
        if (transferHandler != null) {
            return transferHandler.sendFile(channel, file.getAbsolutePath());
        }

        return null;
    }

    /**
     * Disconnects from the lock server
     */
    public void disconnectFromServer() {

        try {
            channel.close().sync();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        eventLoopGroup.shutdownGracefully();

        eventLoopGroup = null;
        channel = null;

        logger.info("Disconnected from the File Transfer Server");
    }

    private class Initializer extends ChannelInitializer<SocketChannel> {

        @Override
        public void initChannel(final SocketChannel ch) throws Exception {

            ch.pipeline().addLast(
                    new DelimiterBasedFrameDecoder(((TRANSFER_BUFFER_SIZE + 2) / 3) * 4 + PATH_MAX,
                            true, Delimiters.lineDelimiter()),

                    new StringEncoder(CharsetUtil.UTF_8),
                    new StringDecoder(CharsetUtil.UTF_8),

                    new Base64Encoder(),
                    new Base64Decoder(),

                    transferHandler);
        }
    }
}
