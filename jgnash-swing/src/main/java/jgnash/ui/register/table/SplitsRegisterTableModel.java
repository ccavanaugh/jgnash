/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.table.AbstractTableModel;

import jgnash.engine.Account;
import jgnash.engine.TransactionEntry;
import jgnash.util.ResourceUtils;

/**
 * Table model for displaying TransactionEntry objects
 *
 * @author Craig Cavanaugh
 */
public class SplitsRegisterTableModel extends AbstractTableModel implements AccountTableModel {

    private final ResourceBundle rb = ResourceUtils.getBundle();

    /**
     * Names of the columns
     */
    private String[] cNames = { rb.getString("Column.Account"), rb.getString("Column.Clr"), rb.getString("Column.Memo"),
            rb.getString("Column.Credit"), rb.getString("Column.Debit"), rb.getString("Column.Balance") };

    private final Class<?>[] cClass = { String.class, String.class, String.class, ShortCommodityStyle.class,
            ShortCommodityStyle.class, FullCommodityStyle.class };

    private final Account account;

    private final SandBoxSplitTransaction transaction;

    public SplitsRegisterTableModel(final Account account, final String[] names, final List<TransactionEntry> splits) {
        this.account = account;
        transaction = new SandBoxSplitTransaction(splits);
        cNames = Arrays.copyOf(names, names.length);
    }

    @Override
    public Account getAccount() {
        return account;
    }

    @Override
    public int getColumnCount() {
        return cNames.length;
    }

    @Override
    public String getColumnName(final int column) {
        return cNames[column];
    }

    @Override
    public Class<?> getColumnClass(final int column) {
        return cClass[column];
    }

    @Override
    public Object getValueAt(final int row, final int col) {

        TransactionEntry t = transaction.getSplitAt(row);
        Account creditAccount = t.getCreditAccount();

        switch (col) {
            case 0:
                if (creditAccount != account) {
                    return creditAccount.getName();
                }
                return t.getDebitAccount().getName();
            case 1:
                return t.getReconciled(account).toString();
            case 2:
                return t.getMemo();
            case 3:
                if (creditAccount == account) {
                    return t.getAmount(account);
                }
                return null;
            case 4:
                if (creditAccount != account) {
                    return t.getAmount(account).abs();
                }
                return null;
            case 5:
                return transaction.getRunningBalance(row + 1);
            default:
                return "";
        }
    }

    @Override
    public int getRowCount() {
        return transaction.getNumSplits();
    }

    public void addTransaction(final TransactionEntry t) {
        transaction.addSplit(t);
        fireTableDataChanged();
    }

    public void removeTransaction(final int index) {
        transaction.removeSplit(index);
        fireTableDataChanged();
    }

    public TransactionEntry getTransactionAt(final int index) {
        return transaction.getSplitAt(index);
    }

    public void modifyTransaction(final TransactionEntry oldTrans, final TransactionEntry newTrans) {
        transaction.replaceSplit(oldTrans, newTrans);
        fireTableDataChanged();
    }

    public ArrayList<TransactionEntry> getSplits() {
        return transaction.getSplits();
    }

    public BigDecimal getBalance() {
        return transaction.getBalance();
    }

    /**
     * This is a sand box class to simulate a split transaction
     * without the mess of dealing with uID's and ownership
     */
    class SandBoxSplitTransaction {

        private final ArrayList<TransactionEntry> splits;

        SandBoxSplitTransaction(List<TransactionEntry> list) {
            if (list != null) {
                splits = new ArrayList<>(list);
            } else {
                splits = new ArrayList<>();
            }
        }

        public int getNumSplits() {
            return splits.size();
        }

        TransactionEntry getSplitAt(final int index) {
            return splits.get(index);
        }

        void removeSplit(final int index) {
            splits.remove(index);
        }

        void addSplit(final TransactionEntry t) {
            splits.add(t);
            Collections.sort(splits);
        }

        void replaceSplit(final TransactionEntry oldTrans, final TransactionEntry newTrans) {
            int index = splits.indexOf(oldTrans);
            if (index != -1) {
                splits.set(index, newTrans);
                Collections.sort(splits);
            }
        }

        public ArrayList<TransactionEntry> getSplits() {
            return splits;
        }

        /**
         * Calculates the balance up to {@code index}.
         * @param index transaction index
         * @return balance for the index
         */
        BigDecimal getRunningBalance(final int index) {
            BigDecimal balance = getSplitAt(0).getAmount(account);
            for (int i = 1; i < index; i++) {
                balance = balance.add(getSplitAt(i).getAmount(account));
            }
            return balance;
        }

        public BigDecimal getBalance() {
            return getRunningBalance(getNumSplits());
        }
    }
}