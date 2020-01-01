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

import jgnash.resource.util.ResourceUtils;

/**
 * Account Group class. Helps to categorize account types to make reporting easier and consistent.
 * 
 * @author Craig Cavanaugh
 */
public enum AccountGroup {

    ASSET(ResourceUtils.getString("AccountType.Asset")),
    EQUITY(ResourceUtils.getString("AccountType.Equity")),
    EXPENSE(ResourceUtils.getString("AccountType.Expense")),
    INCOME(ResourceUtils.getString("AccountType.Income")),
    INVEST(ResourceUtils.getString("AccountType.Investment")),
    LIABILITY(ResourceUtils.getString("AccountType.Liability")),
    ROOT(ResourceUtils.getString("AccountType.Root")),
    SIMPLEINVEST(ResourceUtils.getString("AccountType.SimpleInvestment")); // CD's, Treasuries, Etc.

    private final transient String description;

    AccountGroup(final String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
