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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;

import jgnash.engine.Transaction;

/**
 * Predicate for age of a {@code Transaction}.
 *
 * @author Craig Cavanaugh
 */
public class TransactionAgePredicate implements Predicate<Transaction> {

    private final int age;
    private final ChronoUnit chronoUnit;

    public TransactionAgePredicate(final ChronoUnit chronoUnit, final int age) {
        this.chronoUnit = chronoUnit;
        this.age = age;
    }

    @Override
    public boolean test(final Transaction transaction) {
        if (chronoUnit == ChronoUnit.FOREVER) {
            return true;
        }
		return chronoUnit.between(transaction.getLocalDate(), LocalDate.now()) <= age;
    }
}
