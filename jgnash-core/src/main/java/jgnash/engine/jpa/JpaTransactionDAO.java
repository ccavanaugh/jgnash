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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import jgnash.engine.Transaction;
import jgnash.engine.dao.TransactionDAO;

/**
 * Transaction DAO.
 *
 * @author Craig Cavanaugh
 */
class JpaTransactionDAO extends AbstractJpaDAO implements TransactionDAO {

    private static final Logger logger = Logger.getLogger(JpaTransactionDAO.class.getName());

    JpaTransactionDAO(final EntityManager entityManager, final boolean isRemote) {
        super(entityManager, isRemote);
        logger.setLevel(Level.ALL);
    }

    /*
     * @see jgnash.engine.dao.TransactionDAO#getTransactions()
     */
    @Override
    public List<Transaction> getTransactions() {
        return query(Transaction.class);
    }

    /*
     * @see jgnash.engine.TransactionDAO#addTransaction(jgnash.engine.Transaction)
     */
    @Override
    public synchronized boolean addTransaction(final Transaction transaction) {
        boolean result = false;

        try {
            final Future<Boolean> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    em.getTransaction().begin();

                    em.persist(transaction);
                    transaction.getAccounts().forEach(em::persist);

                    em.getTransaction().commit();

                    dirtyFlag.set(true);

                    return true;
                } finally {
                    emLock.unlock();
                }
            });

            result = future.get();  // block and return
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        return result;
    }

    @Override
    public Transaction getTransactionByUuid(final UUID uuid) {
        return getObjectByUuid(Transaction.class, uuid);
    }

    /*
     * @see jgnash.engine.TransactionDAO#removeTransaction(jgnash.engine.Transaction)
     */
    @Override
    public synchronized boolean removeTransaction(final Transaction transaction) {
        boolean result = false;

        try {
            final Future<Boolean> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    em.getTransaction().begin();

                    // look at accounts this transaction impacted and update the accounts
                    transaction.getAccounts().forEach(em::persist);

                    em.persist(transaction);    // saved, removed with the trash
                    em.getTransaction().commit();

                    dirtyFlag.set(true);

                    return true;
                } finally {
                    emLock.unlock();
                }
            });

            result = future.get();  // block and return
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        return result;
    }

    @Override
    public List<Transaction> getTransactionsWithAttachments() {
        List<Transaction> transactionList = Collections.emptyList();

        try {
            final Future<List<Transaction>> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    final TypedQuery<Transaction> q = em
                            .createQuery("SELECT t FROM Transaction t WHERE t.markedForRemoval = false AND t.attachment is not null",
                                    Transaction.class);

                    return new ArrayList<>(q.getResultList());
                } finally {
                    emLock.unlock();
                }
            });

            transactionList = future.get(); // block and return
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        return transactionList;
    }
}
