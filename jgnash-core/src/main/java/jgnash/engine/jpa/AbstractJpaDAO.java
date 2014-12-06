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
package jgnash.engine.jpa;

import jgnash.engine.StoredObject;
import jgnash.engine.dao.AbstractDAO;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract DAO
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractJpaDAO extends AbstractDAO {

    static final ReentrantLock emLock = new ReentrantLock();

    final EntityManager em;

    boolean isRemote = false;

    /**
     * This ExecutorService is to be used whenever the entity manager is
     * accessed because and EntityManager is not thread safe
     */
    static ExecutorService executorService;

    AbstractJpaDAO(final EntityManager entityManager, final boolean isRemote) {
        Objects.requireNonNull(entityManager);

        this.isRemote = isRemote;
        em = entityManager;

        emLock.lock();

        // Regenerate the executor service if needed
        try {
            if (executorService == null || executorService.isShutdown()) {
                executorService = Executors.newSingleThreadExecutor();
            }
        } finally {
            emLock.unlock();
        }
    }

    /**
     * Merge / Update the object in place
     *
     * @param object Object to merge
     * @return the merged object or null if an error occurred
     */
    <T extends StoredObject> T merge(final T object) {
        emLock.lock();

        try {
            final Future<T> future = executorService.submit(new Callable<T>() {
                @Override
                public T call() throws Exception {

                    em.getTransaction().begin();
                    T mergedObject = em.merge(object);
                    em.getTransaction().commit();

                    return mergedObject;
                }
            });

            return future.get();

        } catch (final InterruptedException | ExecutionException e) {
            Logger.getLogger(AbstractJpaDAO.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            return null;
        } finally {
            emLock.unlock();
        }
    }


    public <T> T getObjectByUuid(final Class<T> tClass, final String uuid) {
        T object = null;

        emLock.lock();

        try {
            Future<T> future = executorService.submit(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    return em.find(tClass, uuid);
                }
            });

            object = future.get();
        } catch (NoResultException e) {
            Logger.getLogger(AbstractJpaDAO.class.getName()).log(Level.INFO, "Did not find {0} for uuid: {1}", new Object[]{tClass.getName(), uuid});
        } catch (ExecutionException | InterruptedException e) {
            Logger.getLogger(AbstractJpaDAO.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }

        return object;
    }
}
