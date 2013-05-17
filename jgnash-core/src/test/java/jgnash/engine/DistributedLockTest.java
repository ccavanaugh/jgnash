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
package jgnash.engine;

import jgnash.engine.concurrent.DistributedLockManager;
import jgnash.engine.concurrent.DistributedLockServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test to validate the Distributed lock manager
 *
 * @author Craig Cavanaugh
 */
public class DistributedLockTest {

    static final int PORT = 5002;

    DistributedLockServer server;

    DistributedLockManager manager;

    static Logger logger = Logger.getLogger(DistributedLockTest.class.getName());

    @Before
    public void setUp() {
        server = new DistributedLockServer(PORT);
        server.startServer();

        manager = new DistributedLockManager("localhost", PORT);
        manager.connectToServer();
    }

    @After
    public void tearDown() {
        manager.disconnectFromServer();
        server.stopServer();
    }

    @Test
    public void testStartUp() {

        try {
            Thread.sleep(1000); // helps with sort order of logging because it's so fast
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(true);
    }

    @Test
    public void simpleLock() {

        ReadWriteLock accountLock = manager.getLock("account");
        ReadWriteLock transactionLock = manager.getLock("transaction");

        try {
            accountLock.readLock().lock();
            transactionLock.writeLock().lock();
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            accountLock.readLock().unlock();
            transactionLock.writeLock().unlock();
        }

        assertTrue(true);
    }

    @Test
    public void multipleReadLocks() {

        class WriteLockTest extends Thread {
            @Override
            public void run() {
                ReadWriteLock lock = manager.getLock("lock");

                logger.info("locking: " + getId());
                lock.readLock().lock();

                try {

                    Random random = new Random();
                    int num = random.nextInt(3000);

                    try {
                        Thread.sleep(num);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                } finally {
                    logger.info("unlocking: " + getId());
                    lock.readLock().unlock();
                }
            }
        }

        Thread thread1 = new WriteLockTest();
        Thread thread2 = new WriteLockTest();
        Thread thread3 = new WriteLockTest();
        Thread thread4 = new WriteLockTest();

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        try {
            thread1.join();
            thread2.join();
            thread3.join();
            thread4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(true);
    }

    @Test
    public void writeLockTest() throws InterruptedException {

        class ReadLockThread implements Runnable {
            private ReadWriteLock readWriteLock;

            public ReadLockThread(final ReadWriteLock readWriteLock) {
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
            private ReadWriteLock readWriteLock;

            public WriteLockThread(final ReadWriteLock readWriteLock) {
                this.readWriteLock = readWriteLock;
            }

            @Override
            public void run() {
                try {
                    logger.info("try write lock");
                    readWriteLock.writeLock().lock();
                    logger.info("got write lock");

                    Thread.sleep(3000);
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
        Thread.sleep(500);
        readLockThread.start();

        writeLockThread.join();
        readLockThread.join();
    }

    @Test
    public void reentrantWriteTest() throws InterruptedException {
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

        Thread.sleep(1000);
    }

    @Test
    public void reentrantReadTest() throws InterruptedException {
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

        Thread.sleep(1000);
    }
}
