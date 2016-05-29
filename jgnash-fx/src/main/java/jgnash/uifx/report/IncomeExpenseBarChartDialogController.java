/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.report.ReportPeriod;
import jgnash.report.ReportPeriodUtils;
import jgnash.text.CommodityFormat;
import jgnash.uifx.Options;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.util.InjectFXML;
import jgnash.time.DateUtils;

/**
 * Income and Expense Bar Chart.
 *
 * @author Craig Cavanaugh
 */
@SuppressWarnings("WeakerAccess")
public class IncomeExpenseBarChartDialogController {

    private static final String CHART_CSS = "jgnash/skin/incomeExpenseBarChart.css";

    private static final String REPORT_PERIOD = "reportPeriod";

    private static final int BAR_GAP = 1;

    private static final int PERIOD_GAP = 20;

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private ComboBox<ReportPeriod> periodComboBox;

    @FXML
    private BarChart<String, Number> barChart;

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    private ResourceBundle resources;

    private CurrencyNode defaultCurrency;

    private NumberFormat numberFormat;

    @FXML
    public void initialize() {

        final Preferences preferences = Preferences.userNodeForPackage(IncomeExpenseBarChartDialogController.class);

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        periodComboBox.getItems().addAll(ReportPeriod.values());
        periodComboBox.setValue(ReportPeriod.values()[preferences.getInt(REPORT_PERIOD, ReportPeriod.MONTHLY.ordinal())]);

        defaultCurrency = engine.getDefaultCurrency();
        numberFormat = CommodityFormat.getFullNumberFormat(defaultCurrency);

        barChart.getStylesheets().addAll(CHART_CSS);
        barChart.getYAxis().setLabel(defaultCurrency.getSymbol());
        barChart.barGapProperty().set(BAR_GAP);
        barChart.setCategoryGap(PERIOD_GAP);

        // Respect animation preference
        barChart.animatedProperty().setValue(Options.animationsEnabledProperty().get());

        startDatePicker.setValue(DateUtils.getFirstDayOfTheMonth(endDatePicker.getValue().minusMonths(11)));

        final ChangeListener<Object> listener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                Platform.runLater(this::updateChart);
            }
        };

        startDatePicker.valueProperty().addListener(listener);
        endDatePicker.valueProperty().addListener(listener);

        periodComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            preferences.putInt(REPORT_PERIOD, newValue.ordinal());
            Platform.runLater(this::updateChart);
        });

        // Push the initial load to the end of the platform thread for better startup and a nicer visual effect
        Platform.runLater(this::updateChart);
    }

    private void updateChart() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final List<Account> incomeAccounts = engine.getIncomeAccountList();

        final List<Account> expenseAccounts = engine.getExpenseAccountList();

        barChart.getData().clear();

        final List<ReportPeriodUtils.Descriptor> descriptors = ReportPeriodUtils.getDescriptors(
                periodComboBox.getValue(), startDatePicker.getValue(), endDatePicker.getValue());

        // Income Series
        final XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName(AccountType.INCOME.toString());
        barChart.getData().add(incomeSeries);

        // Expense Series
        final XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName(AccountType.EXPENSE.toString());
        barChart.getData().add(expenseSeries);

        // Profit Series
        final XYChart.Series<String, Number> profitSeries = new XYChart.Series<>();
        profitSeries.setName(resources.getString("Word.NetIncome"));
        barChart.getData().add(profitSeries);

        for (final ReportPeriodUtils.Descriptor descriptor : descriptors) {
            final BigDecimal income = getSum(incomeAccounts, descriptor.getStartDate(), descriptor.getEndDate());
            final BigDecimal expense = getSum(expenseAccounts, descriptor.getStartDate(), descriptor.getEndDate());

            incomeSeries.getData().add(new XYChart.Data<>(descriptor.getLabel(), income));
            expenseSeries.getData().add(new XYChart.Data<>(descriptor.getLabel(), expense));
            profitSeries.getData().add(new XYChart.Data<>(descriptor.getLabel(), income.add(expense)));
        }

        for (XYChart.Data<String, Number> data : incomeSeries.getData()) {
            Tooltip.install(data.getNode(), new Tooltip(numberFormat.format(data.getYValue())));
        }

        for (XYChart.Data<String, Number> data : expenseSeries.getData()) {
            Tooltip.install(data.getNode(), new Tooltip(numberFormat.format(data.getYValue())));
        }

        for (XYChart.Data<String, Number> data : profitSeries.getData()) {
            Tooltip.install(data.getNode(), new Tooltip(numberFormat.format(data.getYValue())));
        }
    }

    private BigDecimal getSum(final List<Account> accounts, final LocalDate statDate, final LocalDate endDate) {
        BigDecimal sum = BigDecimal.ZERO;

        for (final Account account : accounts) {
            sum = sum.add(account.getBalance(statDate, endDate, defaultCurrency));
        }

        return sum.negate();
    }

    @FXML
    private void handleSaveAction() {
        ChartUtilities.saveChart(barChart);
    }

    @FXML
    private void handleCopyToClipboard() {
        ChartUtilities.copyToClipboard(barChart);
    }

    @FXML
    private void handlePrintAction() {
        ChartUtilities.printChart(barChart);
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parentProperty.get().getWindow()).close();
    }
}
