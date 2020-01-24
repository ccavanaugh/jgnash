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
import java.util.ArrayList;
import java.util.List;
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
import jgnash.uifx.Options;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.CheckComboBox;
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

    private static final String REPORT_COLUMNS = "reportColumns";

    @FXML
    private CheckComboBox<String> columnComboBox;

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

    private ChangeListener<Object> changeListener;

    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<Boolean> comboChangeListener;

    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<Object> accountChangeListener;

    @FXML
    private void initialize() {
        final Preferences preferences = getPreferences();

        initColumnCombo();

        comboChangeListener = (observable, oldValue, newValue) -> {
            final List<String> columns = columnComboBox.getCheckedItems();

            if (columns.size() > 0) {
                preferences.put(REPORT_COLUMNS, String.join(",", columns));
                System.out.println(String.join(",", columns));
            } else {
                preferences.put(REPORT_COLUMNS, "");
            }

            handleReportRefresh();
        };

        // list to changes and update preferences and the report
        columnComboBox.addListener(new WeakChangeListener<>(comboChangeListener));

        // Only show visible investment accounts
        accountComboBox.setPredicate(account -> account.instanceOf(AccountType.INVEST) && account.isVisible());

        subAccountCheckBox.setSelected(preferences.getBoolean(RECURSIVE, true));
        longNameCheckBox.setSelected(preferences.getBoolean(VERBOSE, false));

        // set date range
        updateDateRanges();

        startDatePicker.preserveDateProperty().bind(Options.restoreReportDateProperty());
        startDatePicker.preferencesProperty().setValue(preferences);
        startDatePicker.preferenceKeyProperty().setValue(START_DATE_KEY);

        endDatePicker.preserveDateProperty().bind(Options.restoreReportDateProperty());
        endDatePicker.preferencesProperty().setValue(preferences);
        endDatePicker.preferenceKeyProperty().setValue(END_DATE_KEY);

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

    private void initColumnCombo() {
        final List<String> columnNames = getAvailColumns();

        for (final String columnName : columnNames) {
            columnComboBox.add(columnName, false);
        }

        final String availColumns = getPreferences().get(REPORT_COLUMNS, String.join(",", columnNames));

        if (availColumns != null && !availColumns.isBlank()) {
            String[] columns = availColumns.split(",");

            for (String column : columns) {
                columnComboBox.setChecked(column, true);
            }
        }
    }

    @FXML
    private void handleResetAll() {
        columnComboBox.setAllChecked();

        JavaFXUtils.runLater(this::updateDateRanges);
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
        final AbstractReportTableModel model = createReportModel();

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
        final List<String> availableColumns = columnComboBox.getCheckedItems();

        return PortfolioReport.createReportModel(accountComboBox.getValue(), startDatePicker.getValue(),
                endDatePicker.getValue(), subAccountCheckBox.isSelected(), longNameCheckBox.isSelected(),
                availableColumns::contains);
    }

    private List<String> getAvailColumns() {
        final List<String> availColumns = new ArrayList<>();

        for (int i = 1; i < PortfolioReport.getColumnCount(); i++) {
            availColumns.add(PortfolioReport.getColumnName(i));
        }

        return availColumns;
    }

}
