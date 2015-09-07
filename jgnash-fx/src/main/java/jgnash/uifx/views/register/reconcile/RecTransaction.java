/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.uifx.views.register.reconcile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.util.NotNull;

/**
 * Decorator around a Transaction to maintain the original reconciledState state
 *
 * @author Craig Cavanaugh
 */
class RecTransaction implements Comparable<RecTransaction> {
    private ReconciledState reconciledState;

    private final Transaction transaction;

    RecTransaction(@NotNull final Transaction transaction, @NotNull final ReconciledState reconciledState) {
        Objects.requireNonNull(transaction);
        Objects.requireNonNull(reconciledState);

        this.transaction = transaction;
        this.reconciledState = reconciledState;
    }

    public LocalDate getDate() {
        return transaction.getLocalDate();
    }

    public String getNumber() {
        return transaction.getNumber();
    }

    public String getPayee() {
        return transaction.getPayee();
    }

    public ReconciledState getReconciledState() {
        return reconciledState;
    }

    public void setReconciledState(final ReconciledState reconciledState) {
        this.reconciledState = reconciledState;
    }

    BigDecimal getAmount(final Account a) {
        if (transaction instanceof InvestmentTransaction && a.memberOf(AccountGroup.INVEST)) {
            return ((InvestmentTransaction) transaction).getMarketValue(transaction.getLocalDate())
                    .add(transaction.getAmount(a));
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
            final RecTransaction other = (RecTransaction) obj;

            if (transaction.equals(other.transaction) && reconciledState == other.reconciledState) {
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
