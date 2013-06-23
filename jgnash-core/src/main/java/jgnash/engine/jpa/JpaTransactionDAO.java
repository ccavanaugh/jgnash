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

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.engine.dao.TransactionDAO;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transaction DAO
 *
 * @author Craig Cavanaugh
 */
class JpaTransactionDAO extends AbstractJpaDAO implements TransactionDAO {

    private static final Logger logger = Logger.getLogger(JpaTransactionDAO.class.getName());

    JpaTransactionDAO(final EntityManager entityManager, final boolean isRemote) {
        super(entityManager, isRemote);
        logger.setLevel(Level.ALL);
    }

    /**
     * @see jgnash.engine.dao.TransactionDAO#getTransactions()
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Transaction> getTransactions() {
        List<Transaction> transactionList = Collections.EMPTY_LIST;

        emLock.lock();

        try {
            Future<List<Transaction>> future = executorService.submit(new Callable<List<Transaction>>() {
                @Override
                public List<Transaction> call() throws Exception {
                    Query q = em.createQuery("SELECT t FROM Transaction t WHERE t.markedForRemoval = false");

                    return new ArrayList<Transaction>(q.getResultList());
                }
            });

            transactionList = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }

        return transactionList;
    }

    /*
     * @see jgnash.engine.TransactionDAO#addTransaction(jgnash.engine.Transaction)
     */
    @Override
    public synchronized boolean addTransaction(final Transaction transaction) {
        boolean result = false;

        emLock.lock();

        try {
            Future<Boolean> future = executorService.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    em.getTransaction().begin();
                    em.persist(transaction);

                    for (final Account account : transaction.getAccounts()) {
                        em.persist(account);
                    }
                    em.getTransaction().commit();

                    return true;
                }
            });

            result = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }

        return result;
    }

    @Override
    public Transaction getTransactionByUuid(final String uuid) {
        return getObjectByUuid(Transaction.class, uuid);
    }

    /*
     * @see jgnash.engine.TransactionDAO#removeTransaction(jgnash.engine.Transaction)
     */
    @Override
    public synchronized boolean removeTransaction(final Transaction transaction) {
        boolean result = false;

        emLock.lock();

        try {
            Future<Boolean> future = executorService.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    em.getTransaction().begin();

                    // look at accounts this transaction impacted and update the accounts
                    for (final Account account : transaction.getAccounts()) {
                        em.persist(account);
                    }

                    em.persist(transaction);    // saved, removed with the trash
                    em.getTransaction().commit();

                    return true;
                }
            });

            result = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }

        return result;
    }

    @Override
    public List<Transaction> getTransactionsWithExternalLinks() {
        List<Transaction> transactionList = Collections.EMPTY_LIST;

        emLock.lock();

        try {
            Future<List<Transaction>> future = executorService.submit(new Callable<List<Transaction>>() {
                @Override
                public List<Transaction> call() throws Exception {
                    Query q = em.createQuery("SELECT t FROM Transaction t WHERE t.markedForRemoval = false AND t.externalLink is not null");

                    return new ArrayList<Transaction>(q.getResultList());
                }
            });

            transactionList = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }

        return transactionList;
    }
}
