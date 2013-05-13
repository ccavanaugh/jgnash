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
    public void multipleWriteLocks() {

        class WriteLockTest extends Thread {
            @Override
            public void run() {
                ReadWriteLock lock = manager.getLock("lock");

                lock.writeLock().lock();

                try {

                    Random random = new Random();
                    int num = random.nextInt(3000);

                    try {
                        Thread.sleep(num);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        Thread thread1 = new  WriteLockTest();
        Thread thread2 = new  WriteLockTest();
        Thread thread3 = new  WriteLockTest();
        Thread thread4 = new  WriteLockTest();

        thread1.run();
        thread2.run();
        thread3.run();
        thread4.run();

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
}
