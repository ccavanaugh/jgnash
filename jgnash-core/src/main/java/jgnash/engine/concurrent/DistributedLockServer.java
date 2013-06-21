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
package jgnash.engine.concurrent;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
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
import jgnash.util.EncodeDecode;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Distributed Lock Server
 *
 * @author Craig Cavanaugh
 */
public class DistributedLockServer {

    private static final Logger logger = Logger.getLogger(DistributedLockServer.class.getName());

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private final ChannelGroup channelGroup = new DefaultChannelGroup("lock-server");

    private NioEventLoopGroup eventLoopGroup;

    private final int port;

    private Map<String, ReadWriteLock> lockMap = new HashMap<>();

    private Map<ChannelHandlerContext, String> handlerContextMap = new HashMap<>();

    static final String LOCK = "lock";

    static final String UNLOCK = "unlock";

    static final String LOCK_TYPE_READ = "READ";

    static final String LOCK_TYPE_WRITE = "WRITE";

    public static final String EOL_DELIMITER = "\r\n";

    public DistributedLockServer(final int port) {
        this.port = port;
    }

    private void processMessage(final ChannelHandlerContext ctx, final String message) {

        // Look for a uuid announcement for a channel
        if (message.startsWith(DistributedLockManager.UUID_PREFIX)) {
            handlerContextMap.put(ctx, message.substring(DistributedLockManager.UUID_PREFIX.length()));
            return;
        }

        /** lock_action, lock_id, thread_id, lock_type */
        // unlock,account,1194917570,read
        // lock,account,1194917570,write

        // decode the message into it's parts
        final String[] strings = EncodeDecode.decodeStringCollection(message).toArray(new String[4]);

        final String action = strings[0];
        final String lockId = strings[1];
        final String remoteThread = strings[2];
        final String lockType = strings[3];

        final ReadWriteLock lock = getLock(lockId);

        try {

            // request a lock or unlock.  This may block
            switch (action) {
                case LOCK:
                    switch (lockType) {
                        case LOCK_TYPE_READ:
                            lock.lockForRead(remoteThread);
                            break;
                        case LOCK_TYPE_WRITE:
                            lock.lockForWrite(remoteThread);
                            break;
                    }
                    break;
                case UNLOCK:
                    switch (lockType) {
                        case LOCK_TYPE_READ:
                            lock.unlockRead(remoteThread);
                            break;
                        case LOCK_TYPE_WRITE:
                            lock.unlockWrite(remoteThread);
                            break;
                    }
                    break;
            }

            // return the message as an acknowledgment lock state has changed
            if (ctx.channel().isOpen()) {
                ctx.write(message + EOL_DELIMITER).sync();
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private ReadWriteLock getLock(final String lockId) {
        ReadWriteLock readWriteLock = lockMap.get(lockId);

        if (readWriteLock == null) {
            readWriteLock = new ReadWriteLock(lockId);
            lockMap.put(lockId, readWriteLock);
        }

        return readWriteLock;
    }

    public boolean startServer() {
        boolean result = false;

        eventLoopGroup = new NioEventLoopGroup();

        final ServerBootstrap bootstrap = new ServerBootstrap();

        try {
            bootstrap.group(eventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new Initializer());

            final ChannelFuture future = bootstrap.bind(port);
            future.sync();

            if (future.isDone() && future.isSuccess()) {
                logger.info("Distributed Lock Server started successfully");
                result = true;
            } else {
                logger.info("Failed to start the Distributed Lock Server");
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
            executorService.shutdown();
            eventLoopGroup.shutdownGracefully();

            eventLoopGroup = null;

            logger.info("Distributed Lock Server Stopped");
        } catch (final InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }


    @ChannelHandler.Sharable
    private class ServerHandler extends ChannelInboundMessageHandlerAdapter<String> {

        @Override
        public void channelActive(final ChannelHandlerContext ctx) throws Exception {
            channelGroup.add(ctx.channel()); // maintain channels

            logger.log(Level.INFO, "Remote connection from: {0}", ctx.channel().remoteAddress().toString());
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            logger.log(Level.INFO, "Remote connection {0} closed", ctx.channel().remoteAddress().toString());

            String uuid = handlerContextMap.get(ctx);

            // Search through the lock map and remove any stale locks
            if (uuid != null) {
                for (ReadWriteLock readWriteLock : lockMap.values()) {  // look at every lock
                    for (String remoteThread : readWriteLock.readingThreads.keySet()) { // if the remoteThread starts with the uuid, request a cleanup
                        if (remoteThread.startsWith(uuid)) {    // cleanup a stale lock
                            readWriteLock.cleanupStaleThread(remoteThread);
                        }
                    }

                    if (readWriteLock.writingThread != null && readWriteLock.writingThread.startsWith(uuid)) {
                        readWriteLock.cleanupStaleThread(readWriteLock.writingThread);
                    }
                }
            }

            handlerContextMap.remove(ctx);
            channelGroup.remove(ctx.channel());
            super.channelInactive(ctx);
        }

        @Override
        public void messageReceived(final ChannelHandlerContext ctx, final String message) throws Exception {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    processMessage(ctx, message);
                }
            });
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
            logger.log(Level.WARNING, "Unexpected exception from downstream.", cause);
            ctx.close();
        }
    }

    private class Initializer extends ChannelInitializer<SocketChannel> {
        private final StringDecoder DECODER = new StringDecoder(CharsetUtil.UTF_8);
        private final StringEncoder ENCODER = new StringEncoder(CharsetUtil.UTF_8);

        private final ServerHandler SERVER_HANDLER = new ServerHandler();

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

    /**
     * Reentrant Read Write lock.
     * <p/>
     * A unique integer must be supplied to identify the thread instead of the current thread.
     */
    private static class ReadWriteLock {

        private final String id;

        /**
         * The key is the uuid of the manager plus the remote thread id
         * <p/>
         * uuid-integer
         */
        private final Map<String, Integer> readingThreads = new ConcurrentHashMap<>();

        private int writeAccesses = 0;
        private int writeRequests = 0;
        private String writingThread = null;

        private ReadWriteLock(final String id) {
            this.id = id;
        }

        synchronized void cleanupStaleThread(final String remoteThread) {
            if (readingThreads.containsKey(remoteThread)) {
                unlockRead(remoteThread);
                logger.warning("Removed a stale read lock for: " + id);
            }

            if (writingThread != null && writingThread.equals(remoteThread)) {
                unlockWrite(remoteThread);
                logger.warning("Removed a stale write lock for: " + id);
            }
        }

        synchronized void lockForRead(final String remoteThread) throws InterruptedException {

            while (!canGrantReadAccess(remoteThread)) {
                wait();
            }

            readingThreads.put(remoteThread, (getReadHoldCount(remoteThread) + 1));
        }

        synchronized void lockForWrite(final String remoteThread) throws InterruptedException {
            writeRequests++;

            while (!canGrantWriteAccess(remoteThread)) {
                wait();
            }

            writeRequests--;
            writeAccesses++;   // bump, if greater than 1, then the lock is reentrant
            writingThread = remoteThread;
        }

        synchronized void unlockRead(final String remoteThread) {

            if (!isReadLockedByCurrentThread(remoteThread)) {
                throw new IllegalMonitorStateException("Remote Thread: " + remoteThread + " does not hold a read lock for: " + id);
            }

            int holdCount = getReadHoldCount(remoteThread);

            if (holdCount == 1) {
                readingThreads.remove(remoteThread);
            } else {
                readingThreads.put(remoteThread, (holdCount - 1));
            }

            notifyAll();
        }

        synchronized void unlockWrite(final String remoteThread) {

            if (!isWriteLockedByCurrentThread(remoteThread)) {
                throw new IllegalMonitorStateException("Remote Thread: " + remoteThread + " does not hold the write lock for: " + id);
            }

            writeAccesses--;

            if (writeAccesses == 0) {
                writingThread = null;
            }

            notifyAll();
        }

        private synchronized boolean canGrantReadAccess(final String remoteThread) {

            if (isWriteLockedByCurrentThread(remoteThread)) { // lock down grade is allowed
                return true;
            }

            if (writingThread != null) {
                return false;
            }
            if (isReadLockedByCurrentThread(remoteThread)) {
                return true;
            }

            return writeRequests <= 0;
        }

        private synchronized boolean canGrantWriteAccess(final String remoteThread) {

            if (!readingThreads.isEmpty()) {
                return false;
            }
            if (writingThread == null) {
                return true;
            }
            return isWriteLockedByCurrentThread(remoteThread); // reentrant write
        }

        private synchronized int getReadHoldCount(final String remoteThread) {
            final Integer accessCount = readingThreads.get(remoteThread);

            if (accessCount == null) {
                return 0;
            }

            return accessCount;
        }

        private synchronized boolean isReadLockedByCurrentThread(final String remoteThread) {
            return readingThreads.get(remoteThread) != null;
        }

        private synchronized boolean isWriteLockedByCurrentThread(final String remoteThread) {
            if (writingThread != null) {
                return writingThread.equals(remoteThread);
            }
            return false;
        }
    }
}
