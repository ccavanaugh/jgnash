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
    private static final String CONFIRM_ON_DELETE = "confirmDelete";

    private static final String ACCOUNTING_TERMS = "accountingTerms";

    private static final String REMEMBER_DATE = "rememberDate";

    private static final String AUTO_COMPLETE = "autoCompleteEnabled";

    private static final String IGNORE_CASE = "autoCompleteIgnoreCaseEnabled";

    private static final String FUZZY_MATCH = "autoCompleteFuzzyMatchEnabled";

    private static SimpleBooleanProperty useAccountingTerms = new SimpleBooleanProperty(null, ACCOUNTING_TERMS, false);

    private static SimpleBooleanProperty confirmTransactionDelete = new SimpleBooleanProperty(null, CONFIRM_ON_DELETE, true);

    private static SimpleBooleanProperty rememberDate = new SimpleBooleanProperty(null, REMEMBER_DATE, true);

    private static SimpleBooleanProperty autoCompleteEnabled = new SimpleBooleanProperty(null, AUTO_COMPLETE, true);

    private static SimpleBooleanProperty autoCompleteIgnoreCaseEnabled = new SimpleBooleanProperty(null, IGNORE_CASE, false);

    private static SimpleBooleanProperty autoCompleteFuzzyMatchEnabled = new SimpleBooleanProperty(null, FUZZY_MATCH, false);

    static {
        final Preferences p = Preferences.userNodeForPackage(Options.class);

        final ChangeListener<Boolean> booleanChangeListener = (observable, oldValue, newValue) ->
                p.putBoolean(((SimpleBooleanProperty)observable).getName(), newValue);

        useAccountingTerms.set(p.getBoolean(ACCOUNTING_TERMS, false));
        useAccountingTerms.addListener(booleanChangeListener);

        confirmTransactionDelete.set(p.getBoolean(CONFIRM_ON_DELETE, true));
        confirmTransactionDelete.addListener(booleanChangeListener);

        rememberDate.set(p.getBoolean(REMEMBER_DATE, true));
        rememberDate.addListener(booleanChangeListener);

        autoCompleteEnabled.set(p.getBoolean(AUTO_COMPLETE, true));
        autoCompleteEnabled.addListener(booleanChangeListener);

        autoCompleteFuzzyMatchEnabled.set(p.getBoolean(FUZZY_MATCH, false));
        autoCompleteFuzzyMatchEnabled.addListener(booleanChangeListener);

        autoCompleteIgnoreCaseEnabled.set(p.getBoolean(IGNORE_CASE, true));
        autoCompleteIgnoreCaseEnabled.addListener(booleanChangeListener);
    }

    /**
     * Returns the availability of sortable registers
     *
     * @return true if confirm on transaction delete is enabled, false otherwise
     */
    public static BooleanProperty getConfirmTransactionDeleteEnabled() {
        return confirmTransactionDelete;
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
