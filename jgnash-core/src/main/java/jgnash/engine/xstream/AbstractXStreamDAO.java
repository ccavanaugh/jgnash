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
package jgnash.engine.xstream;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import jgnash.engine.StoredObject;
import jgnash.engine.dao.AbstractDAO;

/**
 * Simple object container for StoredObjects that reads and writes and xml file
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractXStreamDAO extends AbstractDAO {

    /**
     * Maximum time in seconds before a commit will occur
     */
    static final int MAX_COMMIT_TIME = 30; // seconds

    static final AtomicInteger commitCount = new AtomicInteger(0);

    AbstractXStreamContainer container;

    private static final ReentrantLock commitLock = new ReentrantLock();

    private static final int MAX_COMMIT_COUNT = 250;

    AbstractXStreamDAO(final AbstractXStreamContainer container) {
        assert container != null;

        this.container = container;
    }

    public StoredObject getObjectByUuid(final String uuid) {
        return container.get(uuid);
    }

    final void commit() {
        if (commitCount.getAndIncrement() >= MAX_COMMIT_COUNT) {
            commitAndReset();
        }
    }

    final void commitAndReset() {
        commitLock.lock();

        try {
            commitCount.set(0);
            container.commit();
        } finally {
            commitLock.unlock();
        }
    }
}
