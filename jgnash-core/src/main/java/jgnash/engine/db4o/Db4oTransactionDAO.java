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
package jgnash.engine.db4o;

import com.db4o.ObjectContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.dao.TransactionDAO;

/**
 * db4o transaction DAO
 *
 * @author Craig Cavanaugh
 *
 */
class Db4oTransactionDAO extends AbstractDb4oDAO implements TransactionDAO {

    private static final Logger logger = Logger.getLogger(Db4oTransactionDAO.class.getName());

    Db4oTransactionDAO(final ObjectContainer container, final boolean isRemote) {
        super(container, isRemote);
        logger.setLevel(Level.ALL);
    }

    /**
     * @see TransactionDAO#getTransactions()    
     */
    @Override
    public synchronized List<Transaction> getTransactions() {
        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            List<Transaction> list = container.query(Transaction.class);
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);

            List<Transaction> resultList = new ArrayList<>(list.size());

            for (Transaction t : list) {
                if (!t.isMarkedForRemoval()) {
                    resultList.add(t);
                }
            }
            return resultList;
        }
        logger.severe(SEMAPHORE_WARNING);
        return Collections.emptyList();
    }

    @Override
    public synchronized void refreshTransaction(final Transaction transaction) {
        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.ext().refresh(transaction, 4);
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }
    }

    /*
     * @see jgnash.engine.TransactionDAO#addTransaction(jgnash.engine.Transaction)
     */
    @Override
    public synchronized boolean addTransaction(final Transaction transaction) {
        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {

            List<TransactionEntry> entries = transaction.getTransactionEntries();
            Set<Account> accounts = transaction.getAccounts();

            for (TransactionEntry entry : entries) {
                container.set(entry);
            }

            for (Account account : accounts) {
                container.set(account);
            }

            container.set(transaction);

            commit();
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);

            return true;
        }
        logger.severe(SEMAPHORE_WARNING);
        return false;
    }

    /*
     * @see jgnash.engine.TransactionDAO#removeTransaction(jgnash.engine.Transaction)
     */
    @Override
    public synchronized boolean removeTransaction(final Transaction transaction) {

        // look at accounts this transaction impacted and update the accounts

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            Set<Account> accounts = transaction.getAccounts();

            for (Account account : accounts) {
                container.set(account);
            }

            commit();
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);

            return true;
        }

        logger.severe(SEMAPHORE_WARNING);
        return false;
    }
}
