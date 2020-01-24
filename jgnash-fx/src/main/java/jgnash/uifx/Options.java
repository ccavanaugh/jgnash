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
package jgnash.uifx;

import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.ButtonBar;

import jgnash.text.NumericFormats;
import jgnash.time.DateUtils;
import jgnash.uifx.control.TimePeriodComboBox;

/**
 * Manages application preferences.
 *
 * @author Craig Cavanaugh
 */
@SuppressWarnings("SameParameterValue")
public class Options {

    private static final Preferences p = Preferences.userNodeForPackage(Options.class);

    private static final String AUTO_PACK_TABLE = "autoPackTables";

    private static final String CONFIRM_DELETE_REMINDER = "confirmDeleteReminder";

    private static final String CONFIRM_DELETE_TRANSACTION = "confirmDeleteTransaction";

    private static final String ACCOUNTING_TERMS = "useAccountingTerms";

    private static final String REMEMBER_DATE = "rememberDate";

    private static final String AUTO_COMPLETE = "autoCompleteEnabled";

    private static final String CASE_SENSITIVE = "autoCompleteIsCaseEnabled";

    private static final String CHECK_UPDATES = "checkForUpdates";

    private static final String CONCATENATE_MEMOS = "concatenateMemos";

    private static final String FUZZY_MATCH = "autoCompleteFuzzyMatchEnabled";

    private static final String REMINDER_SNOOZE = "reminderSnoozePeriod";

    private static final String OPEN_LAST = "openLastEnabled";

    private static final String SELECT_ON_FOCUS = "selectOnFocus";

    private static final String BUTTON_ORDER = "buttonOrder";

    private static final String ANIMATIONS_ENABLED = "animationsEnabled";

    private static final String RESTORE_LAST_TAB = "restoreLastTab";

    private static final String REGEX_FOR_FILTERS = "regexForFilters";

    private static final String GLOBAL_BAYES_ENABLED = "globalBayesEnabled";

    private static final String LAST_FORMAT_CHANGE = "lastFormatChange";

    private static final String RESTORE_REPORT_DATES = "restoreReportDates";

    private static final int DEFAULT_SNOOZE = TimePeriodComboBox.getPeriods()[0];

    private static final SimpleBooleanProperty useAccountingTerms;

    private static final SimpleBooleanProperty checkForUpdates;

    private static final SimpleBooleanProperty confirmDeleteTransaction;

    private static final SimpleBooleanProperty confirmDeleteReminder;

    private static final SimpleBooleanProperty rememberDate;

    private static final SimpleBooleanProperty autoPackTablesEnabled;

    private static final SimpleBooleanProperty autoCompleteEnabled;

    private static final SimpleBooleanProperty autoCompleteCaseSensitiveEnabled;

    private static final SimpleBooleanProperty autoCompleteFuzzyMatchEnabled;

    private static final SimpleBooleanProperty selectOnFocusEnabled;

    private static final SimpleBooleanProperty openLastEnabled;

    private static final SimpleBooleanProperty animationsEnabled;

    private static final SimpleBooleanProperty restoreLastRegisterTab;

    private static final SimpleBooleanProperty concatenateMemos;

    private static final SimpleBooleanProperty regexForFilters;

    private static final SimpleBooleanProperty globalBayesEnabled;

    private static final SimpleBooleanProperty restoreReportDates;

    private static final SimpleIntegerProperty reminderSnoozePeriod;

    private static final SimpleStringProperty buttonOrder;

    private static final SimpleStringProperty fullNumericFormat;

    private static final SimpleStringProperty shortNumericFormat;

    private static final SimpleStringProperty shortDateFormat;

    private static final ChangeListener<Boolean> booleanChangeListener;

    private static final ChangeListener<Number> integerChangeListener;

