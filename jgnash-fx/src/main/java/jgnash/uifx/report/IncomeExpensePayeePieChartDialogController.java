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
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.Comparators;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.uifx.Options;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.util.InjectFXML;
import jgnash.util.NotNull;

/**
 * Income and Expense Payee Pie Chart.
 *
 * @author Craig Cavanaugh
 * @author Pranay Kumar
 */
@SuppressWarnings("WeakerAccess")
public class IncomeExpensePayeePieChartDialogController {

    private static final int CREDIT = 0;

    private static final int DEBIT = 1;

    private static final int MAX_NAME_LENGTH = 12;

    private static final String ELLIPSIS = "…";

    private static final String CHART_CSS = "jgnash/skin/incomeExpensePieChart.css";

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private PieChart debitPieChart;

    @FXML
    private PieChart creditPieChart;

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
                updateChart();
                preferences.put(LAST_ACCOUNT, accountComboBox.getValue().getUuid());
            }
        };

        accountComboBox.valueProperty().addListener(listener);
        startDatePicker.valueProperty().addListener(listener);
        endDatePicker.valueProperty().addListener(listener);

        creditPieChart.setLegendSide(Side.BOTTOM);
        debitPieChart.setLegendSide(Side.BOTTOM);

        // Push the initial load to the end of the platform thread for better startup and nicer visual effect
        Platform.runLater(this::updateChart);
    }

    private void updateChart() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final Account a = accountComboBox.getValue();

        if (a != null) {

            ObservableList<PieChart.Data>[] chartData = createPieDataSet(a);

            creditPieChart.setData(chartData[CREDIT]);
            debitPieChart.setData(chartData[DEBIT]);

            final NumberFormat percentFormat = NumberFormat.getPercentInstance();
            percentFormat.setMaximumFractionDigits(1);
            percentFormat.setMinimumFractionDigits(1);

            /*

            // Install tooltips on the data after it has been added to the chart
            creditPieChart.getData().stream().forEach(data ->
                    Tooltip.install(data.getNode(), new Tooltip((((Account) data.getNode().getUserData()).getName()
                            + " - " + percentFormat.format(data.getPieValue() / 100d))))); */

            /*final String title;

            // pick an appropriate title
            if (a.getAccountType() == AccountType.EXPENSE) {
                title = resources.getString("Title.PercentExpense");
            } else if (a.getAccountType() == AccountType.INCOME) {
                title = resources.getString("Title.PercentIncome");
            } else {
                title = resources.getString("Title.PercentDist");
            }

            creditPieChart.setTitle(title + " - " + accountComboBox.getValue().getName() + " - " + numberFormat.format(total));*/

            // abs() on all values won't work if children aren't of uniform sign,
            // then again, this chart is not right to display those trees
            //boolean negate = total != null && total.signum() < 0;
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

        CurrencyNode currency = account.getCurrencyNode();

        //final NumberFormat numberFormat = CommodityFormat.getFullNumberFormat(currency);

        for (final TranTuple tranTuple : list) {

            Transaction tran = tranTuple.transaction;
            //Account account = tranTuple.account;

            String payee = tran.getPayee();
            BigDecimal sum = tran.getAmount(tranTuple.account);

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
            //final String label = child.getName() + " - " + numberFormat.format(value);
            final PieChart.Data data = new PieChart.Data(truncateName(entry.getKey()),
                    entry.getValue().abs().doubleValue());

            // nodes are created lazily.  Set the user data (Account) after the node is created
            //data.nodeProperty().addListener((observable, oldValue, newValue) -> newValue.setUserData(child));

            if (entry.getValue().signum() == -1) {
                chartData[DEBIT].add(data);
            } else {
                chartData[CREDIT].add(data);
            }
        }

        return chartData;
    }

    private static String truncateName(final String name) {
        if (name.length() <= MAX_NAME_LENGTH) {
            return name;
        } else {
            return name.substring(0, MAX_NAME_LENGTH - 1) + ELLIPSIS;
        }
    }

    private static class TranTuple {

        final Account account;

        final Transaction transaction;

        public TranTuple(Account account, Transaction transaction) {
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
