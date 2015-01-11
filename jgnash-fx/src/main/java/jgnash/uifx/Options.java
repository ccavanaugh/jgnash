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

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

/**
 * Manages application preferences
 *
 * @author Craig Cavanaugh
 */
public class Options {
    private static final String CONFIRM_ON_DELETE = "confirmDelete";

    private static final String ACCOUNTING_TERMS = "accountingTerms";

    private static final String REMEMBER_DATE = "rememberDate";

    private static final String AUTO_COMPLETE = "autoCompleteEnabled";

    private static final String IGNORE_CASE = "autoCompleteIgnoreCaseEnabled";

    private static final String FUZZY_MATCH = "autoCompleteFuzzyMatchEnabled";

    private static boolean useAccountingTerms;

    private static boolean confirmTransactionDelete;

    private static boolean rememberDate;

    private static SimpleBooleanProperty autoCompleteEnabled = new SimpleBooleanProperty(null, AUTO_COMPLETE, true);

    private static SimpleBooleanProperty autoCompleteIgnoreCaseEnabled = new SimpleBooleanProperty(null, IGNORE_CASE, false);

    private static SimpleBooleanProperty autoCompleteFuzzyMatchEnabled = new SimpleBooleanProperty(null, FUZZY_MATCH, false);

    static {
        final Preferences p = Preferences.userNodeForPackage(Options.class);

        final ChangeListener<Boolean> booleanChangeListener = (observable, oldValue, newValue) ->
                p.putBoolean(((SimpleBooleanProperty)observable).getName(), newValue);

        useAccountingTerms = p.getBoolean(ACCOUNTING_TERMS, false);
        confirmTransactionDelete = p.getBoolean(CONFIRM_ON_DELETE, true);
        rememberDate = p.getBoolean(REMEMBER_DATE, true);

        autoCompleteEnabled.set(p.getBoolean(AUTO_COMPLETE, true));
        autoCompleteEnabled.addListener(booleanChangeListener);

        autoCompleteFuzzyMatchEnabled.set(p.getBoolean(FUZZY_MATCH, false));
        autoCompleteFuzzyMatchEnabled.addListener(booleanChangeListener);

        autoCompleteIgnoreCaseEnabled.set(p.getBoolean(IGNORE_CASE, true));
        autoCompleteIgnoreCaseEnabled.addListener(booleanChangeListener);
    }

    /**
     * Sets if confirm on transaction delete is enabled
     *
     * @param enabled true if deletion confirmation is required
     */
    public static void setConfirmTransactionDeleteEnabled(final boolean enabled) {
        confirmTransactionDelete = enabled;
        Preferences p = Preferences.userNodeForPackage(Options.class);
        p.putBoolean(CONFIRM_ON_DELETE, confirmTransactionDelete);
    }

    /**
     * Returns the availability of sortable registers
     *
     * @return true if confirm on transaction delete is enabled, false otherwise
     */
    public static boolean isConfirmTransactionDeleteEnabled() {
        return confirmTransactionDelete;
    }

    public static void setAccountingTermsEnabled(final boolean enabled) {
        useAccountingTerms = enabled;
        Preferences p = Preferences.userNodeForPackage(Options.class);
        p.putBoolean(ACCOUNTING_TERMS, useAccountingTerms);
    }

    public static boolean isAccountingTermsEnabled() {
        return useAccountingTerms;
    }

    public static void setRememberLastDate(final boolean reset) {
        rememberDate = reset;
        Preferences p = Preferences.userNodeForPackage(Options.class);
        p.putBoolean(REMEMBER_DATE, rememberDate);
    }

    /**
     * Determines if the last date used for a transaction is reset
     * to the current date or remembered.
     *
     * @return true if the last date should be reused
     */
    public static boolean getRememberLastDate() {
        return rememberDate;
    }

    /**
     * Provides access to the enabled state of auto completion
     *
     * @return {@code SimpleBooleanProperty} controlling enabled state
     */
    public static SimpleBooleanProperty getAutoCompleteEnabled() {
        return autoCompleteEnabled;
    }

    /**
     * Provides access to the case sensitivity of auto completion
     *
     * @return {@code SimpleBooleanProperty} controlling case sensitivity
     */
    public static SimpleBooleanProperty getAutoCompleteIgnoreCaseEnabled() {
        return autoCompleteIgnoreCaseEnabled;
    }

    /** Provides access to the property controlling if fuzzy match is used for auto completion
     *
     * @return {@code SimpleBooleanProperty} controlling fuzzy match
     */
    public static SimpleBooleanProperty getAutoCompleteFuzzyMatchEnabled() {
        return autoCompleteFuzzyMatchEnabled;
    }
}
