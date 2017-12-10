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
package jgnash.engine;

import jgnash.util.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Utility Decorator around a Transaction to maintain the original reconciledState state. This class is not intended
 * to be serialized.
 *
 * @author Craig Cavanaugh
 */
public class RecTransaction implements Comparable<RecTransaction> {
    private ReconciledState reconciledState;

    private final Transaction transaction;

    public RecTransaction(@NotNull final Transaction transaction, @NotNull final ReconciledState reconciledState) {
        Objects.requireNonNull(transaction);
        Objects.requireNonNull(reconciledState);

        this.transaction = transaction;
        this.reconciledState = reconciledState;
    }

    public LocalDate getDate() {
        return getTransaction().getLocalDate();
    }

    public String getNumber() {
        return getTransaction().getNumber();
    }

    public String getPayee() {
        return getTransaction().getPayee();
    }

    public ReconciledState getReconciledState() {
        return reconciledState;
    }

    public void setReconciledState(final ReconciledState reconciledState) {
        this.reconciledState = reconciledState;
    }

    public BigDecimal getAmount(final Account a) {
        if (getTransaction() instanceof InvestmentTransaction && a.memberOf(AccountGroup.INVEST)) {
            return ((InvestmentTransaction) getTransaction()).getMarketValue(getTransaction().getLocalDate())
                    .add(getTransaction().getAmount(a));
        }

        return getTransaction().getAmount(a);
    }

    @Override
    public int hashCode() {
        return getTransaction().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        boolean result = false;

        if (obj instanceof RecTransaction) {
            final RecTransaction other = (RecTransaction) obj;

            if (getTransaction().equals(other.getTransaction()) && reconciledState == other.reconciledState) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public int compareTo(@NotNull final RecTransaction t) {
        return getTransaction().compareTo(t.getTransaction());
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
