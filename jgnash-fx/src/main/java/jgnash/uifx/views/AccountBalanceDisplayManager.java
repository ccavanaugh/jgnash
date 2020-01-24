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
package jgnash.uifx.views;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;

import java.math.BigDecimal;
import java.util.prefs.Preferences;

/**
 * AccountBalanceDisplayManager converts the account balances according to the selected displaying mode.
 * <p>
 * If accounting balances mode is active, then there will not be done any conversion.
 * <p>
 * If the normally positive balances mode is active, then the balances of account groups income, equity and liability
 * will be negated.
 *
 * @author Peter Vida
 * @author Craig Cavanaugh
 */
public class AccountBalanceDisplayManager {

    private static final String ACCOUNT_BALANCE_DISPLAY_MODE = "accountBalanceDisplayMode";

    private static final ObjectProperty<AccountBalanceDisplayMode> accountBalanceDisplayMode
            = new SimpleObjectProperty<>();

    static {
        final Preferences p = Preferences.userNodeForPackage(AccountBalanceDisplayManager.class);
        accountBalanceDisplayMode().set(AccountBalanceDisplayMode.valueOf(p.get(ACCOUNT_BALANCE_DISPLAY_MODE,
                AccountBalanceDisplayMode.NONE.name())));
    }

    private AccountBalanceDisplayManager() {
        // utility class
    }

    private static BigDecimal reverseCredit(final AccountType accountType, final BigDecimal balance) {
        if (accountType.getAccountGroup() == AccountGroup.EQUITY
                || accountType.getAccountGroup() == AccountGroup.INCOME
                || accountType.getAccountGroup() == AccountGroup.LIABILITY) {
            return balance.negate();
        }
        return balance;
    }

    private static BigDecimal reverseIncomeAndExpense(final AccountType accountType, final BigDecimal balance) {
        if (accountType.getAccountGroup() == AccountGroup.INCOME
                || accountType.getAccountGroup() == AccountGroup.EXPENSE) {
            return balance.negate();
        }
        return balance;
    }

    public static BigDecimal convertToSelectedBalanceMode(final AccountType accountType, final BigDecimal balance) {
        switch (accountBalanceDisplayMode.get()) {
            case REVERSE_INCOME_EXPENSE:
                return reverseIncomeAndExpense(accountType, balance);
            case REVERSE_CREDIT:
                return reverseCredit(accountType, balance);
            case NONE:
            default:
                return balance;
        }
    }

    public static void setDisplayMode(final AccountBalanceDisplayMode newMode) {
        accountBalanceDisplayMode().setValue(newMode);

        final Preferences p = Preferences.userNodeForPackage(AccountBalanceDisplayManager.class);
        p.put(ACCOUNT_BALANCE_DISPLAY_MODE, accountBalanceDisplayMode().getValue().name());
    }

    public static ObjectProperty<AccountBalanceDisplayMode> accountBalanceDisplayMode() {
        return accountBalanceDisplayMode;
    }
}
