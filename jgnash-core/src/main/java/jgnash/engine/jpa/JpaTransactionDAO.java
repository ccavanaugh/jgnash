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

        try {
            emLock.lock();

            Query q = em.createQuery("SELECT t FROM Transaction t WHERE t.markedForRemoval = false");

            // result lists are readonly
            transactionList = new ArrayList<Transaction>(q.getResultList());
        } finally {
            emLock.unlock();
        }

        return transactionList;
    }

    @Override
    public void refreshTransaction(final Transaction transaction) {
        try {
            emLock.lock();
            em.getTransaction().begin();

            em.refresh(transaction);
        } finally {
            em.getTransaction().commit();
            emLock.unlock();
        }
    }

    /*
     * @see jgnash.engine.TransactionDAO#addTransaction(jgnash.engine.Transaction)
     */
    @Override
    public synchronized boolean addTransaction(final Transaction transaction) {
        boolean result = false;

        try {
            emLock.lock();
            em.getTransaction().begin();

            em.persist(transaction);

            for (final Account account : transaction.getAccounts()) {
                em.merge(account);
            }

            result = true;
        } finally {
            em.getTransaction().commit();
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

        try {
            emLock.lock();
            em.getTransaction().begin();

            // look at accounts this transaction impacted and update the accounts
            for (final Account account : transaction.getAccounts()) {
                em.merge(account);
            }

            em.persist(transaction);    // saved, removed with the trash

            result = true;
        } finally {
            em.getTransaction().commit();
            emLock.unlock();
        }

        return result;
    }
}
