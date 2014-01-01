/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.ui.register.table;

import java.awt.EventQueue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import jgnash.engine.Account;
import jgnash.engine.Comparators;
import jgnash.engine.Transaction;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageProperty;
import jgnash.ui.register.AccountBalanceDisplayManager;

/**
 * Sortable table model
 *
 * @author Craig Cavanaugh
 */
public class SortedTableModel extends RegisterTableModel implements SortableTableModel {

    private List<Transaction> transactions = new ArrayList<>();

    private int sortColumn = 0;

    private boolean ascending = true;

    private boolean[] sortedColumnMap;

    private boolean[] sortColumns;

    private Comparator<Transaction> comparator = Comparators.getTransactionByDate();

    private final ReentrantLock lock = new ReentrantLock();

    public SortedTableModel(final Account account, final String[] names) {
        super(account, names);
        getTransactions();
    }

    private static boolean[] getSortableColumns() {
        return new boolean[]{true, true, true, true, true, false, true, true, false};
    }

    /**
     * Overrides the super to build the sort column map
     *
     * @see jgnash.ui.register.table.AbstractRegisterTableModel#buildColumnMap()
     */
    @Override
    protected void buildColumnMap() {
        super.buildColumnMap();

        if (sortColumns == null) {
            sortColumns = getSortableColumns();
        }

        sortedColumnMap = new boolean[getColumnCount()];

        int index = 0;
        for (int i = 0; i < columnVisible.length; i++) {
            if (columnVisible[i]) {
                sortedColumnMap[index] = sortColumns[i];
                index++;
            }
        }
    }

    /**
     * Creates a private clone of the account's transactions that can be manipulated
     */
    private void getTransactions() {
        lock.lock();

        try {
            transactions = new ArrayList<>(account.getSortedTransactionList());
            Collections.sort(transactions, comparator);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getRowCount() {
        lock.lock();

        try {
            return transactions.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Transaction getTransactionAt(final int index) {
        lock.lock();

        try {
            if (ascending) {
                return transactions.get(index);
            }
            return transactions.get(transactions.size() - index - 1);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Converts the model row index into the transaction index for the account
     *
     * @param index model row index
     * @return index for the account
     */
    public int convertRowIndexToAccount(final int index) {
        lock.lock();

        try {
            return account.indexOf(getTransactionAt(index));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int indexOf(final Transaction t) {
        lock.lock();

        try {
            return transactions.indexOf(t);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the account balance up to a specified index.
     *
     * @param index the balance of this account at the specified index.
     * @return the balance of the account at the specified index.
     */
    @Override
    public BigDecimal getBalanceAt(final int index) {
        if (balanceCache.get(index) != null) {
            return AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), balanceCache.get(index));
        }

        /* look to see if we have the balance for the previous transaction
           * and use it */
        if (index > 0 && balanceCache.get(index - 1) != null) {
            BigDecimal bal = balanceCache.get(index - 1);
            bal = bal.add(getTransactionAt(index).getAmount(account));
            balanceCache.set(index, bal);
            return bal;
        }

        /* Do not have a clue where to start. Do it the long way */
        BigDecimal bal = BigDecimal.ZERO;
        if (transactions != null) {

            lock.lock();

            try {
                for (int i = 0; i <= index; i++) {
                    bal = bal.add(getTransactionAt(i).getAmount(account));
                }
            } finally {
                lock.unlock();
            }
        }
        balanceCache.set(index, bal);
        return AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), bal);
    }

    /**
     * this override the update in AbstractRegisterTableModel to improve performance
     */
    @Override
    public void messagePosted(final Message event) {

        if (event.getObject(MessageProperty.ACCOUNT) == account) {
            switch (event.getEvent()) {
                case FILE_CLOSING:
                    unregister();
                    return;
                case TRANSACTION_ADD:
                    EventQueue.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            balanceCache.ensureCapacity(account.getTransactionCount());
                            addTransaction((Transaction) event.getObject(MessageProperty.TRANSACTION));
                        }
                    });
                    return;
                case TRANSACTION_REMOVE:
                    EventQueue.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            removeTransaction((Transaction) event.getObject(MessageProperty.TRANSACTION));
                        }
                    });
                    return;
                default: // ignore any other messages that don't belong to us
                    break;
            }
        }
    }

    private void addTransaction(final Transaction t) {
        lock.lock();

        try {
            int index = Collections.binarySearch(transactions, t, comparator);

            if (index < 0) {
                int i = -index - 1;

                transactions.add(i, t);
                if (i >= 1) {
                    balanceCache.clear(i - 1);
                } else {
                    balanceCache.clear();
                }
                fireTableRowsInserted(i, i);
            }
        } finally {
            lock.unlock();
        }
    }

    private void removeTransaction(final Transaction t) {

        lock.lock();

        try {
            int i = transactions.indexOf(t);

            if (i >= 0) {
                transactions.remove(t);
                if (i >= 1) {
                    balanceCache.clear(i - 1); // clear the previous index in the balance cache
                } else {
                    balanceCache.clear();
                }
                fireTableRowsDeleted(i, i);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isSortable(final int col) {
        return sortedColumnMap[col];
    }

    // "Date", "Num", "Payee", "Memo", "Account", "C", "Credit", "Debit", "Balance"
    @Override
    public void sortColumn(final int col, final boolean ascending1) {
        this.ascending = ascending1;
        this.sortColumn = col;
        switch (getColumnMapping(col)) {
            case 0:
                comparator = Comparators.getTransactionByDate();
                break;
            case 1:
                comparator = Comparators.getTransactionByNumber();
                break;
            case 2:
                comparator = Comparators.getTransactionByPayee();
                break;
            case 3:
                comparator = Comparators.getTransactionByMemo();
                break;
            case 4:
                comparator = new Comparators.TransactionByAccount(account);
                break;
            case 6:
            case 7:
                comparator = Comparators.getTransactionByAmount();
                break;
            default:
                comparator = Comparators.getTransactionByDate();
                break;
        }

        lock.lock();

        try {
            Collections.sort(transactions, comparator);

            balanceCache.clear(); // order has changed, clear the balance cache
        } finally {
            lock.unlock();
        }

        fireTableDataChanged();
    }

    @Override
    public int getSortedColumn() {
        return sortColumn;
    }

    @Override
    public boolean getAscending() {
        return ascending;
    }
}
