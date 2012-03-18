/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
package jgnash.engine.db4o;

import com.db4o.ObjectContainer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import jgnash.engine.dao.AbstractDAO;
import jgnash.net.ConnectionFactory;

/**
 * Abstract Db4o DAO
 *
 * @author Craig Cavanaugh
 *
 */
public abstract class AbstractDb4oDAO extends AbstractDAO {

    static final int SEMAPHORE_WAIT_TIME = ConnectionFactory.getConnectionTimeout() * 1000; //max wait time in seconds

    static final String GLOBAL_SEMAPHORE = "Lock";

    static final String SEMAPHORE_WARNING = "Could not obtain global semaphore";

    /**
     * Maximum number of changes before a commit will occur
     */
    private static final int MAX_COMMIT_COUNT = 250;

    /**
     * Maximum time in seconds before a commit will occur
     */
    static final int MAX_COMMIT_TIME = 30; // seconds

    static final AtomicInteger commitCount = new AtomicInteger(0);

    private static final ReentrantLock commitLock = new ReentrantLock();

    ObjectContainer container;

    protected boolean isRemote = false;

    AbstractDb4oDAO(final ObjectContainer container, final boolean isRemote) {
        assert container != null;

        this.isRemote = isRemote;
        this.container = container;
    }

    /**
     * Commit the database every 250 requests.
     * <p>
     * If this is a remote client, every request must be committed for
     * correct container lookup by other clients
     */
    final void commit() {
        if (commitCount.getAndIncrement() >= MAX_COMMIT_COUNT || isRemote) {
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
