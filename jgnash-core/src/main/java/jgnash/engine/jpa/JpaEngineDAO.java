/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import jgnash.engine.StoredObject;
import jgnash.engine.dao.AccountDAO;
import jgnash.engine.dao.BudgetDAO;
import jgnash.engine.dao.CommodityDAO;
import jgnash.engine.dao.ConfigDAO;
import jgnash.engine.dao.EngineDAO;
import jgnash.engine.dao.RecurringDAO;
import jgnash.engine.dao.TransactionDAO;
import jgnash.engine.dao.TrashDAO;

import static jgnash.util.LogUtil.logSevere;

/**
 * Engine DAO.
 *
 * @author Craig Cavanaugh
 */
class JpaEngineDAO extends AbstractJpaDAO implements EngineDAO {

    private AccountDAO accountDAO;

    private BudgetDAO budgetDAO;

    private CommodityDAO commodityDAO;

    private ConfigDAO configDAO;

    private RecurringDAO recurringDAO;

    private TransactionDAO transactionDAO;

    private TrashDAO trashDAO;

    JpaEngineDAO(final EntityManager entityManager, final boolean isRemote) {
        super(entityManager, isRemote);
    }

    @Override
    public synchronized void shutdown() {

        emLock.lock();

        try {
            // Stop the trash executor service
            ((JpaTrashDAO) getTrashDAO()).stopTrashExecutor();

            // Stop the shared executor service, wait for all tasks to complete and reset
            shutDownExecutor();
        } finally {
            emLock.unlock();
        }
    }

    @Override
    public synchronized AccountDAO getAccountDAO() {
        if (accountDAO == null) {
            accountDAO = new JpaAccountDAO(em, isRemote);
        }
        return accountDAO;
    }

    @Override
    public BudgetDAO getBudgetDAO() {
        if (budgetDAO == null) {
            budgetDAO = new JpaBudgetDAO(em, isRemote);
        }
        return budgetDAO;
    }

    @Override
    public synchronized CommodityDAO getCommodityDAO() {
        if (commodityDAO == null) {
            commodityDAO = new JpaCommodityDAO(em, isRemote);
        }
        return commodityDAO;
    }

    @Override
    public synchronized ConfigDAO getConfigDAO() {
        if (configDAO == null) {
            configDAO = new JpaConfigDAO(em, isRemote);
        }
        return configDAO;
    }

    @Override
    public synchronized RecurringDAO getRecurringDAO() {
        if (recurringDAO == null) {
            recurringDAO = new JpaRecurringDAO(em, isRemote);
        }
        return recurringDAO;
    }

    @Override
    public synchronized TransactionDAO getTransactionDAO() {
        if (transactionDAO == null) {
            transactionDAO = new JpaTransactionDAO(em, isRemote);
        }
        return transactionDAO;
    }

    @Override
    public synchronized TrashDAO getTrashDAO() {
        if (trashDAO == null) {
            trashDAO = new JpaTrashDAO(em, isRemote);
        }
        return trashDAO;
    }

    @Override
    public List<StoredObject> getStoredObjects() {
        List<StoredObject> list = Collections.emptyList();

        try {
            final Future<List<StoredObject>> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    final CriteriaBuilder cb = em.getCriteriaBuilder();
                    final CriteriaQuery<StoredObject> cq = cb.createQuery(StoredObject.class);
                    final Root<StoredObject> root = cq.from(StoredObject.class);
                    cq.select(root);

                    final TypedQuery<StoredObject> q = em.createQuery(cq);

                    return new ArrayList<>(q.getResultList());
                } finally {
                    emLock.unlock();
                }
            });

            list = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logSevere(JpaEngineDAO.class, e);
        }

        return list;
    }

    @Override
    public <T extends StoredObject> List<T> getStoredObjects(final Class<T> tClass) {
        List<T> list = Collections.emptyList();

        emLock.lock();

        try {
            final Future<List<T>> future = executorService.submit(() -> {
                final CriteriaBuilder cb = em.getCriteriaBuilder();
                final CriteriaQuery<T> cq = cb.createQuery(tClass);
                final Root<T> root = cq.from(tClass);
                cq.select(root);

                final TypedQuery<T> q = em.createQuery(cq);

                return new ArrayList<>(q.getResultList());
            });

            list = future.get();
        } catch (InterruptedException | ExecutionException e) {
            logSevere(JpaEngineDAO.class, e);
        } finally {
            emLock.unlock();
        }

        return stripMarkedForRemoval(list);
    }

    /**
     * Refresh a managed object.
     *
     * @param object object to re
     */
    @Override
    public void refresh(final StoredObject object) {
        try {
            Future<Void> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    em.refresh(object);
                    return null;
                } finally {
                    emLock.unlock();
                }
            });

            future.get();   // block
        } catch (ExecutionException | InterruptedException e) {
            logSevere(JpaEngineDAO.class, e);
        }
    }

    @Override
    public void bulkUpdate(final List<? extends StoredObject> objectList) {
        try {
            final Future<Void> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    em.getTransaction().begin();
                    objectList.forEach(em::persist);
                    em.getTransaction().commit();

                    return null;
                } finally {
                    emLock.unlock();
                }
            });

            future.get();   // block
        } catch (final InterruptedException | ExecutionException e) {
            logSevere(JpaEngineDAO.class, e);
        }
    }

    @Override
    public boolean isRemote() {
        return isRemote;
    }
}
