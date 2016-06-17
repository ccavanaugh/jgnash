/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
import jgnash.engine.dao.TrashDAO;
import jgnash.util.DefaultDaemonThreadFactory;

/**
 * JPA Trash DAO.
 *
 * @author Craig Cavanaugh
 */
class JpaTrashDAO extends AbstractJpaDAO implements TrashDAO {

    private static final Logger logger = Logger.getLogger(JpaTrashDAO.class.getName());

    private static final long MAXIMUM_ENTITY_TRASH_AGE = 2000;

    private ScheduledExecutorService trashExecutor;

    JpaTrashDAO(final EntityManager entityManager, final boolean isRemote) {
        super(entityManager, isRemote);

        trashExecutor = Executors.newSingleThreadScheduledExecutor(new DefaultDaemonThreadFactory());

        // run trash cleanup every 2 minutes 1 minute after startup
        trashExecutor.scheduleWithFixedDelay(JpaTrashDAO.this::cleanupEntityTrash, 1, 2, TimeUnit.MINUTES);
    }

    void stopTrashExecutor() {
        trashExecutor.shutdown();

        try {
            trashExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            trashExecutor = null;
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    @Override
    public List<TrashObject> getTrashObjects() {
        List<TrashObject> trashObjectList = Collections.emptyList();

        emLock.lock();

        try {
            final Future<List<TrashObject>> future = executorService.submit(() -> {
                final CriteriaBuilder cb = em.getCriteriaBuilder();
                final CriteriaQuery<TrashObject> cq = cb.createQuery(TrashObject.class);
                final Root<TrashObject> root = cq.from(TrashObject.class);
                cq.select(root);

                final TypedQuery<TrashObject> q = em.createQuery(cq);

                return new ArrayList<>(q.getResultList());
            });

            trashObjectList = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }

        return trashObjectList;
    }

    @Override
    public void add(final TrashObject trashObject) {
        emLock.lock();

        try {
            final Future<Void> future = executorService.submit(() -> {
                em.getTransaction().begin();

                em.persist(trashObject.getObject());
                em.persist(trashObject);
                em.getTransaction().commit();

                return null;
            });

            future.get();   // block
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }
    }

    @Override
    public void remove(final TrashObject trashObject) {
        emLock.lock();

        try {
            final Future<Void> future = executorService.submit(() -> {
                em.getTransaction().begin();

                final StoredObject object = trashObject.getObject();

                em.remove(object);
                em.remove(trashObject);

                logger.info("Removed TrashObject");

                em.getTransaction().commit();
                return null;
            });

            future.get(); // block
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }
    }

    @Override
    public void addEntityTrash(final Object entity) {
        emLock.lock();

        try {
            final Future<Void> future = executorService.submit(() -> {
                em.getTransaction().begin();
                em.persist(entity);
                em.persist(new JpaTrashEntity(entity));
                em.getTransaction().commit();

                return null;
            });

            future.get();   // block
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }
    }

    private void cleanupEntityTrash() {
        logger.info("Checking for entity trash");

        emLock.lock();

        try {
            final Future<Void> future = executorService.submit(() -> {

                final CriteriaBuilder cb = em.getCriteriaBuilder();
                final CriteriaQuery<JpaTrashEntity> cq = cb.createQuery(JpaTrashEntity.class);
                final Root<JpaTrashEntity> root = cq.from(JpaTrashEntity.class);
                cq.select(root);

                final TypedQuery<JpaTrashEntity> q = em.createQuery(cq);

                for (final JpaTrashEntity trashEntity : q.getResultList()) {

                    if (ChronoUnit.MILLIS.between(trashEntity.getDate(), LocalDateTime.now())
                            >= MAXIMUM_ENTITY_TRASH_AGE) {

                        final Class<?> clazz = Class.forName(trashEntity.getClassName());
                        final Object entity = em.find(clazz, trashEntity.getEntityId());

                        em.getTransaction().begin();

                        if (entity != null) {
                            em.remove(entity);
                            logger.log(Level.INFO, "Removed entity trash: {0}@{1}",
                                    new Object[]{trashEntity.getClassName(), trashEntity.getEntityId()});
                        }
                        em.remove(trashEntity);

                        em.getTransaction().commit();
                    }
                }
                return null;
            });

            future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }
    }
}
