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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.net.ConnectionFactory;
import jgnash.util.EncryptionManager;
import jgnash.util.NotNull;

/**
 * Lock manager for distributed engine instances.
 *
 * @author Craig Cavanaugh
 */
public class DistributedLockManager implements LockManager {

    private static final Logger logger = Logger.getLogger(DistributedLockManager.class.getName());

    private final Map<String, DistributedReadWriteLock> lockMap = new ConcurrentHashMap<>();

    private final Map<String, CountDownLatch> latchMap = new HashMap<>();

    private final Lock latchLock = new ReentrantLock();

    /**
     * lock_action, lock_id, thread_id, lock_type.
     */
    private static final String PATTERN = "{0},{1},{2},{3}";

    static final String UUID_PREFIX = "UUID:";

    private NioEventLoopGroup eventLoopGroup;

    private final int port;

    private final String host;

    private Channel channel;

    private static final String EOL_DELIMITER = "\r\n";

    private final ExecutorService executorService = Executors.newCachedThreadPool(new LockManagerThreadFactory());

    private EncryptionManager encryptionManager = null;

    /**
     * Unique id to differentiate remote threads.
     */
    private static final String uuid = UUID.randomUUID().toString();

    static {
        logger.setLevel(Level.INFO);
    }

    public DistributedLockManager(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    private String encrypt(final String message) {
        if (encryptionManager != null) {
            return encryptionManager.encrypt(message);
        }
        return message;
    }

    /**
     * Starts the connection with the lock server.
     *
     * @param password connection password
     * @return {@code true} if successful
     */
    public boolean connectToServer(final char[] password) {
        boolean result = false;

        // If a password has been specified, create an EncryptionManager
        if (password != null && password.length > 0) {
            encryptionManager = new EncryptionManager(password);
        }

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

            channel.writeAndFlush(encrypt(UUID_PREFIX + uuid) + EOL_DELIMITER).sync();   // send this channels uuid

            result = true;
            logger.info("Connection made with Distributed Lock Server");
        } catch (final InterruptedException e) {
            logger.log(Level.SEVERE, "Failed to connect to Distributed Lock Server", e);
            disconnectFromServer();
        }

        return result;
    }

