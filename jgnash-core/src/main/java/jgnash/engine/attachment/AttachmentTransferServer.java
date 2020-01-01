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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.base64.Base64Decoder;
import io.netty.handler.codec.base64.Base64Encoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.util.EncryptionManager;

import static jgnash.engine.attachment.NettyTransferHandler.PATH_MAX;
import static jgnash.engine.attachment.NettyTransferHandler.TRANSFER_BUFFER_SIZE;

/**
 * File server for attachments.
 *
 * @author Craig Cavanaugh
 */
public class AttachmentTransferServer {

    private static final Logger logger = Logger.getLogger(AttachmentTransferServer.class.getName());

    private final int port;

    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    private final ChannelGroup channelGroup = new DefaultChannelGroup("file-server", GlobalEventExecutor.INSTANCE);

    private final Path attachmentPath;

    private EncryptionManager encryptionManager = null;

    public AttachmentTransferServer(final int port, final Path attachmentPath) {
        this.port = port;
        this.attachmentPath = attachmentPath;
    }

    public boolean startServer(final char[] password) {
        boolean result = false;

        // If a password has been specified, create an EncryptionManager
        if (password != null && password.length > 0) {
            encryptionManager = new EncryptionManager(password);
        }

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(eventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    //.handler(new LoggingHandler(LogLevel.INFO))   // for debugging purposes
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        public void initChannel(final SocketChannel ch) {

                            ch.pipeline().addLast(
                                    new DelimiterBasedFrameDecoder(((TRANSFER_BUFFER_SIZE + 2) / 3) * 4 + PATH_MAX,
                                            true, Delimiters.lineDelimiter()),

                                    new StringEncoder(CharsetUtil.UTF_8),
                                    new StringDecoder(CharsetUtil.UTF_8),

                                    new Base64Encoder(),
                                    new Base64Decoder(),

                                    new ServerTransferHandler());
                        }
                    });

            // Start the server.
            final ChannelFuture future = b.bind(port).sync();

            if (future.isDone() && future.isSuccess()) {
                result = true;
                logger.info("File Transfer Server started successfully");
            } else {
                logger.info("Failed to start the File Transfer Server");
            }
        } catch (final InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            stopServer();
        }

        return result;
    }

    public void stopServer() {
        try {
            channelGroup.close().sync();

            eventLoopGroup.shutdownGracefully();

            logger.info("File Transfer Server stopped");
        } catch (final InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private final class ServerTransferHandler extends NettyTransferHandler {

        ServerTransferHandler() {
            super(attachmentPath, encryptionManager);
        }

        @Override
        public void channelActive(final ChannelHandlerContext ctx) {
            channelGroup.add(ctx.channel()); // maintain channels

            logger.log(Level.INFO, "Remote connection from: {0}", ctx.channel().remoteAddress());
        }
    }
}
