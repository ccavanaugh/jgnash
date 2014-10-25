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
package jgnash.ui.reconcile;

import java.awt.EventQueue;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.table.AbstractTableModel;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.text.CommodityFormat;
import jgnash.ui.register.AccountBalanceDisplayManager;
import jgnash.ui.register.table.PackableTableModel;
import jgnash.util.DateUtils;
import jgnash.util.NotNull;
import jgnash.util.Resource;

/**
 * Model initializes itself by grabbing all of the transactions from the account and then filters for transactions that
 * have not been reconciled and are after the startDate(inclusive). Any new transactions added to the account that are
 * after the start date (reconciled or not) will be added to the list of transactions shown.
 *
 * @author Craig Cavanaugh
 */
public abstract class AbstractReconcileTableModel extends AbstractTableModel implements MessageListener, PackableTableModel {

    final Account account;

    private final Date closingDate;

    private final List<RecTransaction> list = new ArrayList<>();

    private final Resource rb = Resource.get();

    private final String[] cNames = {rb.getString("Column.Clr"), rb.getString("Column.Date"), rb.getString("Column.Num"),
            rb.getString("Column.Payee"), rb.getString("Column.Amount")};

    private final Class<?>[] cClass = {String.class, String.class, String.class, String.class, BigDecimal.class};

    private final int[] columnWidths = {0, 0, 0, 99, 0};

    private final DateFormat dateFormatter = DateUtils.getShortDateFormat();

    private final NumberFormat commodityFormatter;

    private final ReadWriteLock rwl = new ReentrantReadWriteLock(true);

    AbstractReconcileTableModel(final Account account, final Date closingDate) {
        Objects.requireNonNull(account);
        assert cNames.length == cClass.length;

        this.account = account;
        this.closingDate = (Date) closingDate.clone();

        commodityFormatter = CommodityFormat.getShortNumberFormat(account.getCurrencyNode());

        loadModel();

        MessageBus.getInstance().registerListener(this, MessageChannel.TRANSACTION);
    }

    @Override
    public int[] getPreferredColumnWeights() {
        return columnWidths;
    }

    private void loadModel() {
        rwl.writeLock().lock();

        try {
            for (Transaction t : account.getSortedTransactionList()) {
                if (reconcilable(t)) {
                    list.add(new RecTransaction(t));
                }
            }
        } finally {
            rwl.writeLock().unlock();
        }
    }

    private boolean reconcilable(final Transaction t) {
        return DateUtils.before(t.getDate(), closingDate) && t.getReconciled(account) != ReconciledState.RECONCILED && isShowable(t);
    }

    abstract boolean isShowable(Transaction t);

    /**
     * Returns the number of columns in the model. A {@code JTable} uses this method to determine how many columns
     * it should create and display by default.
     *
     * @return the number of columns in the model
     * @see #getRowCount
     */
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

    /**
     * Returns the number of rows in the model. A {@code JTable} uses this method to determine how many rows it
     * should display. This method should be quick, as it is called frequently during rendering.
     *
     * @return the number of rows in the model
     * @see #getColumnCount
     */
    @Override
    public int getRowCount() {
        rwl.readLock().lock();

        try {
            return list.size();
        } finally {
            rwl.readLock().unlock();
        }
    }

