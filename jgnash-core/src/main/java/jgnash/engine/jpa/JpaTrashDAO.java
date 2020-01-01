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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import jgnash.engine.StoredObject;
import jgnash.engine.TrashObject;
import jgnash.engine.concurrent.Priority;
import jgnash.engine.dao.TrashDAO;
import jgnash.util.DefaultDaemonThreadFactory;

import org.apache.commons.collections4.ListUtils;

/**
 * JPA Trash DAO.
 *
 * @author Craig Cavanaugh
 */
class JpaTrashDAO extends AbstractJpaDAO implements TrashDAO {

    private static final Logger logger = Logger.getLogger(JpaTrashDAO.class.getName());

    private static final long MAXIMUM_ENTITY_TRASH_AGE = 2000;

    private static final int INITIAL_DELAY = 60;    // Delay start 60 seconds

    private static final int PERIOD = 35;   // Execute every 35 seconds

    private static final int MAX_ENTITY_LUMP = 5;

    private ScheduledExecutorService entityTrashExecutor;

    JpaTrashDAO(final EntityManager entityManager, final boolean isRemote) {
        super(entityManager, isRemote);

        entityTrashExecutor = Executors.newSingleThreadScheduledExecutor(
                new DefaultDaemonThreadFactory("JPA Trash Executor"));

        // run trash cleanup every 35 seconds 1 minute after startup
        entityTrashExecutor.scheduleWithFixedDelay(JpaTrashDAO.this::cleanupEntityTrash, INITIAL_DELAY, PERIOD,
                TimeUnit.SECONDS);
    }

    void stopTrashExecutor() {
        entityTrashExecutor.shutdown();

        try {
            entityTrashExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            entityTrashExecutor = null;
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    @Override
    public List<TrashObject> getTrashObjects() {
        List<TrashObject> trashObjectList = Collections.emptyList();

        try {
            final Future<List<TrashObject>> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    final CriteriaBuilder cb = em.getCriteriaBuilder();
                    final CriteriaQuery<TrashObject> cq = cb.createQuery(TrashObject.class);
                    final Root<TrashObject> root = cq.from(TrashObject.class);
                    cq.select(root);

                    final TypedQuery<TrashObject> q = em.createQuery(cq);

                    return new ArrayList<>(q.getResultList());
                } finally {
                    emLock.unlock();
                }
            });

            trashObjectList = future.get(); // block
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        return trashObjectList;
    }

    @Override
    public void add(final TrashObject trashObject) {
        try {
            final Future<Void> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    em.getTransaction().begin();

                    em.persist(trashObject.getObject());
                    em.persist(trashObject);

                    em.getTransaction().commit();

                    dirtyFlag.set(true);

                    return null;
                } finally {
                    emLock.unlock();
                }
            });

            future.get();   // block
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void remove(final TrashObject trashObject) {
        try {
            final Future<Void> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    em.getTransaction().begin();

                    final StoredObject object = trashObject.getObject();

                    em.remove(object);
                    em.remove(trashObject);

                    em.getTransaction().commit();

                    dirtyFlag.set(true);

                    logger.info("Removed TrashObject");

                    return null;
                } finally {
                    emLock.unlock();
                }
            }, Priority.BACKGROUND);

            future.get(); // block
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void addEntityTrash(final Object entity) {
        try {
            final Future<Void> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    em.getTransaction().begin();

                    em.persist(entity);
                    em.persist(new JpaTrashEntity(entity));

                    em.getTransaction().commit();

                    dirtyFlag.set(true);

                    return null;
                } finally {
                    emLock.unlock();
                }
            });

            future.get();   // block
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private void cleanupEntityTrash() {
        try {
            final Future<Void> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    final CriteriaBuilder cb = em.getCriteriaBuilder();
                    final CriteriaQuery<JpaTrashEntity> cq = cb.createQuery(JpaTrashEntity.class);
                    final Root<JpaTrashEntity> root = cq.from(JpaTrashEntity.class);
                    cq.select(root);

                    final TypedQuery<JpaTrashEntity> q = em.createQuery(cq);

                    /* Partition the results into small chunks so other higher priority work can be performed without
                       stalling the application */
                    final List<List<JpaTrashEntity>> listList = ListUtils.partition(q.getResultList(), MAX_ENTITY_LUMP);

                    for (final List<JpaTrashEntity> entityList : listList) {

                        executorService.submit(() -> {
                            emLock.lock();

                            try {
                                em.getTransaction().begin();

                                for (final JpaTrashEntity trashEntity : entityList) {
                                    if (!trashEntity.isPending()
                                            && ChronoUnit.MILLIS.between(trashEntity.getDate(), LocalDateTime.now())
                                            >= MAXIMUM_ENTITY_TRASH_AGE) {

                                        trashEntity.setPending();

                                        final Class<?> clazz = Class.forName(trashEntity.getClassName());
                                        final Object entity = em.find(clazz, trashEntity.getEntityId());

                                        if (entity != null) {
                                            em.remove(entity);
                                            logger.log(Level.INFO, "Removed entity trash: {0}@{1}",
                                                    new Object[]{trashEntity.getClassName(), trashEntity.getEntityId()});
                                        }
                                        em.remove(trashEntity);
                                    }
                                }

                                em.getTransaction().commit();
                            } finally {
                                emLock.unlock();
                            }

                            return null;
                        }, Priority.BACKGROUND);
                    }

                    return null;
                } finally {
                    emLock.unlock();
                }
            }, Priority.BACKGROUND);

            future.get();       // block
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
