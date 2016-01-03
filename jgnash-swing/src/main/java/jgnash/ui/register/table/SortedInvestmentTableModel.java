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
package jgnash.ui.register.table;

import java.awt.EventQueue;
import java.math.BigDecimal;
import java.util.Arrays;

import jgnash.engine.Account;
import jgnash.engine.Comparators;
import jgnash.engine.Transaction;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageProperty;

/**
 * Sorts the account transactions.
 *
 * @author Craig Cavanaugh
 *
 */
public class SortedInvestmentTableModel extends InvestmentRegisterTableModel implements SortableTableModel {

    private Transaction transactions[] = new Transaction[0];
    private int sortColumn = 0;
    private boolean ascending = true;
    private boolean[] sortedColumnMap;
    private boolean[] sortColumns;

    private final Object mutex = new Object();

    public SortedInvestmentTableModel(Account account) {
        super(account);
        getTransactions();
    }

    /**
     * @see jgnash.ui.register.table.SortableTableModel#isSortable(int)
     */
    @Override
    public boolean isSortable(int col) {
        return sortedColumnMap[col];
    }

    private static boolean[] getSortableColumns() {
        return new boolean[] { true, true, true, false, false, false, false };
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
     * Creates a private clone of the account's transactions
     */
    private void getTransactions() {
        synchronized (mutex) {
            int size = account.getTransactionCount();
            transactions = new Transaction[size];
            for (int i = 0; i < size; i++) {
                transactions[i] = account.getTransactionAt(i);
            }
            sortColumn(sortColumn, ascending);
        }
    }

    @Override
    public int getRowCount() {
        synchronized (mutex) {
            return transactions.length;
        }
    }

    @Override
    public Transaction getTransactionAt(int index) {
        synchronized (mutex) {
            if (ascending) {
                return transactions[index];
            }
            return transactions[transactions.length - index - 1];
        }
    }

    /**
     * Get the account balance up to a specified index.
     *
     * @param index the balance of this account at the specified index.
     * @return the balance of the account at the specified index.
     */
    @Override
    public BigDecimal getBalanceAt(int index) {
        BigDecimal balance = BigDecimal.ZERO;
        synchronized (mutex) {
            for (int i = 0; i <= index; i++) {
                balance = balance.add(getTransactionAt(i).getAmount(account));
            }
        }
        return balance;
    }

    @Override
    public void messagePosted(final Message event) {
        if (event.getObject(MessageProperty.ACCOUNT) == account) {
            EventQueue.invokeLater(() -> {
                switch (event.getEvent()) {
                    case TRANSACTION_ADD:
                    case TRANSACTION_REMOVE:
                        getTransactions();
                        break;
                    default: // ignore any other messages that don't matter

                        break;
                }
            });
        }
        super.messagePosted(event); // this will fire the update

    }

    @Override
    public boolean getAscending() {
        return ascending;
    }

    @Override
    public int getSortedColumn() {
        return sortColumn;
    }

    // private static String[] cNames = {"Date", "Action", "Investment", "C", "Quantity", "Price", "Total"};

    @Override
    public void sortColumn(final int col, final boolean ascending1) {
        this.ascending = ascending1;
        this.sortColumn = col;
        switch (getColumnMapping(col)) {
            case 0:
                Arrays.sort(transactions, Comparators.getTransactionByDate());
                break;
            case 1:
                Arrays.sort(transactions, Comparators.getTransactionByType());
                break;
            case 2:
                Arrays.sort(transactions, Comparators.getTransactionBySecurity());
                break;
            default:
                Arrays.sort(transactions, Comparators.getTransactionByDate());
                break;
        }
        fireTableDataChanged();
    }
}