    /**
     * Returns the value for the cell at {@code columnIndex} and {@code rowIndex}.
     *
     * @param rowIndex    the row whose value is to be queried
     * @param columnIndex the column whose value is to be queried
     * @return the value Object at the specified cell
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        rwl.readLock().lock();

        try {
            RecTransaction t = list.get(rowIndex);

            switch (columnIndex) {
                case 0:
                    return t.getReconciled().toString();
                case 1:
                    return dateFormatter.format(t.getDate());
                case 2:
                    return t.getNumber();
                case 3:
                    return t.getPayee();
                case 4:
                    BigDecimal amount = AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), t.getAmount(account));
                    return this.commodityFormatter.format(amount);
                default:
                    return null;
            }
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public void messagePosted(final Message event) {
        if (event.getObject(MessageProperty.ACCOUNT).equals(account)) {

            rwl.writeLock().lock();

            try {

                final Transaction transaction = (Transaction) event.getObject(MessageProperty.TRANSACTION);

                EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        switch (event.getEvent()) {
                            case TRANSACTION_REMOVE:
                                RecTransaction trans = findTransaction(transaction);
                                if (trans != null) {
                                    int index = list.indexOf(trans);
                                    list.remove(index);
                                    fireTableRowsDeleted(index, index);
                                }
                                break;
                            case TRANSACTION_ADD:
                                if (isShowable(transaction)) {
                                    RecTransaction newTran = new RecTransaction(transaction);
                                    int index = Collections.binarySearch(list, newTran);
                                    if (index < 0) {
                                        index = -index - 1;
                                        list.add(index, newTran);
                                        fireTableRowsInserted(index, index);
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                    }
                });
            } finally {
                rwl.writeLock().unlock();
            }
        }

    }

    public BigDecimal getReconciledTotal() {
        BigDecimal sum = BigDecimal.ZERO;

        rwl.readLock().lock();

        try {
            for (RecTransaction t : list) {
                if (t.getReconciled() == ReconciledState.RECONCILED) {
                    sum = sum.add(t.getAmount(account));
                }
            }
        } finally {
            rwl.readLock().unlock();
        }

        return AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), sum);
    }

    private synchronized RecTransaction findTransaction(final Transaction t) {

        rwl.readLock().lock();

        try {
            if (t != null) {
                for (RecTransaction tran : list) {
                    if (tran.transaction == t) {
                        return tran;
                    }
                }
            }
            return null;
        } finally {
            rwl.readLock().unlock();
        }
    }

    void toggleReconciledState(final int index) {
        rwl.readLock().lock();

        try {
            RecTransaction t = list.get(index);

            if (t.getReconciled() == ReconciledState.RECONCILED) {
                t.setReconciled(ReconciledState.NOT_RECONCILED);
            } else {
                t.setReconciled(ReconciledState.RECONCILED);
            }
            fireTableRowsUpdated(index, index);
        } finally {
            rwl.readLock().unlock();
        }
    }

    void selectAll() {

        rwl.readLock().lock();

        try {
            for (RecTransaction tran : list) {
                tran.setReconciled(ReconciledState.RECONCILED);
            }
            fireTableRowsUpdated(0, list.size() - 1);
        } finally {
            rwl.readLock().unlock();
        }
    }

    void clearAll() {

        rwl.readLock().lock();

        try {
            for (RecTransaction tran : list) {
                tran.setReconciled(ReconciledState.NOT_RECONCILED);
            }
            fireTableRowsUpdated(0, list.size() - 1);
        } finally {
            rwl.readLock().unlock();
        }
    }

    void commitChanges(final ReconciledState reconciledState) {
        List<RecTransaction> transactions = null;

        // create a copy of the list to prevent concurrent modification errors
        rwl.readLock().lock();
        try {
            transactions = new ArrayList<>(list);
        } finally {
            rwl.readLock().unlock();
        }

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        for (final RecTransaction transaction : transactions) {
            if (transaction.getReconciled() != transaction.transaction.getReconciled(account)) {

                // Set to the requested reconcile state
                if (transaction.getReconciled() != ReconciledState.NOT_RECONCILED) {
                    engine.setTransactionReconciled(transaction.transaction, account, reconciledState);
                } else { // must not be reconciled or cleared
                    engine.setTransactionReconciled(transaction.transaction, account, transaction.getReconciled());
                }
            }
        }
    }

    /**
     * Decorator around a Transaction to maintain the original reconciled state
     */
    private class RecTransaction implements Comparable<RecTransaction> {

        private ReconciledState reconciled;

        Transaction transaction;

        RecTransaction(final Transaction transaction) {
            assert transaction != null;
            this.transaction = transaction;
            reconciled = transaction.getReconciled(account);
        }

        public Date getDate() {
            return transaction.getDate();
        }

        public String getNumber() {
            return transaction.getNumber();
        }

        public String getPayee() {
            return transaction.getPayee();
        }

        public ReconciledState getReconciled() {
            return reconciled;
        }

        public void setReconciled(final ReconciledState reconciled) {
            this.reconciled = reconciled;
        }

        BigDecimal getAmount(final Account a) {
            if (transaction instanceof InvestmentTransaction && a.memberOf(AccountGroup.INVEST)) {
                return ((InvestmentTransaction) transaction).getMarketValue(transaction.getDate()).add(transaction.getAmount(a));
            }

            return transaction.getAmount(a);
        }

        @Override
        public int hashCode() {
            return transaction.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {

            boolean result = false;

            if (obj != null && obj instanceof RecTransaction) {
                RecTransaction other = (RecTransaction) obj;

                if (transaction != null && other.transaction != null && transaction.equals(other.transaction) && reconciled == other.reconciled) {
                    result = true;
                }
            }

            return result;
        }

        @Override
        public int compareTo(@NotNull final RecTransaction t) {
            return transaction.compareTo(t.transaction);
        }
    }
}
