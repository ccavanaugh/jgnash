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
package jgnash.ui.register;

/**
 * AccountBalanceDisplayingMode defines the modes how the account balances can be displayed.
 * 
 * @author Peter Vida
 * @author Craig Cavanaugh
 *
 */
public enum AccountBalanceDisplayMode {
    NONE(1),
    REVERSE_CREDIT(2),
    REVERSE_INCOME_EXPENSE(3);

    private final transient int value;

    private AccountBalanceDisplayMode(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static AccountBalanceDisplayMode createFromInt(final int value) {
        if (value == NONE.getValue()) {
            return AccountBalanceDisplayMode.NONE;
        } else if (value == REVERSE_CREDIT.getValue()) {
            return AccountBalanceDisplayMode.REVERSE_CREDIT;
        } else if (value == REVERSE_INCOME_EXPENSE.getValue()) {
            return AccountBalanceDisplayMode.REVERSE_INCOME_EXPENSE;
        } else {
            throw new IllegalArgumentException();
        }
    }
}
