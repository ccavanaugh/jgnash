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
package jgnash.engine.xstream;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import jgnash.engine.StoredObject;
import jgnash.engine.dao.AbstractDAO;
import jgnash.engine.dao.DAO;
import jgnash.util.NotNull;

/**
 * Simple object container for StoredObjects that reads and writes and xml file.
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractXStreamDAO extends AbstractDAO implements DAO {

    /**
     * Maximum time in seconds before a commit will occur.
     */
    static final int MAX_COMMIT_TIME = 30; // seconds

    static final AtomicInteger commitCount = new AtomicInteger(0);

    final AbstractXStreamContainer container;

    private static final ReentrantLock commitLock = new ReentrantLock();

    private static final int MAX_COMMIT_COUNT = 250;

    AbstractXStreamDAO(@NotNull final AbstractXStreamContainer container) {
        Objects.requireNonNull(container);

        this.container = container;
    }

    private StoredObject getObjectByUuid(final UUID uuid) {
        return container.get(uuid);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getObjectByUuid(final Class<T> clazz, final UUID uuid) {
        Object o = getObjectByUuid(uuid);

        if (o != null && clazz.isAssignableFrom(o.getClass())) {
            return (T) o;
        }

        return null;
    }

    final void commit() {
        dirtyFlag.set(true);

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
