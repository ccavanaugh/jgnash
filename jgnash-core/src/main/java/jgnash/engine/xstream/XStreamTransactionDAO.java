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
package jgnash.engine.xstream;

import jgnash.engine.Transaction;
import jgnash.engine.dao.TransactionDAO;

import java.util.ArrayList;
import java.util.List;

/**
 * Transaction XML DAO
 *
 * @author Craig Cavanaugh
 */
public class XStreamTransactionDAO extends AbstractXStreamDAO implements TransactionDAO {

    XStreamTransactionDAO(final AbstractXStreamContainer container) {
        super(container);
    }

    /**
     * @see TransactionDAO#getTransactions()    
     */
    @Override
    public List<Transaction> getTransactions() {
        return stripMarkedForRemoval(container.query(Transaction.class));
    }

    @Override
    public boolean addTransaction(final Transaction transaction) {
        container.set(transaction);
        commit();

        return true;
    }

    @Override
    public Transaction getTransactionByUuid(final String uuid) {
        return getObjectByUuid(Transaction.class, uuid);
    }

    @Override
    public boolean removeTransaction(final Transaction transaction) {
        commit();
        return true;
    }

    @Override
    public List<Transaction> getTransactionsWithAttachments() {
        List<Transaction> transactionList = new ArrayList<>();

        for (Transaction transaction : container.query(Transaction.class)) {
            if (!transaction.isMarkedForRemoval() && transaction.getAttachment() != null) {
                transactionList.add(transaction);
            }
        }

        return transactionList;
    }
}
