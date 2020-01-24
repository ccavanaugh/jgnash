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
package jgnash.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jgnash.time.DateUtils;
import jgnash.util.NotNull;

/**
 * Manages the reconciliation options and provides utility functions for preserving account reconciliation attributes.
 * <p>
 * Both reconcileIncomeExpense and reconcileBothSides can be false, but only one can be true.
 *
 * @author Craig Cavanaugh
 */
public class ReconcileManager {
    private static final String RECONCILE_INCOME_EXPENSE = "reconcileIncomeExpense";

    private static final String RECONCILE_BOTH_SIDES = "reconcileBothSides";

    private static boolean reconcileIncomeExpense;

    private static boolean reconcileBothSides;

    private ReconcileManager() {
    }

    static {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            reconcileIncomeExpense = engine.getBoolean(RECONCILE_INCOME_EXPENSE, false);
            reconcileBothSides = engine.getBoolean(RECONCILE_BOTH_SIDES, true);
        }
    }

    /**
     * Set so the income or expense side of a transaction is
     * automatically reconciled.
     *
     * @param reconcile true if income and expense accounts should be
     *                  automatically reconciled
     */
    public static void setAutoReconcileIncomeExpense(final boolean reconcile) {
        reconcileIncomeExpense = reconcile;

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        if (engine != null) {
            engine.putBoolean(RECONCILE_INCOME_EXPENSE, reconcileIncomeExpense);
        }

        if (reconcile) {
            setAutoReconcileBothSides(false);
        }
    }

    /**
     * Determines if the income or expense side of a transaction should
     * be automatically reconciled.
     *
     * @return true if income and expense accounts should automatically be
     * reconciled.
     */
    public static boolean getAutoReconcileIncomeExpense() {
        return reconcileIncomeExpense;
    }

    /**
     * Set so both sides of a double entry transaction are automatically
     * reconciled.
     *
     * @param reconcile true if income and expense accounts should be
     *                  automatically reconciled
     */
    public static void setAutoReconcileBothSides(final boolean reconcile) {
        reconcileBothSides = reconcile;

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        if (engine != null) {
            engine.putBoolean(RECONCILE_BOTH_SIDES, reconcileBothSides);
        }

        if (reconcile) {
            setAutoReconcileIncomeExpense(false);
        }
    }

    /**
     * Determines if both sides of a transaction should be automatically reconciled.
     *
     * @return true if income and expense accounts should automatically be
     * reconciled.
     */
    public static boolean getAutoReconcileBothSides() {
        return reconcileBothSides;
    }

    public static boolean isAutoReconcileDisabled() {
        return !reconcileBothSides && !reconcileIncomeExpense;
    }

    /**
     * Disables auto reconciliation.
     */
    public static void setDoNotAutoReconcile() {
        setAutoReconcileBothSides(false);
        setAutoReconcileIncomeExpense(false);
    }

    /**
     * Sets the reconciled state of the transaction using the rules set
     * by the user.
     *
     * @param account    Base account
     * @param t          Transaction to reconcile
     * @param reconciled Reconciled state
     */
    public static void reconcileTransaction(final Account account, final Transaction t, final ReconciledState reconciled) {

        // mark transaction reconciled for the primary account
        t.setReconciled(account, reconciled);

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

    public static void reconcileTransactions(final Account account, final List<RecTransaction> list,
                                             final ReconciledState reconciledState) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        // create a copy of the list to prevent concurrent modification errors
        final List<RecTransaction> transactions = new ArrayList<>(list);

        // Set to the requested reconcile state, ignore if no change is detected
        transactions.parallelStream().filter(transaction -> transaction.getReconciledState()
                != transaction.getTransaction().getReconciled(account)).forEach(recTransaction -> {

            // Set to the requested reconcile state
            if (recTransaction.getReconciledState() != ReconciledState.NOT_RECONCILED) {
                engine.setTransactionReconciled(recTransaction.getTransaction(), account, reconciledState);
            } else { // must have been reconciled or cleared
                engine.setTransactionReconciled(recTransaction.getTransaction(), account, recTransaction.getReconciledState());
            }
        });
    }

    /**
     * Sets a date attribute for an account.
     *
     * @param account account that is being updated
     * @param attribute key for the date attribute
     * @param date date value
     */
    public static void setAccountDateAttribute(@NotNull final Account account, @NotNull final String attribute,
                                               @NotNull final LocalDate date) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        engine.setAccountAttribute(account, attribute, Long.toString(DateUtils.asEpochMilli(date)));
    }

    /**
     * Returns a date attribute for an account.
     *
     * @param account {@code Account} that is being queried
     * @param attribute key for the date attribute
     * @return an {@code Optional} containing the date if previously set
     */
    public static Optional<LocalDate> getAccountDateAttribute(@NotNull final Account account,
                                                              @NotNull final String attribute) {
        final String value = account.getAttribute(attribute);
        if (value != null) {
            return Optional.ofNullable(DateUtils.asLocalDate(Long.parseLong(value)));
        }
        return Optional.empty();
    }

    /**
     * Sets a {@code BigDecimal} attribute for an account.
     *
     * @param account account that is being updated
     * @param attribute key for the {@code BigDecimal} attribute
     * @param decimal decimal value
     */
    public static void setAccountBigDecimalAttribute(@NotNull final Account account, @NotNull final String attribute,
                                                     @NotNull final BigDecimal decimal) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        engine.setAccountAttribute(account, attribute, decimal.toString());
    }

    /**
     * Returns a {@code BigDecimal} attribute for an account.
     *
     * @param account {@code Account} that is being queried
     * @param attribute key for the {@code BigDecimal} attribute
     * @return an {@code Optional} containing the {@code BigDecimal} if previously set
     */
    public static Optional<BigDecimal> getAccountBigDecimalAttribute(@NotNull final Account account,
                                                                     @NotNull final String attribute) {
        final String value = account.getAttribute(attribute);
        if (value != null) {
            return Optional.of(new BigDecimal(value));
        }
        return Optional.empty();
    }
}
