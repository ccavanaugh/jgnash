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
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.net.ConnectionFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.base64.Base64Decoder;
import io.netty.handler.codec.base64.Base64Encoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

/**
 * Client for sending and receiving files
 *
 * @author Craig Cavanaugh
 */
public class AttachmentTransferClient {
    private static final Logger logger = Logger.getLogger(AttachmentTransferClient.class.getName());

    private NioEventLoopGroup eventLoopGroup;

    private Channel channel;

    /**
     * Starts the connection with the lock server
     *
     * @return <code>true</code> if successful
     */
    public boolean connectToServer(final String host, final int port) {
        boolean result = false;

        final Bootstrap bootstrap = new Bootstrap();

        eventLoopGroup = new NioEventLoopGroup();

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

    private class Initializer extends ChannelInitializer<SocketChannel> {

        @Override
        public void initChannel(final SocketChannel ch) throws Exception {

            ch.pipeline().addLast(
                    new LoggingHandler(),
                    new LineBasedFrameDecoder(8192),

                    new StringEncoder(CharsetUtil.UTF_8),
                    new StringDecoder(CharsetUtil.UTF_8),

                    new Base64Encoder(),
                    new Base64Decoder(),

                    new TransferHandler());
        }
    }

    public void requestFile(final File file) {
        try {
            channel.write(TransferHandler.FILE_REQUEST + file.toString() + '\n').sync();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
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
}