    /**
     * Disconnects from the lock server.
     */
    public void disconnectFromServer() {

        try {
            channel.close().sync();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        executorService.shutdown();
        eventLoopGroup.shutdownGracefully();

        eventLoopGroup = null;
        channel = null;

        logger.info("Disconnected from the Distributed Lock Server");
    }

    @Override
    public synchronized ReentrantReadWriteLock getLock(final String lockId) {
        return lockMap.computeIfAbsent(lockId, k -> new DistributedReadWriteLock(lockId));
    }

    private CountDownLatch getLatch(final String lockMessage) {
        latchLock.lock();

        try {
            return latchMap.computeIfAbsent(lockMessage, k -> new CountDownLatch(1));
        } finally {
            latchLock.unlock();
        }
    }

    private void lock(final String lockId, final String type) {
        changeLockState(lockId, type, DistributedLockServer.LOCK);
    }

    private void unlock(final String lockId, final String type) {
        changeLockState(lockId, type, DistributedLockServer.UNLOCK);
    }

    private void changeLockState(final String lockId, final String type, final String lockState) {
        final String threadId = uuid + '-' + Thread.currentThread().getId();
        final String lockMessage = MessageFormat.format(PATTERN, lockState, lockId, threadId, type);

        final CountDownLatch responseLatch = getLatch(lockMessage);

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (responseLatch) {   // synchronize on the lock to prevent concurrency errors

            boolean result = false;

            try {

                // send the message to the server and wait until it if flushed
                channel.writeAndFlush(encrypt(lockMessage) + EOL_DELIMITER).sync();

                for (int i = 0; i < 2; i++) {
                    result = responseLatch.await(45L, TimeUnit.SECONDS);

                    if (!result) {
                        logger.log(Level.WARNING, "Excessive wait for release of the lock latch for: {0}", lockId);
                    } else {
                        break;
                    }
                }

                if (!result) {  // check for a failed release or deadlock
                    logger.log(Level.SEVERE, "Failed to release the lock latch for: {0}", lockId);

                    latchLock.lock();

                    try {
                        responseLatch.countDown();  // force a countdown to occur
                        latchMap.remove(lockId);    // force removal
                    } finally {
                        latchLock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
        }
    }

    private void processMessage(final String lockMessage) {

        final String plainMessage;

        if (encryptionManager != null) {
            plainMessage = encryptionManager.decrypt(lockMessage);
        } else {
            plainMessage = lockMessage;
        }

        //logger.info(plainMessage);

        /* lock_action, lock_id, thread_id, lock_type */
        // unlock,account,3456384756384563,read
        // lock,account,3456384756384563,write

        latchLock.lock();

        try {
            final CountDownLatch responseLatch = getLatch(plainMessage);

            responseLatch.countDown();  // this should release the responseLatch allowing a blocked thread to continue
            latchMap.remove(plainMessage);    // remove the used up latch
        } finally {
            latchLock.unlock();
        }
    }

    private static class LockManagerThreadFactory implements ThreadFactory {
        private final AtomicLong counter = new AtomicLong();

        @Override
        public Thread newThread(final Runnable r) {
            return new Thread(r, "jGnash Distributed Lock Manager " + counter.incrementAndGet());
        }
    }

    private class Initializer extends ChannelInitializer<SocketChannel> {

        @Override
        public void initChannel(final SocketChannel ch) {
            ChannelPipeline pipeline = ch.pipeline();

            // Add the text line codec combination first,
            pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, true, Delimiters.lineDelimiter()));
            pipeline.addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
            pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));

            // and then business logic.
            pipeline.addLast("handler", new ClientHandler());
        }
    }

    /**
     * Handles a client-side channel.
     */
    @ChannelHandler.Sharable
    private class ClientHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
            executorService.submit(() -> {
                processMessage(msg.toString());

                ReferenceCountUtil.release(msg);
            });
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            logger.log(Level.WARNING, "Unexpected exception from downstream.", cause);
            ctx.close();
        }
    }

    private class DistributedReadWriteLock extends ReentrantReadWriteLock {

        private final String lockId;

        private final DistributedReadWriteLock.ReadLock readLock;

        private final DistributedReadWriteLock.WriteLock writeLock;

        DistributedReadWriteLock(final String lockId) {
            super();

            this.lockId = lockId;

            readLock = new DistributedReadWriteLock.ReadLock(this);
            writeLock = new DistributedReadWriteLock.WriteLock(this);
        }

        @Override
        @NotNull
        public ReentrantReadWriteLock.ReadLock readLock() {
            return readLock;
        }

        @Override
        @NotNull
        public ReentrantReadWriteLock.WriteLock writeLock() {
            return writeLock;
        }

        class ReadLock extends ReentrantReadWriteLock.ReadLock {

            ReadLock(final ReentrantReadWriteLock lock) {
                super(lock);
            }

            @Override
            public void lock() {
                DistributedLockManager.this.lock(lockId, DistributedLockServer.LOCK_TYPE_READ);
                super.lock();
            }

            @Override
            public void unlock() {
                DistributedLockManager.this.unlock(lockId, DistributedLockServer.LOCK_TYPE_READ);
                super.unlock();
            }
        }

        class WriteLock extends ReentrantReadWriteLock.WriteLock {

            WriteLock(final ReentrantReadWriteLock lock) {
                super(lock);
            }

            @Override
            public void lock() {
                DistributedLockManager.this.lock(lockId, DistributedLockServer.LOCK_TYPE_WRITE);
                super.lock();
            }

            @Override
            public void unlock() {
                DistributedLockManager.this.unlock(lockId, DistributedLockServer.LOCK_TYPE_WRITE);
                super.unlock();
            }
        }
    }
}
