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
import jgnash.engine.TrashObject;
import jgnash.engine.dao.TrashDAO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Trash DAO
 *
 * @author Craig Cavanaugh
 */
class JpaTrashDAO extends AbstractJpaDAO implements TrashDAO {

    private static final Logger logger = Logger.getLogger(JpaTrashDAO.class.getName());

    JpaTrashDAO(final EntityManager entityManager, final boolean isRemote) {
        super(entityManager, isRemote);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TrashObject> getTrashObjects() {
        List<TrashObject> trashObjectList = Collections.EMPTY_LIST;

        emLock.lock();

        try {
            Future<List<TrashObject>> future = executorService.submit(new Callable<List<TrashObject>>() {
                @Override
                public List<TrashObject> call() throws Exception {
                    CriteriaBuilder cb = em.getCriteriaBuilder();
                    CriteriaQuery<TrashObject> cq = cb.createQuery(TrashObject.class);
                    Root<TrashObject> root = cq.from(TrashObject.class);
                    cq.select(root);

                    TypedQuery<TrashObject> q = em.createQuery(cq);

                    return new ArrayList<>(q.getResultList());
                }
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
            Future<Void> future = executorService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    em.getTransaction().begin();

                    em.persist(trashObject.getObject());
                    em.persist(trashObject);
                    em.getTransaction().commit();

                    return null;
                }
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
            Future<Void> future = executorService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    em.getTransaction().begin();

                    StoredObject object = trashObject.getObject();

                    em.remove(object);
                    em.remove(trashObject);

                    logger.info("Removed TrashObject");

                    em.getTransaction().commit();
                    return null;
                }
            });

            future.get(); // block
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }
    }
}
