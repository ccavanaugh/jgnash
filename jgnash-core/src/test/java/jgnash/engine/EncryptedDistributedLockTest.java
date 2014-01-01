/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
import jgnash.util.EncryptionManager;

import io.netty.util.ResourceLeakDetector;
import org.junit.Before;

/**
 * Test to validate the Distributed lock manager
 *
 * @author Craig Cavanaugh
 */
public class EncryptedDistributedLockTest extends DistributedLockTest {

    @Before
    @Override
    public void setUp() {
        final char[] password = new char[]{'P', 'a', 's', 's', 'w', 'o', 'r', 'd'};

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        System.setProperty(EncryptionManager.ENCRYPTION_FLAG, "true");
        System.setProperty("ssl", "true");

        server = new DistributedLockServer(PORT);
        server.startServer(password);

        manager = new DistributedLockManager("localhost", PORT);
        manager.connectToServer(password);
    }
}
