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
package jgnash.engine.xstream;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jgnash.engine.Transaction;
import jgnash.engine.dao.TransactionDAO;

/**
 * Transaction XML DAO.
 *
 * @author Craig Cavanaugh
 */
class XStreamTransactionDAO extends AbstractXStreamDAO implements TransactionDAO {

    XStreamTransactionDAO(final AbstractXStreamContainer container) {
        super(container);
    }

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
    public Transaction getTransactionByUuid(final UUID uuid) {
        return getObjectByUuid(Transaction.class, uuid);
    }

    @Override
    public boolean removeTransaction(final Transaction transaction) {
        commit();
        return true;
    }

    @Override
    public List<Transaction> getTransactionsWithAttachments() {
        return container.query(Transaction.class).parallelStream()
                .filter(transaction -> !transaction.isMarkedForRemoval() && transaction.getAttachment() != null)
                .collect(Collectors.toList());
    }
}
