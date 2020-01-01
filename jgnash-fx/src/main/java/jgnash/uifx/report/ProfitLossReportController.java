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
package jgnash.uifx.report;

import java.io.IOException;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

import jgnash.report.pdf.Report;
import jgnash.report.table.AbstractReportTableModel;
import jgnash.report.table.SortOrder;
import jgnash.resource.util.ResourceUtils;
import jgnash.time.DateUtils;
import jgnash.time.Period;
import jgnash.uifx.Options;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.report.pdf.ReportController;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Profit Loss Report Controller
 *
 * @author Craig Cavanaugh
 */
public class ProfitLossReportController implements ReportController {

    @FXML
    private ComboBox<Period> resolutionComboBox;

    @FXML
    private ComboBox<SortOrder> sortOrderComboBox;

    @FXML
    private CheckBox showLongNamesCheckBox;

    @FXML
    private CheckBox showAccountPercentages;

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    private CheckBox hideZeroBalanceAccounts;

    private static final String HIDE_ZERO_BALANCE = "hideZeroBalance";

    private static final String PERIOD = "period";

    private static final String MONTHS = "months";

    private static final String SHOW_FULL_ACCOUNT_PATH = "showFullAccountPath";

    private static final String SHOW_PERCENTAGES = "showPercentages";

    private static final String SORT_ORDER = "sortOrder";

    private final ProfitLossReport report = new ProfitLossReport();

    private Runnable refreshRunnable = null;

    private final Preferences preferences = getPreferences();

    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<Object> changeListener;  // need to hold a reference to prevent premature collection

    public ProfitLossReportController() {
        super();
    }

    @FXML
    private void initialize() {
        final Preferences preferences = getPreferences();

        hideZeroBalanceAccounts.setSelected(preferences.getBoolean(HIDE_ZERO_BALANCE, true));

        showLongNamesCheckBox.setSelected(preferences.getBoolean(SHOW_FULL_ACCOUNT_PATH, false));
        showAccountPercentages.setSelected(preferences.getBoolean(SHOW_PERCENTAGES, false));

        resolutionComboBox.getItems().setAll(Period.MONTHLY, Period.QUARTERLY, Period.YEARLY);
        resolutionComboBox.setValue(Period.values()[preferences.getInt(PERIOD, Period.QUARTERLY.ordinal())]);

        resetDates();

        startDatePicker.preserveDateProperty().bind(Options.restoreReportDateProperty());
        startDatePicker.preferencesProperty().setValue(preferences);
        startDatePicker.preferenceKeyProperty().setValue(START_DATE_KEY);

        endDatePicker.preserveDateProperty().bind(Options.restoreReportDateProperty());
        endDatePicker.preferencesProperty().setValue(preferences);
        endDatePicker.preferenceKeyProperty().setValue(END_DATE_KEY);

        sortOrderComboBox.getItems().setAll(SortOrder.values());
        sortOrderComboBox.setValue(SortOrder.values()[preferences.getInt(SORT_ORDER, SortOrder.BY_NAME.ordinal())]);

        // change listener is assigned after controls have been set to prevent multiple report refreshes
        changeListener = (observable, oldValue, newValue) -> handleReportRefresh();

        startDatePicker.valueProperty().addListener(new WeakChangeListener<>(changeListener));
        endDatePicker.valueProperty().addListener(new WeakChangeListener<>(changeListener));
        resolutionComboBox.valueProperty().addListener(new WeakChangeListener<>(changeListener));
        hideZeroBalanceAccounts.selectedProperty().addListener(new WeakChangeListener<>(changeListener));
        showLongNamesCheckBox.selectedProperty().addListener(new WeakChangeListener<>(changeListener));
        showAccountPercentages.selectedProperty().addListener(new WeakChangeListener<>(changeListener));

        // boot the report generation
        JavaFXUtils.runLater(this::refreshReport);
    }

    @Override
    public void setRefreshRunnable(final Runnable runnable) {
        refreshRunnable = runnable;
    }

    @Override
    public void getReport(final Consumer<Report> reportConsumer) {
        reportConsumer.accept(report);
    }

    @Override
    public void refreshReport() {
        handleReportRefresh();
    }

    private void handleReportRefresh() {

        preferences.putBoolean(HIDE_ZERO_BALANCE, hideZeroBalanceAccounts.isSelected());
        preferences.putInt(MONTHS, DateUtils.getLastDayOfTheMonths(startDatePicker.getValue(),
                endDatePicker.getValue()).size());
        preferences.putInt(PERIOD, resolutionComboBox.getValue().ordinal());
        preferences.putInt(SORT_ORDER, sortOrderComboBox.getValue().ordinal());
        preferences.putBoolean(SHOW_FULL_ACCOUNT_PATH, showLongNamesCheckBox.isSelected());
        preferences.getBoolean(SHOW_PERCENTAGES, showAccountPercentages.isSelected());

        addTable();

        // send notification the report has been updated
        if (refreshRunnable != null) {
            refreshRunnable.run();
        }
    }

    private void resetDates() {

        // calculate number of months for 4-5 columns
        final int months = (int)(resolutionComboBox.getValue().getMonths() * 4);

        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(DateUtils.getFirstDayOfTheMonth(LocalDate.now().minusMonths(months - 1)));
    }

    private void addTable() {
        AbstractReportTableModel model = createReportModel();

        report.clearReport();
        report.setTitle(ResourceUtils.getString("Title.ProfitLoss"));


        try {
            report.addTable(model);
            report.addFooter();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public AbstractReportTableModel createReportModel() {
        report.setAddPercentileColumn(showAccountPercentages.isSelected());

        report.setSortOrder(sortOrderComboBox.getValue());
        report.setReportPeriod(resolutionComboBox.getValue());

        report.setShowFullAccountPath(showLongNamesCheckBox.isSelected());

        return report.createReportModel(startDatePicker.getValue(), endDatePicker.getValue(),
                hideZeroBalanceAccounts.isSelected());
    }

    @FXML
    public void handleRefresh() {
        JavaFXUtils.runLater(this::refreshReport);
    }

    @FXML
    private void handleResetAll() {
        resetDates();
    }
}
