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
package jgnash.uifx;

import java.util.prefs.Preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

/**
 * Manages application preferences
 *
 * @author Craig Cavanaugh
 */
public class Options {

    private static final Preferences p = Preferences.userNodeForPackage(Options.class);

    private static final String CONFIRM_DELETE_REMINDER = "confirmDeleteReminder";

    private static final String CONFIRM_DELETE_TRANSACTION = "confirmDeleteTransaction";

    private static final String ACCOUNTING_TERMS = "useAccountingTerms";

    private static final String REMEMBER_DATE = "rememberDate";

    private static final String AUTO_COMPLETE = "autoCompleteEnabled";

    private static final String IGNORE_CASE = "autoCompleteIgnoreCaseEnabled";

    private static final String FUZZY_MATCH = "autoCompleteFuzzyMatchEnabled";

    private static final SimpleBooleanProperty useAccountingTerms;

    private static final SimpleBooleanProperty confirmDeleteTransaction;

    private static final SimpleBooleanProperty confirmDeleteReminder;

    private static final SimpleBooleanProperty rememberDate;

    private static final SimpleBooleanProperty autoCompleteEnabled;

    private static final SimpleBooleanProperty autoCompleteIgnoreCaseEnabled;

    private static final SimpleBooleanProperty autoCompleteFuzzyMatchEnabled;

    private static final ChangeListener<Boolean> booleanChangeListener;

    static {
        booleanChangeListener = (observable, oldValue, newValue) ->
                p.putBoolean(((SimpleBooleanProperty)observable).getName(), newValue);

        useAccountingTerms = createBooleanProperty(ACCOUNTING_TERMS, false);
        confirmDeleteTransaction = createBooleanProperty(CONFIRM_DELETE_TRANSACTION, true);
        confirmDeleteReminder = createBooleanProperty(CONFIRM_DELETE_REMINDER, true);
        rememberDate = createBooleanProperty(REMEMBER_DATE, true);
        autoCompleteEnabled = createBooleanProperty(AUTO_COMPLETE, true);
        autoCompleteIgnoreCaseEnabled = createBooleanProperty(IGNORE_CASE, false);
        autoCompleteFuzzyMatchEnabled = createBooleanProperty(FUZZY_MATCH, false);
    }

    private Options() {
        // Utility class
    }

    private static SimpleBooleanProperty createBooleanProperty(final String name, final boolean defaultValue) {
        final SimpleBooleanProperty property = new SimpleBooleanProperty(null, name, p.getBoolean(name, defaultValue));
        property.addListener(booleanChangeListener);

        return property;
    }

    /**
     * Returns transaction deletion confirmation
     *
     * @return true if confirm on transaction delete is enabled, false otherwise
     */
    public static BooleanProperty getConfirmTransactionDeleteEnabled() {
        return confirmDeleteTransaction;
    }

    /**
     * Returns reminder deletion confirmation
     *
     * @return true if confirm on transaction delete is enabled, false otherwise
     */
    public static BooleanProperty getConfirmDeleteReminderEnabled() {
        return confirmDeleteReminder;
    }

    public static BooleanProperty getAccountingTermsEnabled() {
        return useAccountingTerms;
    }

    /**
     * Determines if the last date used for a transaction is reset
     * to the current date or remembered.
     *
     * @return true if the last date should be reused
     */
    public static BooleanProperty getRememberLastDate() {
        return rememberDate;
    }

    /**
     * Provides access to the enabled state of auto completion
     *
     * @return {@code BooleanProperty} controlling enabled state
     */
    public static BooleanProperty getAutoCompleteEnabled() {
        return autoCompleteEnabled;
    }

    /**
     * Provides access to the case sensitivity of auto completion
     *
     * @return {@code BooleanProperty} controlling case sensitivity
     */
    public static BooleanProperty getAutoCompleteIgnoreCaseEnabled() {
        return autoCompleteIgnoreCaseEnabled;
    }

    /** Provides access to the property controlling if fuzzy match is used for auto completion
     *
     * @return {@code BooleanProperty} controlling fuzzy match
     */
    public static BooleanProperty getAutoCompleteFuzzyMatchEnabled() {
        return autoCompleteFuzzyMatchEnabled;
    }
}
