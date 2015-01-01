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
package jgnash.engine;

import jgnash.util.Resource;

/**
 * Account Group class. Helps to categorize account types to make reporting easier and consistent.
 * 
 * @author Craig Cavanaugh
 */
public enum AccountGroup {

    ASSET(Resource.get().getString("AccountType.Asset")),
    EQUITY(Resource.get().getString("AccountType.Equity")),
    EXPENSE(Resource.get().getString("AccountType.Expense")),
    INCOME(Resource.get().getString("AccountType.Income")),
    INVEST(Resource.get().getString("AccountType.Investment")),
    LIABILITY(Resource.get().getString("AccountType.Liability")),
    ROOT(Resource.get().getString("AccountType.Root")),
    SIMPLEINVEST(Resource.get().getString("AccountType.SimpleInvestment")); // CD's, Treasuries, Etc.

    private final transient String description;

    private AccountGroup(final String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