    static {
        booleanChangeListener = (observable, oldValue, newValue) ->
                p.putBoolean(((SimpleBooleanProperty)observable).getName(), newValue);

        integerChangeListener = (observable, oldValue, newValue) ->
                p.putInt(((SimpleIntegerProperty) observable).getName(), (Integer) newValue);

        useAccountingTerms = createBooleanProperty(ACCOUNTING_TERMS, false);
        checkForUpdates = createBooleanProperty(CHECK_UPDATES, true);
        confirmDeleteTransaction = createBooleanProperty(CONFIRM_DELETE_TRANSACTION, true);
        confirmDeleteReminder = createBooleanProperty(CONFIRM_DELETE_REMINDER, true);
        rememberDate = createBooleanProperty(REMEMBER_DATE, true);
        autoCompleteEnabled = createBooleanProperty(AUTO_COMPLETE, true);
        autoCompleteCaseSensitiveEnabled = createBooleanProperty(CASE_SENSITIVE, false);
        autoCompleteFuzzyMatchEnabled = createBooleanProperty(FUZZY_MATCH, false);
        autoPackTablesEnabled = createBooleanProperty(AUTO_PACK_TABLE, true);
        openLastEnabled = createBooleanProperty(OPEN_LAST, false);
        selectOnFocusEnabled = createBooleanProperty(SELECT_ON_FOCUS, false);
        concatenateMemos = createBooleanProperty(CONCATENATE_MEMOS, false);
        animationsEnabled = createBooleanProperty(ANIMATIONS_ENABLED, true);
        restoreLastRegisterTab = createBooleanProperty(RESTORE_LAST_TAB, true);
        regexForFilters = createBooleanProperty(REGEX_FOR_FILTERS, false);
        globalBayesEnabled = createBooleanProperty(GLOBAL_BAYES_ENABLED, false);
        restoreReportDates = createBooleanProperty(RESTORE_REPORT_DATES, false);

        reminderSnoozePeriod = createIntegerProperty(REMINDER_SNOOZE, DEFAULT_SNOOZE);

        /* Zero value caused by a prior bug */
        if (Options.reminderSnoozePeriodProperty().get() <= 0) {
            Options.reminderSnoozePeriodProperty().setValue(DEFAULT_SNOOZE);
        }

        shortNumericFormat = createStringProperty(NumericFormats.getShortFormatPattern(), pattern -> {
            NumericFormats.setShortFormatPattern(pattern);
            updateLastFormatChange();
        });

        fullNumericFormat = createStringProperty(NumericFormats.getFullFormatPattern(), pattern -> {
            NumericFormats.setFullFormatPattern(pattern);
            updateLastFormatChange();
        });

        shortDateFormat = createStringProperty(DateUtils.getShortDatePattern(), pattern -> {
            DateUtils.setShortDateFormatPattern(pattern);
            updateLastFormatChange();
        });

        buttonOrder = createStringProperty(new ButtonBar().getButtonOrder(), s -> p.put(BUTTON_ORDER, s));

        // Initialize with the current value if it's not been set before
        if (getLastFormatChange() == 0) {
            updateLastFormatChange();
        }
    }

    private Options() {
        // Utility class
    }

    private static SimpleBooleanProperty createBooleanProperty(final String name, final boolean defaultValue) {
        final SimpleBooleanProperty property = new SimpleBooleanProperty(null, name, p.getBoolean(name, defaultValue));
        property.addListener(booleanChangeListener);

        return property;
    }

    private static SimpleIntegerProperty createIntegerProperty(final String name, final int defaultValue) {
        final SimpleIntegerProperty property = new SimpleIntegerProperty(null, name, p.getInt(name, defaultValue));
        property.addListener(integerChangeListener);

        return property;
    }

    private static SimpleStringProperty createStringProperty(final String defaultValue, final Consumer<String> stringConsumer) {
        final SimpleStringProperty property = new SimpleStringProperty(defaultValue);
        property.addListener((observable, oldValue, newValue) -> stringConsumer.accept(newValue));
        return property;
    }

    public static StringProperty fullNumericFormatProperty() {
        return fullNumericFormat;
    }

    public static StringProperty shortNumericFormatProperty() {
        return shortNumericFormat;
    }

    public static StringProperty shortDateFormatProperty() {
        return shortDateFormat;
    }

    public static long getLastFormatChange() {
        return p.getLong(LAST_FORMAT_CHANGE, 0);   // default should trigger an update
    }

