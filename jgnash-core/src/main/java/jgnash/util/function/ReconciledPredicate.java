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
package jgnash.util.function;

import java.util.function.Predicate;

import jgnash.engine.Account;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.util.NotNull;
import jgnash.util.Nullable;

/**
 * Predicate for the reconciled state of a transaction.
 *
 * @author Craig Cavanaugh
 */
public class ReconciledPredicate implements Predicate<Transaction> {

    private final Account account;
    private final ReconciledState reconciledState;

    public ReconciledPredicate(@NotNull Account account, @Nullable final ReconciledState reconciledState) {
        this.account = account;
        this.reconciledState = reconciledState;
    }

    @Override
    public boolean test(final Transaction transaction) {
        if (reconciledState == null) {
            return true;
        }
		return reconciledState == transaction.getReconciled(account);
    }
}
