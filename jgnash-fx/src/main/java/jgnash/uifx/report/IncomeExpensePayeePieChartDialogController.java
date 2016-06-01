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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.Comparators;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.text.CommodityFormat;
import jgnash.uifx.Options;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DoughnutChart;
import jgnash.uifx.util.InjectFXML;
import jgnash.util.NotNull;

/**
 * Income and Expense Payee Pie Chart.
 *
 * @author Craig Cavanaugh
 * @author Pranay Kumar
 */
public class IncomeExpensePayeePieChartDialogController {

    private static final int CREDIT = 0;

    private static final int DEBIT = 1;

    private static final int MAX_NAME_LENGTH = 12;

    private static final String ELLIPSIS = "…";

    private static final String CHART_CSS = "jgnash/skin/incomeExpensePieChart.css";

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private DoughnutChart debitPieChart;

    @FXML
    private DoughnutChart creditPieChart;

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    private AccountComboBox accountComboBox;

    @FXML
    private ResourceBundle resources;

    private static final String LAST_ACCOUNT = "lastAccount";

    @FXML
    public void initialize() {

        // Respect animation preference
        debitPieChart.animatedProperty().setValue(Options.animationsEnabledProperty().get());
        creditPieChart.animatedProperty().setValue(Options.animationsEnabledProperty().get());

        creditPieChart.getStylesheets().addAll(CHART_CSS);
        debitPieChart.getStylesheets().addAll(CHART_CSS);

        creditPieChart.centerTitleProperty().setValue(resources.getString("Column.Credit"));
        debitPieChart.centerTitleProperty().setValue(resources.getString("Column.Debit"));

        accountComboBox.setPredicate(AccountComboBox.getShowAllPredicate());

        final Preferences preferences = Preferences.userNodeForPackage(IncomeExpensePayeePieChartDialogController.class);

        if (preferences.get(LAST_ACCOUNT, null) != null) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            final Account account = engine.getAccountByUuid(preferences.get(LAST_ACCOUNT, null));

            if (account != null) {
                accountComboBox.setValue(account);
            }
        }

        startDatePicker.setValue(endDatePicker.getValue().minusYears(1));

