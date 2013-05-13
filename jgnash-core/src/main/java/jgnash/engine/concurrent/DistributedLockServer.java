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
import io.netty.buffer.BufType;
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
import jgnash.util.EncodeDecode;

import java.util.HashMap;
import java.util.Map;
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

    private ServerBootstrap bootstrap;

    private final ChannelGroup channelGroup = new DefaultChannelGroup("all-connected");

    private final int port;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Map<String, ReadWriteLock> lockMap = new HashMap<>();

    static final String LOCK_ACTION = "lock";

    static final String UNLOCK_ACTION = "unlock";

    static final String LOCK_TYPE_READ = "READ";

    static final String LOCK_TYPE_WRITE = "WRITE";

    public static final String EOL_DELIMITER = "\r\n";

    public DistributedLockServer(final int port) {
        this.port = port;
    }

    private void processMessage(final ChannelHandlerContext ctx, final String message) {

        /** lock_action, lock_id, thread_id, lock_type */
        // unlock,account,3456384756384563,read
        // lock,account,3456384756384563,write

        // decode the message into it's parts
        final String[] strings = EncodeDecode.decodeStringCollection(message).toArray(new String[4]);

        final String action = strings[0];
        final String lockId = strings[1];
        final long remoteThread = Long.parseLong(strings[2]);
        final String lockType = strings[3];

        final ReadWriteLock lock = getLock(lockId);

        try {
            switch (action) {
                case LOCK_ACTION:
                    switch (lockType) {
                        case LOCK_TYPE_READ:
                            lock.lockForRead(remoteThread);
                            break;
                        case LOCK_TYPE_WRITE:
                            lock.lockForWrite(remoteThread);
                            break;
                    }
                    break;
                case UNLOCK_ACTION:
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

            ctx.write(message + EOL_DELIMITER).sync();  // return the message as an acknowledgment lock state has changed
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private ReadWriteLock getLock(final String lockId) {
        ReadWriteLock readWriteLock = lockMap.get(lockId);

        if (readWriteLock == null) {
            readWriteLock = new ReadWriteLock();
            lockMap.put(lockId, readWriteLock);
        }

        return readWriteLock;
    }

    public boolean startServer() {
        boolean result = false;

        bootstrap = new ServerBootstrap();

        try {
            bootstrap.group(new NioEventLoopGroup(), new NioEventLoopGroup())
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new MessageBusRemoteInitializer());

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
            bootstrap.shutdown();

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

    private class MessageBusRemoteInitializer extends ChannelInitializer<SocketChannel> {
        private final StringDecoder DECODER = new StringDecoder();
        private final StringEncoder ENCODER = new StringEncoder(BufType.BYTE);

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
     *
     * The id of the thread is used to map reentrant reads
     *
     *  TODO The id needs to be a sting in combination with a session UUID.  Native thread id's are not very random
     */
    private class ReadWriteLock {

        private final Map<Long, Integer> readingThreads = new HashMap<>();

        private int writeAccesses = 0;
        private int writeRequests = 0;
        private long writingThread = -1;

        synchronized void lockForRead(final long remoteThread) throws InterruptedException {

            while (!canGrantReadAccess(remoteThread)) {
                wait();
            }

            readingThreads.put(remoteThread, (getReadAccessCount(remoteThread) + 1));
        }

        synchronized void lockForWrite(final long remoteThread) throws InterruptedException {
            writeRequests++;

            while (!canGrantWriteAccess(remoteThread)) {
                wait();
            }

            writeRequests--;
            writeAccesses++;   // bump, if greater than 1, then the lock is reentrant
            writingThread = remoteThread;
        }

        synchronized void unlockRead(final long remoteThread) {

            if (!isRemoteReader(remoteThread)) {
                throw new IllegalMonitorStateException("Remote Thread does not hold a read lock on this ReadWriteLock");
            }

            int accessCount = getReadAccessCount(remoteThread);

            if (accessCount == 1) {
                readingThreads.remove(remoteThread);
            } else {
                readingThreads.put(remoteThread, (accessCount - 1));
            }

            notifyAll();
        }

        synchronized void unlockWrite(final long remoteThread) throws InterruptedException {

            if (!isRemoteWriter(remoteThread)) {
                throw new IllegalMonitorStateException("Remote Thread does not hold the write lock on this ReadWriteLock");
            }

            writeAccesses--;

            if (writeAccesses == 0) {
                writingThread = -1;
            }

            notifyAll();
        }

        private boolean canGrantReadAccess(final long remoteThread) {

            if (isRemoteWriter(remoteThread)) { // lock down grade is allowed
                return true;
            }

            if (writingThread != -1) {
                return false;
            }
            if (isRemoteReader(remoteThread)) {
                return true;
            }
            return writeRequests <= 0;

        }

        private boolean canGrantWriteAccess(final long remoteThread) {

            if (!readingThreads.isEmpty()) {
                return false;
            }
            if (writingThread == -1) {
                return true;
            }
            return isRemoteWriter(remoteThread); // reentrant write
        }

        private int getReadAccessCount(final long remoteThread) {
            final Integer accessCount = readingThreads.get(remoteThread);

            if (accessCount == null) {
                return 0;
            }

            return accessCount;
        }

        private boolean isRemoteReader(final long remoteThread) {
            return readingThreads.get(remoteThread) != null;
        }

        private boolean isRemoteWriter(final long remoteThread) {
            return writingThread == remoteThread;
        }
    }
}
