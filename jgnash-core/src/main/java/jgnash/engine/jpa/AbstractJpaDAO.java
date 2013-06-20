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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

/**
 * Abstract DAO
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractJpaDAO extends AbstractDAO {

    static final ReentrantLock emLock = new ReentrantLock();

    EntityManager em;

    boolean isRemote = false;

    /**
     * This ExecutorService is to be used whenever the entity manager is
     * accessed because and EntityManager is not thread safe
     */
    static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    AbstractJpaDAO(final EntityManager entityManager, final boolean isRemote) {
        assert entityManager != null;

        this.isRemote = isRemote;
        em = entityManager;
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
            Future<T> future = executorService.submit(new Callable<T>() {
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

    /**
     * Refresh a managed object
     *
     * @param object object to re
     */
    public void refresh(final StoredObject object) {
        emLock.lock();

        try {
            Future<Void> future = executorService.submit(new Callable<Void>() {
                @Override
                public Void call() {
                    em.refresh(object);
                    return null;
                }
            });

            future.get();
        } catch (ExecutionException | InterruptedException e) {
            Logger.getLogger(AbstractJpaDAO.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
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
            Logger.getLogger(AbstractJpaDAO.class.getName()).info("Did not find " + tClass.getName() + " for uuid: " + uuid);
        } catch (ExecutionException | InterruptedException e) {
            Logger.getLogger(AbstractJpaDAO.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }

        return object;
    }
}
