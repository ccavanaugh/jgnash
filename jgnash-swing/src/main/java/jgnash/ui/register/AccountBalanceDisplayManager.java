/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;

/**
 * AccountBalanceDisplay converts the account balances according to the selected displaying mode.
 * 
 * If accounting balances mode is active, then there will not be done any conversion.
 * 
 * If the normally positive balances mode is active, then the balances of account groups income, equity and liability
 * will be negated.
 * 
 * @author Peter Vida
 * @author Craig Cavanaugh
 *
 */
public class AccountBalanceDisplayManager {

    private static AccountBalanceDisplayMode accountBalanceDisplayingMode;

    private static final String ACCOUNT_BALANCE_DISPLAY_MODE = "accountbalancedisplayingmode";

    private static final List<WeakReference<ActionListener>> actionListeners = new ArrayList<>();

    static {
        Preferences p = Preferences.userNodeForPackage(AccountBalanceDisplayManager.class);
        accountBalanceDisplayingMode = AccountBalanceDisplayMode.createFromInt(p.getInt(ACCOUNT_BALANCE_DISPLAY_MODE, AccountBalanceDisplayMode.NONE.getValue()));
    }

    private AccountBalanceDisplayManager() { // utility class

    }

    private static BigDecimal reverseCredit(final AccountType accountType, final BigDecimal balance) {
        if (accountType.getAccountGroup() == AccountGroup.EQUITY || accountType.getAccountGroup() == AccountGroup.INCOME || accountType.getAccountGroup() == AccountGroup.LIABILITY) {
            return balance.negate();
        }
        return balance;
    }

    private static BigDecimal reverseIncomeAndExpense(final AccountType accountType, final BigDecimal balance) {
        if (accountType.getAccountGroup() == AccountGroup.INCOME || accountType.getAccountGroup() == AccountGroup.EXPENSE) {
            return balance.negate();
        }
        return balance;
    }

    public static BigDecimal convertToSelectedBalanceMode(final AccountType accountType, final BigDecimal balance) {
        if (getDisplayMode() == AccountBalanceDisplayMode.NONE) {
            return balance;
        } else if (getDisplayMode() == AccountBalanceDisplayMode.REVERSE_INCOME_EXPENSE) {
            return reverseIncomeAndExpense(accountType, balance);
        }
        return reverseCredit(accountType, balance);
    }

    public static void setDisplayMode(final AccountBalanceDisplayMode newMode) {
        accountBalanceDisplayingMode = newMode;
        Preferences p = Preferences.userNodeForPackage(AccountBalanceDisplayManager.class);
        p.putInt(ACCOUNT_BALANCE_DISPLAY_MODE, accountBalanceDisplayingMode.getValue());

        accountBalanceDisplayModeChanged();
    }

    public static AccountBalanceDisplayMode getDisplayMode() {
        return accountBalanceDisplayingMode;
    }

    public static void addAccountBalanceDisplayModeChangeListener(final ActionListener actionListener) {
        actionListeners.add(new WeakReference<>(actionListener));
    }

    private static void accountBalanceDisplayModeChanged() {

        Iterator<WeakReference<ActionListener>> iterator = actionListeners.iterator();

        while (iterator.hasNext()) {
            WeakReference<ActionListener> reference = iterator.next();

            ActionListener actionListener = reference.get();

            if (actionListener != null) {

                // return the listener as the source because this is a utility class
                ActionEvent e = new ActionEvent(actionListener, ActionEvent.ACTION_PERFORMED, "AccountBalanceChanged");
                actionListener.actionPerformed(e);
            } else {
                iterator.remove();
            }
        }
    }
}