        final ChangeListener<Object> listener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateCharts();
                preferences.put(LAST_ACCOUNT, accountComboBox.getValue().getUuid());
            }
        };

        accountComboBox.valueProperty().addListener(listener);
        startDatePicker.valueProperty().addListener(listener);
        endDatePicker.valueProperty().addListener(listener);

        creditPieChart.setLegendSide(Side.BOTTOM);
        debitPieChart.setLegendSide(Side.BOTTOM);

        // Push the initial load to the end of the platform thread for better startup and nicer visual effect
        Platform.runLater(this::updateCharts);
    }

    private void updateCharts() {
        final Account account = accountComboBox.getValue();

        if (account != null) {
            final ObservableList<PieChart.Data>[] chartData = createPieDataSet(account);

            creditPieChart.setData(chartData[CREDIT]);
            debitPieChart.setData(chartData[DEBIT]);

            final NumberFormat numberFormat = CommodityFormat.getFullNumberFormat(account.getCurrencyNode());

            // Calculate the totals for percentage value
            final double creditTotal = chartData[CREDIT].parallelStream().mapToDouble(PieChart.Data::getPieValue).sum();
            final double debitTotal = chartData[DEBIT].parallelStream().mapToDouble(PieChart.Data::getPieValue).sum();

            final NumberFormat percentFormat = NumberFormat.getPercentInstance();
            percentFormat.setMaximumFractionDigits(1);
            percentFormat.setMinimumFractionDigits(1);

            // Install tooltips on the data after it has been added to the chart
            creditPieChart.getData().stream().forEach(data ->
                    Tooltip.install(data.getNode(), new Tooltip((data.getNode().getUserData()
                            + "\n" + numberFormat.format(data.getPieValue()) + "(" +
                            percentFormat.format(data.getPieValue() / creditTotal)) + ")")));

            // Install tooltips on the data after it has been added to the chart
            debitPieChart.getData().stream().forEach(data ->
                    Tooltip.install(data.getNode(), new Tooltip(((data.getNode().getUserData())
                            + "\n" + numberFormat.format(data.getPieValue()) + "(" +
                            percentFormat.format(data.getPieValue() / debitTotal)) + ")")));

            creditPieChart.centerSubTitleProperty().setValue(numberFormat.format(creditTotal));
            debitPieChart.centerSubTitleProperty().setValue(numberFormat.format(debitTotal));
        } else {
            creditPieChart.setData(FXCollections.emptyObservableList());
            creditPieChart.setTitle("No Data");

            debitPieChart.setData(FXCollections.emptyObservableList());
            debitPieChart.setTitle("No Data");
        }
    }


    private ObservableList<PieChart.Data>[] createPieDataSet(@NotNull final Account account) {

        @SuppressWarnings("unchecked")
        final ObservableList<PieChart.Data>[] chartData = (ObservableList<PieChart.Data>[]) new ObservableList[2];

        chartData[CREDIT] = FXCollections.observableArrayList();
        chartData[DEBIT] = FXCollections.observableArrayList();

        final Map<String, BigDecimal> names = new HashMap<>();

        final List<TranTuple> list = getTransactions(account, new ArrayList<>(), startDatePicker.getValue(),
                endDatePicker.getValue());

        final CurrencyNode currency = account.getCurrencyNode();

        for (final TranTuple tranTuple : list) {

            final String payee = tranTuple.transaction.getPayee();
            BigDecimal sum = tranTuple.transaction.getAmount(tranTuple.account);

            sum = sum.multiply(tranTuple.account.getCurrencyNode().getExchangeRate(currency));

            /*if (useFilters.isSelected()) {
                for (String aFilterList : filterList) {

                    PayeeMatcher pm = new PayeeMatcher(aFilterList, false);

                    if (pm.matches(tran)) {
                        payee = aFilterList;
                        //System.out.println(filterList.get(i));
                        break;
                    }
                }
            }*/

            if (names.containsKey(payee)) {
                sum = sum.add(names.get(payee));
            }

            names.put(payee, sum);
        }

        for (final Map.Entry<String, BigDecimal> entry : names.entrySet()) {
            final PieChart.Data data = new PieChart.Data(truncateString(entry.getKey()),
                    entry.getValue().abs().doubleValue());

            // nodes are created lazily.  Set the user data (Account) after the node is created
            data.nodeProperty().addListener((observable, oldValue, newValue) -> newValue.setUserData(entry.getKey()));

            if (entry.getValue().signum() == -1) {
                chartData[DEBIT].add(data);
            } else {
                chartData[CREDIT].add(data);
            }
        }

        return chartData;
    }

    private static String truncateString(final String string) {
        if (string.length() <= MAX_NAME_LENGTH) {
            return string;
        } else {
            return string.substring(0, MAX_NAME_LENGTH - 1) + ELLIPSIS;
        }
    }

    private static class TranTuple {

        final Account account;

        final Transaction transaction;

        TranTuple(Account account, Transaction transaction) {
            this.account = account;
            this.transaction = transaction;
        }
    }

    private List<TranTuple> getTransactions(final Account account, final List<TranTuple> transactions,
                                            final LocalDate startDate, final LocalDate endDate) {

        for (final Transaction transaction : account.getTransactions(startDate, endDate)) {
            TranTuple tuple = new TranTuple(account, transaction);
            transactions.add(tuple);
        }

        for (final Account child : account.getChildren(Comparators.getAccountByCode())) {
            getTransactions(child, transactions, startDate, endDate);
        }

        return transactions;
    }


    @FXML
    private void handleSaveAction() {
        ChartUtilities.saveChart(creditPieChart);
    }

    @FXML
    private void handleCopyToClipboard() {
        ChartUtilities.copyToClipboard(creditPieChart);
    }

    @FXML
    private void handlePrintAction() {
        ChartUtilities.printChart(creditPieChart);
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parentProperty.get().getWindow()).close();
    }
}
