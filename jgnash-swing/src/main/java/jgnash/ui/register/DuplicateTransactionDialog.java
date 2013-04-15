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
package jgnash.ui.register;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import jgnash.engine.Account;
import jgnash.engine.EngineFactory;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.util.DateUtils;
import jgnash.util.Resource;

/**
 * A Dialog for duplicating a transaction. If the date for the duplicate transaction is the same as the current date
 * (month, day, year), then the current date and time is used to maintain entry order.
 *
 * @author Craig Cavanaugh
 */
class DuplicateTransactionDialog extends DateChkNumberDialog {

    private Transaction transaction;

    private SwingWorker<Transaction, Void> worker;

    /**
     * Creates new DuplicateTransactionDialog.<b> null transactions and SplitEntryTransactions are ignored to prevent
     * bad results.
     *
     * @param a the account for the transaction. This cannot be null.
     * @param t the transaction to duplicate. This cannot be null.
     * @return DuplicateTransactionDialog instance
     */
    static DuplicateTransactionDialog showDialog(final Account a, final Transaction t) {
        if (t != null) {
            DuplicateTransactionDialog d = new DuplicateTransactionDialog(a, t);
            d.setVisible(true);

            return d;
        }
        return null;
    }

    /**
     * Creates new form DuplicateTransactionDialog
     *
     * @param a the account for the transaction. This cannot be null.
     * @param t the transaction to duplicate. This cannot be null.
     */
    private DuplicateTransactionDialog(final Account a, final Transaction t) {
        super(a, Resource.get().getString("Title.DuplicateTransaction"));

        if (a == null) {
            throw new IllegalArgumentException("Account parameter was null");
        }

        if (t == null) {
            throw new IllegalArgumentException("Transaction parameter was null");
        }

        this.transaction = t;

        // make the best guess about what the check number should be
        if (t.getNumber() != null && !t.getNumber().isEmpty()) {
            String n = a.getNextTransactionNumber();
            if (n != null) {
                numberCombo.setText(n);
            } else {
                numberCombo.setText(t.getNumber());
            }
        } else { // for it to null, otherwise the base class picks a default
            numberCombo.setText(null);
        }
    }

    @Override
    public void okAction() {

        worker = new SwingWorker<Transaction, Void>() {

            @Override
            public Transaction doInBackground() {
                Transaction clone = null; // get a clone

                try {
                    clone = (Transaction) transaction.clone();

                    Date today = DateUtils.today(); // get today's date

                    if (today.equals(datePanel.getDate())) {
                        clone.setDate(new Date()); // maintain entry order
                    } else {
                        clone.setDate(datePanel.getDate()); // set the new date
                    }

                    clone.setNumber(numberCombo.getText()); // set the transaction number

                    // clear the reconciled state of the transaction
                    clone.setReconciled(ReconciledState.NOT_RECONCILED);
                } catch (CloneNotSupportedException e) {
                    Logger.getLogger(DuplicateTransactionDialog.class.getName()).log(Level.SEVERE, e.toString(), e);
                }

                return clone;
            }

            @Override
            protected void done() {
                try {
                    Transaction clone = get();

                    EngineFactory.getEngine(EngineFactory.DEFAULT).addTransaction(clone); // add the transaction

                } catch (InterruptedException | ExecutionException e) {
                    Logger.getLogger(DuplicateTransactionDialog.class.getName()).log(Level.SEVERE, e.toString(), e);
                }
            }
        };

        worker.execute();

        super.okAction();
    }

    public Transaction getTransaction() {

        Transaction newTransaction = null;

        try {
            if (worker != null) {   // null if duplicate action was canceled
                newTransaction = worker.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            Logger.getLogger(DuplicateTransactionDialog.class.getName()).log(Level.SEVERE, e.toString(), e);
        }

        return newTransaction;
    }
}
