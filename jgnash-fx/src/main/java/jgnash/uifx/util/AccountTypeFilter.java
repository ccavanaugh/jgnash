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
package jgnash.uifx.util;

import java.util.prefs.Preferences;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import jgnash.engine.Account;
import jgnash.engine.AccountType;

/**
 * Account Type Filter class.
 *
 * @author Craig Cavanaugh
 */
public class AccountTypeFilter {

    private static final String HIDDEN_VISIBLE = "HiddenVisible";
    private static final String EXPENSE_VISIBLE = "ExpenseVisible";
    private static final String INCOME_VISIBLE = "IncomeVisible";
    private static final String ACCOUNT_VISIBLE = "AccountVisible";

    private final BooleanProperty accountTypesVisible = new SimpleBooleanProperty(true);
    private final BooleanProperty expenseTypesVisible = new SimpleBooleanProperty(true);
    private final BooleanProperty incomeTypesVisible = new SimpleBooleanProperty(true);
    private final BooleanProperty hiddenTypesVisible = new SimpleBooleanProperty(true);

    private final Preferences preferences;

    public AccountTypeFilter(final Preferences preferences) {
        this.preferences = preferences;

        accountTypesVisible.set(preferences.getBoolean(ACCOUNT_VISIBLE, true));
        expenseTypesVisible.set(preferences.getBoolean(EXPENSE_VISIBLE, true));
        incomeTypesVisible.set(preferences.getBoolean(INCOME_VISIBLE, true));
        hiddenTypesVisible.set(preferences.getBoolean(HIDDEN_VISIBLE, true));

        // Add change listeners to write preferences
        accountTypesVisible.addListener((observable, oldValue, newValue) -> setFilter(observable.getValue(), ACCOUNT_VISIBLE));
        incomeTypesVisible.addListener((observable, oldValue, newValue) -> setFilter(observable.getValue(), INCOME_VISIBLE));
        expenseTypesVisible.addListener((observable, oldValue, newValue) -> setFilter(observable.getValue(), EXPENSE_VISIBLE));
        hiddenTypesVisible.addListener((observable, oldValue, newValue) -> setFilter(observable.getValue(), HIDDEN_VISIBLE));
    }

    public BooleanProperty getAccountTypesVisibleProperty() {
        return accountTypesVisible;
    }

    public BooleanProperty getIncomeTypesVisibleProperty() {
        return incomeTypesVisible;
    }

    public BooleanProperty getExpenseTypesVisibleProperty() {
        return expenseTypesVisible;
    }

    public BooleanProperty getHiddenTypesVisibleProperty() {
        return hiddenTypesVisible;
    }

    private void setFilter(final boolean visible, final String propertyKey) {
        preferences.putBoolean(propertyKey, visible);
    }

    /**
     * Determines if an account is visible.
     *
     * @param a account to check for visibility
     * @return true is account should be displayed
     */
    public boolean isAccountVisible(final Account a) {
        final AccountType type = a.getAccountType();

        if (type == AccountType.INCOME && incomeTypesVisible.get()) {
            return a.isVisible() || hiddenTypesVisible.get();
        } else if (type == AccountType.EXPENSE && expenseTypesVisible.get()) {
            return a.isVisible() || hiddenTypesVisible.get();
        } else if (type != AccountType.INCOME && type != AccountType.EXPENSE && accountTypesVisible.get()) {
            return a.isVisible() || hiddenTypesVisible.get();
        }
        return false;
    }

    /**
     * Adds a single {@code InvalidationListener} to all visibility properties.
     *
     * @param listener {@code InvalidationListener} to add
     */
    public void addListener(final InvalidationListener listener) {
        getAccountTypesVisibleProperty().addListener(listener);
        getExpenseTypesVisibleProperty().addListener(listener);
        getIncomeTypesVisibleProperty().addListener(listener);
        getHiddenTypesVisibleProperty().addListener(listener);
    }
}
