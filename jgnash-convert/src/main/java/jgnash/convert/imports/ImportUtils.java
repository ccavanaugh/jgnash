/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
package jgnash.convert.imports;

import java.util.Objects;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;

/**
 * @author Craig Cavanaugh
 */
public class ImportUtils {

    private ImportUtils() {
    }

    public static Account getRootExpenseAccount() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        return searchForRootType(engine.getRootAccount(), AccountType.EXPENSE);
    }

    public static Account getRootIncomeAccount() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        return searchForRootType(engine.getRootAccount(), AccountType.INCOME);
    }

    private static Account searchForRootType(final Account account, final AccountType accountType) {
        Account result = null;

        // search immediate top level accounts
        for (Account a : account.getChildren()) {
            if (a.getAccountType().equals(accountType)) {
                return a;
            }
        }

        // recursive search
        for (Account a : account.getChildren()) {
            result = searchForRootType(a, accountType);
            if (result != null) {
                break;
            }
        }

        return result;
    }

}
