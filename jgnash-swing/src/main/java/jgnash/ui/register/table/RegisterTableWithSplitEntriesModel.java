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

import jgnash.engine.Account;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionType;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageProperty;
import jgnash.ui.register.AccountBalanceDisplayManager;

/**
 * Register table model supporting split entries transactions.
 * 
 * @author Craig Cavanaugh
 * @author Vincent Frison
 *
 * @see jgnash.ui.register.RegisterFactory#getTableModel(boolean, jgnash.engine.Account, boolean)
 */
public class RegisterTableWithSplitEntriesModel extends RegisterTableModel {

    private ArrayList<TransactionWrapper> data; // internal data is a list which contains all account transactions *including their possible split entries*

    private boolean showSplitDetails = false;

    public RegisterTableWithSplitEntriesModel(final Account account, final String[] names, final boolean detailSplits) {
        super(account, names, _clazz);
        this.showSplitDetails = detailSplits;
        updateData();
    }

    @Override
    protected Object getInternalValueAt(final int row, final int col) {
        TransactionWrapper wrapper = data.get(row);

        BigDecimal amount;

        if (wrapper.entry == null) {
            amount = wrapper.transaction.getAmount(account);
        } else {
            amount = wrapper.entry.getAmount(account);
        }

        int signum = amount.signum();

        /* only show details if showSplitDetails is true and account
         * does not directly contain the transaction */
        boolean showDetail = wrapper.entry != null && showSplitDetails;

        switch (col) {
            case 0:
                if (showDetail) {
                    return null;
                }
                return wrapper.transaction.getDate();
            case 1:
                if (showDetail) {
                    return null;
                }
                return wrapper.transaction.getNumber();
            case 2:
                if (showDetail) {
                    return null;
                }
                return wrapper.transaction.getPayee();
            case 3:
                if (showDetail) {
                    return null;
                }
                return wrapper.transaction.getMemo();
            case 4:
                if (wrapper.entry != null && showDetail) {
                    TransactionEntry _t = wrapper.entry;
                    if (_t.getCreditAccount() != account) {
                        return "   - " + _t.getCreditAccount().getName();
                    }
                    return "   - " + _t.getDebitAccount().getName();
                } else if (wrapper.entry == null && wrapper.transaction.getTransactionType() == TransactionType.DOUBLEENTRY) {
                    TransactionEntry _t = wrapper.transaction.getTransactionEntries().get(0);

                    if (_t.getCreditAccount() != account) {
                        return _t.getCreditAccount().getName();
                    }
                    return _t.getDebitAccount().getName();
                } else if (wrapper.entry == null && wrapper.transaction.getTransactionType() == TransactionType.SINGLENTRY) {
                    TransactionEntry _t = wrapper.transaction.getTransactionEntries().get(0);
                    return _t.getCreditAccount().getName();
                } else if (wrapper.transaction.getTransactionType() == TransactionType.SPLITENTRY) {
                    Transaction _t = wrapper.transaction;
                    return "[ " + _t.size() + " " + split + " ]";
                } else if (wrapper.transaction instanceof InvestmentTransaction) {
                    return ((InvestmentTransaction) wrapper.transaction).getInvestmentAccount().getName();
                } else {
                    System.out.println("here");
                    return ERROR;
                }
            case 5:
                if (wrapper.entry == null) {
                    return wrapper.transaction.getReconciled(account) == ReconciledState.NOT_RECONCILED ? null : reconcileSymbol;
                }
                return wrapper.entry.getReconciled(account) == ReconciledState.NOT_RECONCILED ? null : reconcileSymbol;
            case 6:
                if (signum >= 0) {
                    return amount;
                }
                return null;
            case 7:
                if (signum < 0) {
                    return amount.abs();
                }
                return null;
            case 8:
                if (showDetail) {
                    return null;
                }
                return getBalanceAt(row);

            default:
                return ERROR;
        }
    }

    private void updateData() {
        updateData(0);
    }

    /**
     * Update the internal data from the specified index. Note that this index is not relative to the account
     * transaction list but to the internal data.
     * 
     * @param startIndex internal data index from which transactions need to updated.
     */
    private void updateData(final int startIndex) {
        if (data == null) {
            data = new ArrayList<>(0);
        }
        while (data.size() > startIndex) {
            data.remove(startIndex);
        }

        for (Transaction t : account.getSortedTransactionList()) {
            if (data.size() >= startIndex) {
                data.add(new TransactionWrapper(t));
                if (t.getTransactionType() == TransactionType.SPLITENTRY && showSplitDetails) {

                    /* Only detail split entries if 2 or more entries impact this account */
                    int splitImpact = 0;

                    // count the number of impacting entry(s)
                    for (TransactionEntry e : t.getTransactionEntries()) {
                        if (e.getAmount(account).signum() != 0) {
                            splitImpact++;
                        }
                    }

                    // load only the entries that impact this account
                    if (splitImpact > 1) {
                        for (TransactionEntry e : t.getTransactionEntries()) {
                            if (e.getAmount(account).signum() != 0) {
                                data.add(new TransactionWrapper(t, e));
                            }
                        }
                    }
                }
            }
        }

        // updating the balance cache too
        balanceCache.ensureCapacity(data.size());
        balanceCache.clear(startIndex);
    }

    /*
     * Methods from AbstractRegisterTableModel which need to be overwritten
     */
    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public Transaction getTransactionAt(final int index) {
        return data.get(index).transaction;
    }

    @Override
    public int indexOf(final Transaction t) {

        for (TransactionWrapper w : data) {
            if (w.transaction.equals(t)) {
                return data.indexOf(w);
            }
        }

        return 0;
    }

    @Override
    BigDecimal getBalanceAt(final int index) {
        if (balanceCache.get(index) != null) {
            return AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), balanceCache.get(index));
        }

        // cannot rely on account.getBalanceAt()
        BigDecimal balance = null;

        Transaction t = data.get(index).transaction;

        if (account.contains(t) && data.get(index).entry == null) { // top level only
            balance = BigDecimal.ZERO;

            for (int i = 0; i <= index; i++) {
                Transaction tran = data.get(i).transaction;
                if (data.get(i).entry == null && account.contains(tran)) {
                    balance = balance.add(tran.getAmount(account));
                }
            }
        }
        balanceCache.set(index, balance);
        return AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), balance);
    }

    @Override
    public void messagePosted(final Message event) {
        if (account.equals(event.getObject(MessageProperty.ACCOUNT))) {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    switch (event.getEvent()) {
                        case FILE_CLOSING:
                            unregister();
                            break;
                        case TRANSACTION_ADD:
                            Transaction t = (Transaction) event.getObject(MessageProperty.TRANSACTION);
                            updateData();
                            int index = indexOfWrapper(t);

                            if (index >= 0) {
                                fireTableRowsInserted(index, index);
                            }
                            break;
                        case TRANSACTION_REMOVE:
                            updateData();
                            fireTableDataChanged();
                            break;
                        default:
                            break;
                    }
                }
            });
        }
    }

    private int indexOfWrapper(final Transaction transaction) {
        int index = -1;

        for (int i = 0; i < data.size(); i++) {
            TransactionWrapper wrapper = data.get(i);

            if (wrapper.transaction == transaction) {
                index = i;
                break;
            }
        }

        return index;
    }

    private static class TransactionWrapper {

        final Transaction transaction;

        TransactionEntry entry;

        TransactionWrapper(final Transaction transaction, final TransactionEntry entry) {
            this.transaction = transaction;
            this.entry = entry;
        }

        TransactionWrapper(final Transaction transaction) {
            this.transaction = transaction;
        }
    }
}
