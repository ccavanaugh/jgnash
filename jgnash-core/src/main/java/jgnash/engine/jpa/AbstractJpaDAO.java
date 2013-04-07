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
package jgnash.engine.jpa;

import jgnash.engine.StoredObject;
import jgnash.engine.dao.AbstractDAO;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

/**
 * Abstract DAO
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractJpaDAO extends AbstractDAO {

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

    protected static ReentrantLock emLock = new ReentrantLock();

    EntityManager em;

    boolean isRemote = false;

    AbstractJpaDAO(final EntityManager entityManager, final boolean isRemote) {
        assert entityManager != null;

        this.isRemote = isRemote;
        em = entityManager;
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
            // TODO: Dump to an XStream file periodically
        } finally {
            commitLock.unlock();
        }
    }

    public <T> T getObjectByUuid(final Class<T> tClass, final String uuid) {
        T object = null;

        try {
            emLock.lock();

            object = em.find(tClass, uuid);
        } catch (NoResultException e) {
            Logger.getLogger(AbstractJpaDAO.class.getName()).info("Did not find object for uuid: " + uuid);
        } finally {
            emLock.unlock();
        }

        return object;
    }

    public StoredObject getObjectByUuid(final String uuid) {

        StoredObject o = null;

        try {
            emLock.lock();

            o = em.find(StoredObject.class, uuid);
        } catch (NoResultException e) {
            Logger.getLogger(AbstractJpaDAO.class.getName()).info("Did not find object for uuid: " + uuid);
        } finally {
            emLock.unlock();
        }

        return o;
    }
}
