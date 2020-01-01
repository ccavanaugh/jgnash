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

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

import jgnash.report.pdf.Report;
import jgnash.report.table.AbstractReportTableModel;
import jgnash.resource.util.ResourceUtils;
import jgnash.time.DateUtils;
import jgnash.time.Period;
import jgnash.uifx.Options;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.report.pdf.ReportController;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Balance Sheet Report Controller
 *
 * @author Craig Cavanaugh
 */
public class BalanceSheetReportController implements ReportController {

    @FXML
    private ComboBox<Period> resolutionComboBox;

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    private CheckBox hideZeroBalanceAccounts;

    private static final String HIDE_ZERO_BALANCE = "hideZeroBalance";

    private static final String PERIOD = "period";

    private static final String MONTHS = "months";

    private final BalanceSheetReport report = new BalanceSheetReport();

    private Runnable refreshRunnable = null;

    private final Preferences preferences = getPreferences();

    public BalanceSheetReportController() {
        super();
    }

    @FXML
    private void initialize() {
        final Preferences preferences = getPreferences();

        hideZeroBalanceAccounts.setSelected(preferences.getBoolean(HIDE_ZERO_BALANCE, true));

        resolutionComboBox.getItems().setAll(Period.MONTHLY, Period.QUARTERLY, Period.YEARLY);
        resolutionComboBox.setValue(Period.values()[preferences.getInt(PERIOD, Period.MONTHLY.ordinal())]);

        resetDates();

        startDatePicker.preserveDateProperty().bind(Options.restoreReportDateProperty());
        startDatePicker.preferencesProperty().setValue(preferences);
        startDatePicker.preferenceKeyProperty().setValue(START_DATE_KEY);

        endDatePicker.preserveDateProperty().bind(Options.restoreReportDateProperty());
        endDatePicker.preferencesProperty().setValue(preferences);
        endDatePicker.preferenceKeyProperty().setValue(END_DATE_KEY);

        startDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> handleReportRefresh());
        endDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> handleReportRefresh());
        hideZeroBalanceAccounts.onActionProperty().setValue(event -> handleReportRefresh());
        resolutionComboBox.valueProperty().addListener((observable, oldValue, newValue) -> handleReportRefresh());

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
        final AbstractReportTableModel model = createReportModel();

        report.clearReport();
        report.setTitle(ResourceUtils.getString("Title.BalanceSheet"));

        try {
            report.addTable(model);
            report.addFooter();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public AbstractReportTableModel createReportModel() {
        report.setReportPeriod(resolutionComboBox.getValue());

        return report.createReportModel(startDatePicker.getValue(), endDatePicker.getValue(),
                hideZeroBalanceAccounts.isSelected());
    }

    @FXML
    private void handleResetAll() {
        resetDates();
    }
}
