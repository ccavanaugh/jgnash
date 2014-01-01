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
package jgnash.convert.imports;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.EngineFactory;

/**
 * @author Craig Cavanaugh
 */
public class ImportUtils {

    private ImportUtils() {
    }

    public static Account getRootExpenseAccount() {
        return searchForRootType(EngineFactory.getEngine(EngineFactory.DEFAULT).getRootAccount(), AccountType.EXPENSE);
    }

    public static Account getRootIncomeAccount() {
        return searchForRootType(EngineFactory.getEngine(EngineFactory.DEFAULT).getRootAccount(), AccountType.INCOME);
    }

    private static Account searchForRootType(Account account, AccountType type) {
        Account result = null;

        // search immediate top level accounts
        for (Account a : account.getChildren()) {
            if (a.instanceOf(type)) {
                return a;
            }
        }

        // recursive search
        for (Account a : account.getChildren()) {
            result = searchForRootType(a, type);
            if (result != null) {
                break;
            }
        }

        return result;
    }

}
