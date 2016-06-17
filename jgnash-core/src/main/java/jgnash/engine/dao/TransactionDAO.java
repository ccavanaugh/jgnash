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
package jgnash.engine.dao;

import java.util.List;

import jgnash.engine.Transaction;

/**
 * Transaction DAO Interface.
 *
 * @author Craig Cavanaugh
 */
public interface TransactionDAO {

    /**
     * Returns a list of transactions.
     *
     * @return List of transactions
     */
    List<Transaction> getTransactions();

    boolean addTransaction(Transaction transaction);

    Transaction getTransactionByUuid(final String uuid);

    boolean removeTransaction(Transaction transaction);

    /**
     * Transactions are generally immutable and should not be updated.
     * <p>
     * This is intended for fix in place data errors
     *
     * @param transaction {@code Transaction} to update
     */
    void updateTransaction(Transaction transaction);

    /**
     * Returns a list of transactions with external links.
     *
     * @return List of transactions
     */
    List<Transaction> getTransactionsWithAttachments();

}