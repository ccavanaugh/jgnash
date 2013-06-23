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
package jgnash.engine.message;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.DataStoreType;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.MessageList;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * Message bus server for remote connections
 *
 * @author Craig Cavanaugh
 */
public class MessageBusServer {

    private static final Logger logger = Logger.getLogger(MessageBusServer.class.getName());

    public static final String PATH_PREFIX = "<PATH>";

    public static final String DATA_STORE_TYPE_PREFIX = "<TYPE>";

    public static final String EOL_DELIMITER = "\r\n";

    private int port = 0;

    private String dataBasePath = "";

    private String dataStoreType = "";

    private NioEventLoopGroup eventLoopGroup;

    private final ReadWriteLock rwl = new ReentrantReadWriteLock(true);

    private final Set<LocalServerListener> listeners = new HashSet<>();

    private final ChannelGroup channelGroup = new DefaultChannelGroup("all-connected", GlobalEventExecutor.INSTANCE);

    private EncryptionFilter filter;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    static {
        logger.setLevel(Level.INFO);
    }

    public MessageBusServer(final int port) {
        this.port = port;
    }

    public boolean startServer(final DataStoreType dataStoreType, final String dataBasePath, final char[] password) {
        boolean result = false;

        logger.info("Starting message bus server");

        this.dataBasePath = dataBasePath;
        this.dataStoreType = dataStoreType.name();

        boolean useSSL = Boolean.parseBoolean(System.getProperties().getProperty("ssl"));

        // If a user and password has been specified, enable an encryption filter
        if (useSSL && password != null && password.length > 0) {
            filter = new EncryptionFilter(password);
        }

        eventLoopGroup = new NioEventLoopGroup();

        final ServerBootstrap bootstrap = new ServerBootstrap();

        try {
            bootstrap.group(eventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new MessageBusRemoteInitializer());

            final ChannelFuture future = bootstrap.bind(port);
            future.sync();

            if (future.isDone() && future.isSuccess()) {
                logger.info("Message Bus Server started successfully");
                result = true;
            } else {
                logger.info("Failed to start the Message Bus Server");
            }
        } catch (final InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            stopServer();
        }

        return result;
    }

    public void stopServer() {
        rwl.writeLock().lock();

        try {
            channelGroup.close().sync();

            executorService.shutdown();
            eventLoopGroup.shutdownGracefully();

            eventLoopGroup = null;

            listeners.clear();

            logger.info("MessageBusServer Stopped");
        } catch (final InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    public void addLocalListener(final LocalServerListener listener) {
        rwl.writeLock().lock();

        try {
            listeners.add(listener);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    public void removeLocalListener(final LocalServerListener listener) {
        rwl.writeLock().lock();

        try {
            listeners.remove(listener);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    /**
     * Utility method to encrypt a message
     *
     * @param message message to encrypt
     * @return encrypted message
     */
    private String encrypt(final String message) {
        if (filter != null) {
            return filter.encrypt(message);
        }
        return message;
    }

    private String decrypt(final String message) {
        String plainMessage;

        if (filter != null) {
            plainMessage = filter.decrypt(message);
        } else {
            plainMessage = message;
        }

        return plainMessage;
    }

    private class MessageBusRemoteInitializer extends ChannelInitializer<SocketChannel> {
        private final StringDecoder DECODER = new StringDecoder(CharsetUtil.UTF_8);
        private final StringEncoder ENCODER = new StringEncoder(CharsetUtil.UTF_8);

        private final MessageBusServerHandler SERVER_HANDLER = new MessageBusServerHandler();

        @Override
        public void initChannel(final SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();

            // Add the text line codec combination first,
            pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, true, Delimiters.lineDelimiter()));

            // the encoder and decoder are static as these are sharable
            pipeline.addLast("decoder", DECODER);
            pipeline.addLast("encoder", ENCODER);

            // and then business logic.
            pipeline.addLast("handler", SERVER_HANDLER);
        }
    }

    @ChannelHandler.Sharable
    private class MessageBusServerHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(final ChannelHandlerContext ctx) throws Exception {
            channelGroup.add(ctx.channel()); // maintain channels

            logger.log(Level.INFO, "Remote connection from: {0}", ctx.channel().remoteAddress().toString());

            // Inform the client what they are talking with so they can establish a correct database url
            ctx.write(encrypt(PATH_PREFIX + dataBasePath) + EOL_DELIMITER);
            ctx.write(encrypt(DATA_STORE_TYPE_PREFIX + dataStoreType) + EOL_DELIMITER);
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            channelGroup.remove(ctx.channel());
            super.channelInactive(ctx);
        }

        @Override
        public void messageReceived(final ChannelHandlerContext ctx, final MessageList<Object> messageList) throws Exception {
            for (final Object msg : messageList) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        processMessage(msg.toString());
                    }
                });
            }
        }

        private void processMessage(final String message) {
            final String plainMessage = decrypt(message);

            rwl.readLock().lock();

            try {
                channelGroup.write(encrypt(plainMessage) + EOL_DELIMITER).sync();

                // Local listeners do not receive encrypted messages
                for (LocalServerListener listener : listeners) {
                    listener.messagePosted(plainMessage);
                }

                logger.log(Level.FINE, "Broadcast: {0}", plainMessage);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            } finally {
                rwl.readLock().unlock();
            }
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
            logger.log(Level.WARNING, "Unexpected exception from downstream.", cause);
            ctx.close();
        }
    }
}
