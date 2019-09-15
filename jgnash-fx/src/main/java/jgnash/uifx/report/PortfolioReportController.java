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

import java.io.IOException;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

import jgnash.engine.AccountType;
import jgnash.engine.EngineFactory;
import jgnash.engine.InvestmentPerformanceSummary;
import jgnash.report.pdf.Report;
import jgnash.report.table.AbstractReportTableModel;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.report.pdf.ReportController;
import jgnash.uifx.util.JavaFXUtils;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Portfolio report controller.
 *
 * @author Craig Cavanaugh
 */
public class PortfolioReportController implements ReportController {

    private static final String RECURSIVE = "recursive";

    private static final String VERBOSE = "verbose";

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    private AccountComboBox accountComboBox;

    @FXML
    private CheckBox subAccountCheckBox;

    @FXML
    private CheckBox longNameCheckBox;

    @FXML
    private ResourceBundle resources;

    private Runnable refreshRunnable = null;

    private final Report report = new PortfolioReport();

    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<Object> changeListener;

    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<Object> accountChangeListener;

    @FXML
    private void initialize() {
        // Only show visible investment accounts
        accountComboBox.setPredicate(account -> account.instanceOf(AccountType.INVEST) && account.isVisible());

        final Preferences preferences = getPreferences();

        subAccountCheckBox.setSelected(preferences.getBoolean(RECURSIVE, true));
        longNameCheckBox.setSelected(preferences.getBoolean(VERBOSE, false));

        // set date range
        updateDateRanges();

        changeListener = (observable, oldValue, newValue) -> {
            if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {   // could be null if GC is slow
                handleReportRefresh();
            }
        };

        // update data ranges prior to refresh
        accountChangeListener = (observable, oldValue, newValue) -> {
            updateDateRanges();
            changeListener.changed(observable, oldValue, newValue);
        };

        subAccountCheckBox.selectedProperty().addListener(new WeakChangeListener<>(changeListener));
        longNameCheckBox.selectedProperty().addListener(new WeakChangeListener<>(changeListener));
        accountComboBox.valueProperty().addListener(new WeakChangeListener<>(accountChangeListener));
        startDatePicker.valueProperty().addListener(new WeakChangeListener<>(changeListener));
        endDatePicker.valueProperty().addListener(new WeakChangeListener<>(changeListener));


        // boot the report generation
        JavaFXUtils.runLater(this::refreshReport);
    }

    private void updateDateRanges() {
        if (accountComboBox.getValue() != null) {

            final Pair<LocalDate, LocalDate> dateRange
                    = InvestmentPerformanceSummary.getTransactionDateRange(accountComboBox.getValue(),
                    subAccountCheckBox.isSelected());

            startDatePicker.setValue(dateRange.getLeft());
            endDatePicker.setValue(dateRange.getRight());
        }
    }

    @Override
    public void setRefreshRunnable(final Runnable runnable) {
        refreshRunnable = runnable;
    }


    @Override
    public void getReport(Consumer<Report> reportConsumer) {
        reportConsumer.accept(report);
    }

    @Override
    public void refreshReport() {
        handleReportRefresh();
    }

    private void handleReportRefresh() {

        final Preferences preferences = getPreferences();

        preferences.putBoolean(RECURSIVE, subAccountCheckBox.isSelected());
        preferences.putBoolean(VERBOSE, longNameCheckBox.isSelected());

        // make sure an account is available and the value is not null due to slow GC
        if (!accountComboBox.isDisabled() && accountComboBox.getValue() != null) {
            addTable();

            // send notification the report has been updated
            if (refreshRunnable != null) {
                refreshRunnable.run();
            }
        }
    }

    private void addTable() {
        AbstractReportTableModel model = createReportModel();

        report.clearReport();

        try {
            report.addTable(model);
            report.addFooter();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public AbstractReportTableModel createReportModel() {
        return PortfolioReport.createReportModel(accountComboBox.getValue(), startDatePicker.getValue(),
                endDatePicker.getValue(), subAccountCheckBox.isSelected(), longNameCheckBox.isSelected());
    }
}
