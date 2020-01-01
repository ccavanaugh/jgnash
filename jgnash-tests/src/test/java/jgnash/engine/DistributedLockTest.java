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
package jgnash.engine;

import jgnash.engine.concurrent.DistributedLockManager;
import jgnash.engine.concurrent.DistributedLockServer;

import io.netty.util.ResourceLeakDetector;
import jgnash.util.LogUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test to validate the Distributed lock manager.
 *
 * @author Craig Cavanaugh
 */
public class DistributedLockTest {

    static final int PORT = 5002;

    DistributedLockServer server;

    DistributedLockManager manager;

    private static final Logger logger = Logger.getLogger(DistributedLockTest.class.getName());

    private final Random random = new Random();

    @BeforeAll
    static void beforeAll() {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        LogUtil.configureLogging();
    }

    @BeforeEach
    public void setUp() {

        server = new DistributedLockServer(PORT);
        assertTrue(server.startServer(EngineFactory.EMPTY_PASSWORD));

        manager = new DistributedLockManager(EngineFactory.LOCALHOST, PORT);
        manager.connectToServer(EngineFactory.EMPTY_PASSWORD);
    }

    @AfterEach
    void tearDown() {
        manager.disconnectFromServer();
        server.stopServer();
    }

    @Test
    void simpleLock() {
        final ReadWriteLock accountLock = manager.getLock("account");
        final ReadWriteLock transactionLock = manager.getLock("transaction");

        try {
            accountLock.readLock().lock();
            transactionLock.writeLock().lock();
            Thread.sleep(1000);
        } catch (final InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            fail();
        } finally {
            accountLock.readLock().unlock();
            transactionLock.writeLock().unlock();
        }

        assertTrue(true);
    }

    @Test
    void multipleReadLocks() {

        final int TIME_BOUND = 3000;

        final int TEST_COUNT = 33;

        final AtomicInteger threadCount = new AtomicInteger();

        class WriteLockTest extends Thread {

            private WriteLockTest() {
                setName("DistributedLockTest WriteLockTest Thread " + threadCount.incrementAndGet());
            }

            @Override
            public void run() {
                final ReentrantReadWriteLock lock = manager.getLock("lockTest");
                logger.info("fetched lock " + getId());


                logger.log(Level.INFO, "locking: {0}", getId());
                lock.readLock().lock();

                try {

                    int num = random.nextInt(TIME_BOUND);

                    try {
                        Thread.sleep(num);
                    } catch (final InterruptedException e) {
                        logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                        fail();
                    }

                } finally {
                    logger.log(Level.INFO, "unlocking: {0}", getId());
                    lock.readLock().unlock();
                }
            }
        }

        List<WriteLockTest> lockTests = new ArrayList<>();

        for (int i = 0; i < TEST_COUNT; i++) {
            WriteLockTest thread = new WriteLockTest();
            lockTests.add(thread);
        }

        for (final WriteLockTest test : lockTests) {
            test.start();
        }

        try {
            for (final WriteLockTest test : lockTests) {
                test.join(TIME_BOUND);
            }
        } catch (final InterruptedException e) {
            Logger.getLogger(DistributedLockTest.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            fail();
        }

        assertTrue(true);
    }

    @Test
    void writeLockTest() throws InterruptedException {

        class ReadLockThread implements Runnable {
            final private ReadWriteLock readWriteLock;

            private ReadLockThread(final ReadWriteLock readWriteLock) {
                this.readWriteLock = readWriteLock;
            }

            @Override
            public void run() {
                try {
                    logger.info("try read lock");
                    readWriteLock.readLock().lock();
                    logger.info("got read lock");
                } finally {
                    logger.info("unlock read lock");
                    readWriteLock.readLock().unlock();
                }
            }
        }

        class WriteLockThread implements Runnable {
            final private ReadWriteLock readWriteLock;

            private WriteLockThread(final ReadWriteLock readWriteLock) {
                this.readWriteLock = readWriteLock;
            }

            @Override
            public void run() {
                try {
                    logger.info("try write lock");
                    readWriteLock.writeLock().lock();
                    logger.info("got write lock");

                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                } finally {
                    logger.info("unlock write lock");
                    readWriteLock.writeLock().unlock();
                }
            }
        }

        ReadWriteLock rwLock = manager.getLock("test-lock");

        Thread writeLockThread = new Thread(new WriteLockThread(rwLock));
        Thread readLockThread = new Thread(new ReadLockThread(rwLock));

        writeLockThread.start();
        Thread.sleep(100);
        readLockThread.start();

        writeLockThread.join();
        readLockThread.join();
    }

    @Test
    void reentrantWriteTest() {
        int count = 0;

        ReadWriteLock lock1 = manager.getLock("reentrant");
        ReadWriteLock lock2 = manager.getLock("reentrant2");

        try {
            lock1.writeLock().lock();
            lock2.writeLock().lock();
            count++;

            lock1.writeLock().lock();
            lock2.writeLock().lock();
            count++;

            lock1.writeLock().lock();
            lock2.writeLock().lock();
            count++;

            lock1.writeLock().lock();
            lock2.writeLock().lock();
            count++;

        } finally {
            lock1.writeLock().unlock();
            lock1.writeLock().unlock();
            lock1.writeLock().unlock();
            lock1.writeLock().unlock();

            lock2.writeLock().unlock();
            lock2.writeLock().unlock();
            lock2.writeLock().unlock();
            lock2.writeLock().unlock();
        }

        assertEquals(4, count);
    }

    @Test
    void reentrantReadTest() {
        int count = 0;

        ReadWriteLock lock = manager.getLock("reentrant");

        try {
            lock.readLock().lock();
            count++;

            lock.readLock().lock();
            count++;

            lock.readLock().lock();
            count++;

            lock.readLock().lock();
            count++;

        } finally {
            lock.readLock().unlock();
            lock.readLock().unlock();
            lock.readLock().unlock();
            lock.readLock().unlock();
        }

        assertEquals(4, count);
    }
}
