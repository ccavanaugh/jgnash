/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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

import jgnash.report.pdf.Report;
import jgnash.report.table.AbstractReportTableModel;
import jgnash.resource.util.ResourceUtils;
import jgnash.time.DateUtils;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.report.pdf.ReportController;
import jgnash.uifx.util.JavaFXUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

/**
 * Net Worth Report Controller
 *
 * @author Craig Cavanaugh
 */
public class NetWorthReportController implements ReportController {

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    private CheckBox hideZeroBalanceAccounts;

    private static final String HIDE_ZERO_BALANCE = "hideZeroBalance";

    private static final String MONTHS = "months";

    private NetWorthReport report = new NetWorthReport();

    private Runnable refreshRunnable = null;

    public NetWorthReportController() {
        super();
    }

    @FXML
    private void initialize() {
        final Preferences preferences = getPreferences();

        hideZeroBalanceAccounts.setSelected(preferences.getBoolean(HIDE_ZERO_BALANCE, true));

        startDatePicker.setValue(LocalDate.now().minusMonths(preferences.getInt(MONTHS, 4) - 1));

        startDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> handleReportRefresh());

        endDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> handleReportRefresh());

        hideZeroBalanceAccounts.onActionProperty().setValue(event -> handleReportRefresh());

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

    @Override
    public void closeReport() throws IOException {
        report.close();
    }

    private void handleReportRefresh() {

        final Preferences preferences = getPreferences();

        preferences.putBoolean(HIDE_ZERO_BALANCE, hideZeroBalanceAccounts.isSelected());
        preferences.putInt(MONTHS, DateUtils.getLastDayOfTheMonths(startDatePicker.getValue(),
                endDatePicker.getValue()).size());

        addTable();

        // send notification the report has been updated
        if (refreshRunnable != null) {
            refreshRunnable.run();
        }
    }

    private void addTable() {
        AbstractReportTableModel model = createReportModel();

        report.clearReport();

        try {
            report.addTable(model, ResourceUtils.getString("Word.NetWorth"));
            report.addFooter();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private AbstractReportTableModel createReportModel() {
        return report.createReportModel(startDatePicker.getValue(), endDatePicker.getValue(),
                hideZeroBalanceAccounts.isSelected());
    }
}