    private static void updateLastFormatChange() {
        p.putLong(LAST_FORMAT_CHANGE, System.currentTimeMillis());
    }

    /**
     * Returns transaction deletion confirmation.
     *
     * @return true if confirm on transaction delete is enabled, false otherwise
     */
    public static BooleanProperty confirmOnTransactionDeleteProperty() {
        return confirmDeleteTransaction;
    }

    public static BooleanProperty checkForUpdatesProperty() {
        return checkForUpdates;
    }

    /**
     * Returns reminder deletion confirmation.
     *
     * @return true if confirm on reminder delete is enabled, false otherwise
     */
    public static BooleanProperty confirmOnDeleteReminderProperty() {
        return confirmDeleteReminder;
    }

    public static BooleanProperty concatenateMemosProperty() {
        return concatenateMemos;
    }

    public static BooleanProperty useAccountingTermsProperty() {
        return useAccountingTerms;
    }

    public static BooleanProperty globalBayesProperty() {
        return globalBayesEnabled;
    }

    /**
     * Determines if the last date used for a transaction is reset
     * to the current date or remembered.
     *
     * @return true if the last date should be reused
     */
    public static BooleanProperty rememberLastDateProperty() {
        return rememberDate;
    }

    /**
     * Determines if the last date used for a report is restored or ignored.
     *
     * @return true if the last date should be used
     */
    public static BooleanProperty restoreReportDateProperty() {
        return restoreReportDates;
    }

    /**
     * Provides access to the enabled state of auto completion.
     *
     * @return {@code BooleanProperty} controlling enabled state
     */
    public static BooleanProperty useAutoCompleteProperty() {
        return autoCompleteEnabled;
    }

    /**
     * Provides access to the case sensitivity of auto completion.
     *
     * @return {@code BooleanProperty} controlling case sensitivity
     */
    public static BooleanProperty autoCompleteIsCaseSensitiveProperty() {
        return autoCompleteCaseSensitiveEnabled;
    }

    /**
     * Provides access to the case sensitivity of auto completion.
     *
     * @return {@code BooleanProperty} controlling case sensitivity
     */
    public static BooleanProperty autoPackTablesProperty() {
        return autoPackTablesEnabled;
    }

    /** Provides access to the property controlling if fuzzy match is used for auto completion.
     *
     * @return {@code BooleanProperty} controlling fuzzy match
     */
    public static BooleanProperty useFuzzyMatchForAutoCompleteProperty() {
        return autoCompleteFuzzyMatchEnabled;
    }

    /**
     * Provides access to the property controlling the snooze time between reminder events.
     *
     * @return {@code IntegerProperty} controlling snooze time. Period is in milliseconds
     */
    public static IntegerProperty reminderSnoozePeriodProperty() {
        return reminderSnoozePeriod;
    }

    /** Provides access to the property controlling if a text field automatically selects all text when
     * it receives the focus.
     *
     * @return {@code BooleanProperty} controlling fuzzy match
     */
    public static BooleanProperty selectOnFocusProperty() {
        return selectOnFocusEnabled;
    }

    /**
     * Provides access to the open last file at startup property.
     *
     * @return {@code BooleanProperty} controlling open last file
     */
    public static BooleanProperty openLastProperty() {
        return openLastEnabled;
    }

    /**
     * Provides access to the enabled state for animations.
     *
     * @return {@code BooleanProperty} controlling open last file
     */
    public static BooleanProperty animationsEnabledProperty() {
        return animationsEnabled;
    }

    /**
     * Provides access to the restore last used tab property.
     *
     * @return {@code BooleanProperty} controlling open last file
     */
    public static BooleanProperty restoreLastTabProperty() {
        return restoreLastRegisterTab;
    }

    /**
     * Provides access to the property controlling use of regular expressions when applying filters.
     *
     * @return {@code BooleanProperty} controlling use of regex for filters/{@code Predicate}
     */
    public static BooleanProperty regexForFiltersProperty() {
        return regexForFilters;
    }

    public static StringProperty buttonOrderProperty() {
        return buttonOrder;
    }
}
