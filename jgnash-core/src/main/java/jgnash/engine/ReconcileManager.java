/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
package jgnash.engine;

import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages the reconciliation options.
 * <p/>
 * Both reconcileIncomeExpense and reconcileBothSides can be false, but
 * only one can be true
 *
 * @author Craig Cavanaugh
 * @version $Id: ReconcileManager.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class ReconcileManager {
    private static final String RECONCILE_INCOMEEXPENSE = "reconcileIncomeExpense";

    private static final String RECONCILE_BOTHSIDES = "reconcileBothSides";

    private static boolean reconcileIncomeExpense;

    private static boolean reconcileBothSides;

    private ReconcileManager() {
    }

    static {
        Preferences p = Preferences.userNodeForPackage(ReconcileManager.class);

        reconcileIncomeExpense = p.getBoolean(RECONCILE_INCOMEEXPENSE, false);
        reconcileBothSides = p.getBoolean(RECONCILE_BOTHSIDES, true);
    }

    /**
     * Set so the income or expense side of a transaction is
     * automatically reconciled
     *
     * @param reconcile true if income and expense accounts should be
     *                  automatically reconciled
     */
    public static void setAutoReconcileIncomeExpense(final boolean reconcile) {
        reconcileIncomeExpense = reconcile;
        Preferences p = Preferences.userNodeForPackage(ReconcileManager.class);
        p.putBoolean(RECONCILE_INCOMEEXPENSE, reconcileIncomeExpense);

        if (reconcile) {
            setAutoReconcileBothSides(false);
        }
    }

    /**
     * Determines if the income or expense side of a transaction should
     * be automatically reconciled
     *
     * @return true if income and expense accounts should automatically be
     *         reconciled.
     */
    public static boolean getAutoReconcileIncomeExpense() {
        return reconcileIncomeExpense;
    }

    /**
     * Set so both sides of a double entry transaction are automatically
     * reconciled
     *
     * @param reconcile true if income and expense accounts should be
     *                  automatically reconciled
     */
    public static void setAutoReconcileBothSides(final boolean reconcile) {
        reconcileBothSides = reconcile;
        Preferences p = Preferences.userNodeForPackage(ReconcileManager.class);
        p.putBoolean(RECONCILE_BOTHSIDES, reconcileBothSides);

        if (reconcile) {
            setAutoReconcileIncomeExpense(false);
        }
    }

    /**
     * Determines if both sides of a transaction should be automatically reconciled
     *
     * @return true if income and expense accounts should automatically be
     *         reconciled.
     */
    public static boolean getAutoReconcileBothSides() {
        return reconcileBothSides;
    }

    public static boolean isAutoReconcileDisabled() {
        return !reconcileBothSides && !reconcileIncomeExpense;
    }

    /**
     * Disables auto reconciliation
     */
    public static void setDoNotAutoReconcile() {
        setAutoReconcileBothSides(false);
        setAutoReconcileIncomeExpense(false);
    }

    /**
     * Sets the reconciled state of the transaction using the rules set
     * by the user
     *
     * @param account    Base account
     * @param t          Transaction to reconcile
     * @param reconciled Reconciled state
     */
    public static void reconcileTransaction(final Account account, final Transaction t, final ReconciledState reconciled) {
        t.setReconciled(account, reconciled); // mark transaction reconciled

        if (getAutoReconcileBothSides()) {
            t.setReconciled(reconciled);
        } else if (getAutoReconcileIncomeExpense()) {
            List<TransactionEntry> entries = t.getTransactionEntries();

            for (TransactionEntry entry : entries) {

                Account c = entry.getCreditAccount();
                if (c.instanceOf(AccountType.INCOME) || c.instanceOf(AccountType.EXPENSE)) {
                    entry.setCreditReconciled(ReconciledState.RECONCILED);
                }

                Account d = entry.getDebitAccount();
                if (d.instanceOf(AccountType.INCOME) || d.instanceOf(AccountType.EXPENSE)) {
                    entry.setDebitReconciled(ReconciledState.RECONCILED);
                }
            }
        }
    }

    /**
     * Sets the reconciled state of the transaction using the rules set by the user
     *
     * @param account    Base account
     * @param t          Transaction to reconcile
     * @param reconciled ReconciledState.RECONCILED if true; ReconciledState.NOT_RECONCILED if false
     */
    public static void reconcileTransaction(final Account account, final Transaction t, final boolean reconciled) {
        reconcileTransaction(account, t, reconciled ? ReconciledState.RECONCILED : ReconciledState.NOT_RECONCILED);
    }
}
