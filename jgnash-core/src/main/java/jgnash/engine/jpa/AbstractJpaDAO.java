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
package jgnash.engine.jpa;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import jgnash.engine.StoredObject;
import jgnash.engine.concurrent.PriorityThreadPoolExecutor;
import jgnash.engine.dao.AbstractDAO;
import jgnash.engine.dao.DAO;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.NotNull;

import static jgnash.util.LogUtil.logSevere;

/**
 * Abstract JPA DAO.  Provides basic framework to work with the {@link EntityManager} in a thread safe manner.
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractJpaDAO extends AbstractDAO implements DAO {

    /**
     * The {@link EntityManager} is not thread safe.  All interaction should be wrapped with this lock
     */
    static final ReentrantLock emLock = new ReentrantLock();

    /**
     * This ExecutorService is to be used whenever the entity manager is
     * accessed because the EntityManager is not thread safe, but we want to return from some methods without blocking
     */
    static PriorityThreadPoolExecutor executorService =
            new PriorityThreadPoolExecutor(new DefaultDaemonThreadFactory("JPA Priority Executor"));

    /**
     * Entity manager reference.
     */
    final EntityManager em;

    /**
     * Remote connection if {@code true}.
     */
    final boolean isRemote;

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
            executorService = new PriorityThreadPoolExecutor();

        } catch (final InterruptedException e) {
            logSevere(AbstractJpaDAO.class, e);
            Thread.currentThread().interrupt();
        } finally {
            emLock.unlock();
        }
    }

    /**
     * Returns a list of objects that are assignable from from the specified Class.
     * <p>
     * Objects marked for removal are not included
     *
     * @param clazz the Class to query for
     * @param <T>   the type of class to query
     * @return A list of type T containing objects of type clazz
     */
    @NotNull
    public <T extends StoredObject> List<T> query(final Class<T> clazz) {

        try {
            final Future<List<T>> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    final CriteriaBuilder cb = em.getCriteriaBuilder();
                    final CriteriaQuery<T> cq = cb.createQuery(clazz);
                    cq.from(clazz);

                    final TypedQuery<T> query = em.createQuery(cq);

                    // filtering though the stream is not has fast as performing the filter within the query, but it
                    // ensures a ConcurrentModificationException is not thrown in rare circumstances by iterating.
                    return query.getResultStream().filter(t -> !t.isMarkedForRemoval()).collect(Collectors.toList());

                } catch (final ConcurrentModificationException | PersistenceException | IllegalStateException e1) {
                    logSevere(AbstractJpaDAO.class, e1);
                    return null;
                } finally {
                    emLock.unlock();
                }
            });

            return future.get();    // block and return
        } catch (final InterruptedException | ExecutionException e) {
            logSevere(AbstractJpaDAO.class, e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Merge / Update the object in place.
     *
     * @param object {@link StoredObject} to merge
     * @param <T>    the type of the value being merged
     * @return the merged object or null if an error occurred
     */
    <T extends StoredObject> T merge(final T object) {
        try {
            final Future<T> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    em.getTransaction().begin();
                    T mergedObject = em.merge(object);
                    em.getTransaction().commit();

                    dirtyFlag.set(true);

                    return mergedObject;
                } catch (final PersistenceException | IllegalStateException e1) {
                    logSevere(AbstractJpaDAO.class, e1);
                    return null;
                } finally {
                    emLock.unlock();
                }
            });

            return future.get();    // block and return
        } catch (final InterruptedException | ExecutionException e) {
            logSevere(AbstractJpaDAO.class, e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Persists an object.
     *
     * @param objects {@link Object} to persist
     * @return {@code true} if successful, {@code false} otherwise
     */
    boolean persist(final Object... objects) {
        boolean result = false;

        try {
            final Future<Boolean> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    em.getTransaction().begin();

                    for (final Object object : objects) {
                        em.persist(object);
                    }

                    em.getTransaction().commit();

                    dirtyFlag.set(true);

                    return true;
                } catch (final PersistenceException | IllegalStateException e1) {
                    logSevere(AbstractJpaDAO.class, e1);
                    return false;
                } finally {
                    emLock.unlock();
                }
            });

            try {
                result = future.get();  // block and return
            } catch (final InterruptedException ie) {
                Logger.getLogger(AbstractJpaDAO.class.getName()).log(Level.INFO, "Interrupted while waiting for result");
                result = false;
            }

        } catch (final ExecutionException e2) {
            logSevere(AbstractJpaDAO.class, e2);
            Thread.currentThread().interrupt();
        }

        return result;
    }

    @Override
    public <T> T getObjectByUuid(final Class<T> tClass, final UUID uuid) {
        T object = null;

        try {
            final Future<T> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    return em.find(tClass, uuid);
                } finally {
                    emLock.unlock();
                }
            });

            object = future.get();  // block and return
        } catch (final NoResultException e) {
            Logger.getLogger(AbstractJpaDAO.class.getName()).log(Level.INFO, "Did not find {0} for uuid: {1}",
                    new Object[]{tClass.getName(), uuid});
        } catch (ExecutionException | InterruptedException e) {
            logSevere(AbstractJpaDAO.class, e);
            Thread.currentThread().interrupt();
        }

        return object;
    }
}
