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
package jgnash.engine.concurrent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.net.ConnectionFactory;
import jgnash.util.EncodeDecode;
import jgnash.util.EncryptionManager;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
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
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

import static jgnash.net.ConnectionFactory.MILLIS_PER_SECOND;

/**
 * Distributed Lock Server.
 *
 * @author Craig Cavanaugh
 */
public class DistributedLockServer {

    private static final Logger logger = Logger.getLogger(DistributedLockServer.class.getName());

    // there needs to be to 2 threads to ensure order of operations and allow for unlocking without blocking
    private final ExecutorService executorService = Executors.newFixedThreadPool(2, new LockServerThreadFactory());

    private final ChannelGroup channelGroup = new DefaultChannelGroup("lock-server", GlobalEventExecutor.INSTANCE);

    private NioEventLoopGroup eventLoopGroup;

    private final int port;

    private final Map<String, ReadWriteLock> lockMap = new HashMap<>();

    private final Map<ChannelHandlerContext, String> handlerContextMap = new HashMap<>();

    static final String LOCK = "lock";

    static final String UNLOCK = "unlock";

    static final String LOCK_TYPE_READ = "READ";

    static final String LOCK_TYPE_WRITE = "WRITE";

    private static final String EOL_DELIMITER = "\r\n";

    private EncryptionManager encryptionManager = null;

    public DistributedLockServer(final int port) {
        this.port = port;
    }

    private String encrypt(final String message) {
        if (encryptionManager != null) {
            return encryptionManager.encrypt(message);
        }
        return message;
    }

    private void processMessage(final ChannelHandlerContext ctx, final String msg) {

        final String message;

        if (encryptionManager != null) {
            message = encryptionManager.decrypt(msg);
        } else {
            message = msg;
        }

        // Look for a uuid announcement for a channel
        if (message.startsWith(DistributedLockManager.UUID_PREFIX)) {
            handlerContextMap.put(ctx, message.substring(DistributedLockManager.UUID_PREFIX.length()));
            return;
        }

        /* lock_action, lock_id, thread_id, lock_type */
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
                        default:
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
                        default:
                            break;
                    }
                    break;
            }

            // return the message as an acknowledgment lock state has changed
            if (ctx.channel().isOpen()) {
                ctx.writeAndFlush(encrypt(message) + EOL_DELIMITER).sync();
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private ReadWriteLock getLock(final String lockId) {
        return lockMap.computeIfAbsent(lockId, k -> new ReadWriteLock(lockId));
    }

    public boolean startServer(final char[] password) {
        boolean result = false;

        // If a password has been specified, create an EncryptionManager
        if (password != null && password.length > 0) {
            encryptionManager = new EncryptionManager(password);
        }

        eventLoopGroup = new NioEventLoopGroup();

        final ServerBootstrap bootstrap = new ServerBootstrap();

        try {
            bootstrap.group(eventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new Initializer())
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

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

    private static class LockServerThreadFactory implements ThreadFactory {
        private final AtomicLong counter = new AtomicLong();

        @Override
        public Thread newThread(final Runnable r) {
            return new Thread(r, "jGnash Distributed Lock Server " + counter.incrementAndGet());
        }
    }


    @ChannelHandler.Sharable
    private class ServerHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(final ChannelHandlerContext ctx) {
            channelGroup.add(ctx.channel()); // maintain channels

            logger.log(Level.INFO, "Remote connection from: {0}", ctx.channel().remoteAddress().toString());
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            logger.log(Level.INFO, "Remote connection {0} closed", ctx.channel().remoteAddress().toString());

            final String uuid = handlerContextMap.get(ctx);

            // Search through the lock map and remove any stale locks
            if (uuid != null) {
                for (ReadWriteLock readWriteLock : lockMap.values()) {  // look at every lock

                    // if the remoteThread starts with the uuid, request a cleanup
                    // cleanup a stale lock
                    readWriteLock.readingThreads.keySet().stream().filter(remoteThread ->
                            remoteThread.startsWith(uuid)).forEach(readWriteLock::cleanupStaleThread);

                    if (readWriteLock.hasWriteThread(uuid)) {
                        readWriteLock.cleanupStaleWriteThread();
                    }
                }
            }

            handlerContextMap.remove(ctx);
            channelGroup.remove(ctx.channel());
            super.channelInactive(ctx);
        }

        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
            executorService.submit(() -> {
                processMessage(ctx, msg.toString());
                ReferenceCountUtil.release(msg);
            });
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
            logger.log(Level.WARNING, "Unexpected exception from downstream.", cause);
            ctx.close();
        }
    }

    private class Initializer extends ChannelInitializer<SocketChannel> {

        @Override
        public void initChannel(final SocketChannel ch) {
            ChannelPipeline pipeline = ch.pipeline();

            // Add the text line codec combination first,
            pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, true, Delimiters.lineDelimiter()));

            // the encoder and decoder are static as these are sharable
            pipeline.addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
            pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));

            // and then business logic.
            pipeline.addLast("handler", new ServerHandler());
        }
    }

    /**
     * Reentrant Read Write lock.
     * <p>
     * A unique integer must be supplied to identify the thread instead of the current thread.
     */
    private static class ReadWriteLock {

        private final String id;

        /**
         * The key is the uuid of the manager plus the remote thread id.
         * <p>
         * uuid-integer
         */
        private final Map<String, Integer> readingThreads = new ConcurrentHashMap<>();

        private int writeAccesses = 0;
        private int writeRequests = 0;
        private String writingThread = null;

        private ReadWriteLock(final String id) {
            this.id = id;
        }

        synchronized boolean hasWriteThread(final String id) {
            boolean result = false;

            if (writingThread != null) {
                result = writingThread.startsWith(id);
            }

            return result;
        }

        synchronized void cleanupStaleThread(final String remoteThread) {
            if (readingThreads.containsKey(remoteThread)) {
                unlockRead(remoteThread);
                logger.log(Level.WARNING, "Removed a stale read lock for: {0}", id);
            }

            if (writingThread != null && writingThread.equals(remoteThread)) {
                unlockWrite(remoteThread);
                logger.log(Level.WARNING, "Removed a stale write lock for: {0}", id);
            }
        }

        synchronized void cleanupStaleWriteThread() {
            if (readingThreads.containsKey(writingThread)) {
                unlockRead( writingThread);
                logger.log(Level.WARNING, "Removed a stale read lock for: {0}", id);
            }

            if (writingThread != null) {
                unlockWrite( writingThread);
                logger.log(Level.WARNING, "Removed a stale write lock for: {0}", id);
            }
        }

        synchronized void lockForRead(final String remoteThread) throws InterruptedException {

            while (!canGrantReadAccess(remoteThread)) {
                // wait for a maximum of 2X the network timout
                wait((long) ConnectionFactory.getConnectionTimeout() * MILLIS_PER_SECOND * 2);
            }

            readingThreads.put(remoteThread, (getReadHoldCount(remoteThread) + 1));
        }

        synchronized void lockForWrite(final String remoteThread) throws InterruptedException {
            writeRequests++;

            while (!canGrantWriteAccess(remoteThread)) {
                // wait for a maximum of 2X the network timout
                wait((long) ConnectionFactory.getConnectionTimeout() * MILLIS_PER_SECOND * 2);
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

            return Objects.requireNonNullElse(accessCount, 0);

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
