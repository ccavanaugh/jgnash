/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import jgnash.engine.StoredObject;
import jgnash.engine.dao.AbstractDAO;
import jgnash.util.DefaultDaemonThreadFactory;

/**
 * Abstract JPA DAO.  Provides basic framework to work with the {@link EntityManager} in a thread safe manner.
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractJpaDAO extends AbstractDAO {

    /**
     * The {@link EntityManager} is not thread safe.  All interaction should be wrapped with this lock
     */
    static final ReentrantLock emLock = new ReentrantLock();

    /**
     * Shared entity manager
     */
    final EntityManager em;

    /**
     * Remote connection if {@code true}
     */
    boolean isRemote = false;

    /**
     * This ExecutorService is to be used whenever the entity manager is
     * accessed because and EntityManager is not thread safe
     */
    static ExecutorService executorService = Executors.newSingleThreadExecutor(new DefaultDaemonThreadFactory());

    AbstractJpaDAO(final EntityManager entityManager, final boolean isRemote) {
        Objects.requireNonNull(entityManager);

        this.isRemote = isRemote;
        em = entityManager;
    }

    static void shutDownExecutor() {
        // Stop the shared executor server, wait for all tasks to complete

        emLock.lock();

        try {
            executorService.shutdown();

            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

            // Regenerate the executor service
            executorService = Executors.newSingleThreadExecutor(new DefaultDaemonThreadFactory());

        } catch (InterruptedException e) {
            Logger.getLogger(AbstractJpaDAO.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }
    }

    /**
     * Merge / Update the object in place
     *
     * @param object {@link StoredObject} to merge
     * @param <T>    the type of the value being merged
     * @return the merged object or null if an error occurred
     */
    <T extends StoredObject> T merge(final T object) {
        emLock.lock();

        try {
            final Future<T> future = executorService.submit(() -> {

                em.getTransaction().begin();
                T mergedObject = em.merge(object);
                em.getTransaction().commit();

                return mergedObject;
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
     * Persists an object
     *
     * @param object {@link StoredObject} to persist
     * @param <T>    the type of the value being persisted
     */
    <T extends StoredObject> void persist(final T object) {
        emLock.lock();

        try {
            final Future<Void> future = executorService.submit(() -> {

                em.getTransaction().begin();
                em.persist(object);
                em.getTransaction().commit();

                return null;
            });

            future.get();

        } catch (final InterruptedException | ExecutionException e) {
            Logger.getLogger(AbstractJpaDAO.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }
    }


    public <T> T getObjectByUuid(final Class<T> tClass, final String uuid) {
        T object = null;

        emLock.lock();

        try {
            final Future<T> future = executorService.submit(() -> em.find(tClass, uuid));

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
