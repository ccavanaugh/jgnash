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
package jgnash.uifx.controllers;

import java.util.prefs.Preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Account Type Filter interface
 *
 * @author Craig Cavanaugh
 */
public interface AccountTypeFilter {

    static final String HIDDEN_VISIBLE = "HiddenVisible";
    static final String EXPENSE_VISIBLE = "ExpenseVisible";
    static final String INCOME_VISIBLE = "IncomeVisible";
    static final String ACCOUNT_VISIBLE = "AccountVisible";

    BooleanProperty accountTypesVisible = new SimpleBooleanProperty();
    BooleanProperty expenseTypesVisible = new SimpleBooleanProperty();
    BooleanProperty incomeTypesVisible = new SimpleBooleanProperty();
    BooleanProperty hiddenTypesVisible = new SimpleBooleanProperty();

    Preferences getPreferences();

    default void initializeFilterPreferences() {
        accountTypesVisible.set(getPreferences().getBoolean(ACCOUNT_VISIBLE, true));
        expenseTypesVisible.set(getPreferences().getBoolean(EXPENSE_VISIBLE, true));
        incomeTypesVisible.set(getPreferences().getBoolean(INCOME_VISIBLE, true));
        hiddenTypesVisible.set(getPreferences().getBoolean(HIDDEN_VISIBLE, true));

        // Add change listeners to write preferences
        accountTypesVisible.addListener((observable, oldValue, newValue) -> setFilter(observable.getValue(), ACCOUNT_VISIBLE));
        incomeTypesVisible.addListener((observable, oldValue, newValue) -> setFilter(observable.getValue(), INCOME_VISIBLE));
        expenseTypesVisible.addListener((observable, oldValue, newValue) -> setFilter(observable.getValue(), EXPENSE_VISIBLE));
        hiddenTypesVisible.addListener((observable, oldValue, newValue) -> setFilter(observable.getValue(), HIDDEN_VISIBLE));
    }

    default BooleanProperty getAccountTypesVisibleProperty() {
        return accountTypesVisible;
    }

    default BooleanProperty getIncomeTypesVisibleProperty() {
        return incomeTypesVisible;
    }

    default BooleanProperty getExpenseTypesVisibleProperty() {
        return expenseTypesVisible;
    }

    default BooleanProperty getHiddenTypesVisibleProperty() {
        return hiddenTypesVisible;
    }

    default boolean getAccountTypesVisible() {
        return accountTypesVisible.get();
    }

    default boolean getExpenseTypesVisible() {
        return expenseTypesVisible.get();
    }

    default boolean getHiddenTypesVisible() {
        return hiddenTypesVisible.get();
    }

    default boolean getIncomeTypesVisible() {
        return incomeTypesVisible.get();
    }

    default void setFilter(final boolean visible, final String propertyKey) {
        getPreferences().putBoolean(propertyKey, visible);
    }
}
