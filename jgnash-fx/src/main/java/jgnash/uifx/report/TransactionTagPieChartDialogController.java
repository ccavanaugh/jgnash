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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.prefs.Preferences;

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
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Tag;
import jgnash.engine.Transaction;
import jgnash.text.NumericFormats;
import jgnash.uifx.Options;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DoughnutChart;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.util.function.ParentAccountPredicate;

/**
 * Transaction Tag Pie Chart.
 *
 * @author Craig Cavanaugh
 */
public class TransactionTagPieChartDialogController {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private StackPane chartPane;

    @FXML
    private DoughnutChart pieChart;

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    private AccountComboBox accountComboBox;

    @FXML
    private ResourceBundle resources;

    private static final String LAST_ACCOUNT = "lastAccount";

    private final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @FXML
    public void initialize() {

        final Preferences preferences = Preferences.userNodeForPackage(TransactionTagPieChartDialogController.class)
                .node("TransactionTagPieChart");

        // Respect animation preference
        pieChart.animatedProperty().set(Options.animationsEnabledProperty().get());

        accountComboBox.setPredicate(new ParentAccountPredicate());

        if (preferences.get(LAST_ACCOUNT, null) != null) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            final String uuid = preferences.get(LAST_ACCOUNT, "");

            if (!uuid.isEmpty()) {
                final Account account = engine.getAccountByUuid(UUID.fromString(uuid));

                if (account != null) {
                    accountComboBox.setValue(account);
                }
            }
        }

        startDatePicker.setValue(endDatePicker.getValue().minusYears(1));

        final ChangeListener<Object> listener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateChart();
                preferences.put(LAST_ACCOUNT, accountComboBox.getValue().getUuid().toString());
            }
        };

        accountComboBox.valueProperty().addListener(listener);
        startDatePicker.valueProperty().addListener(listener);
        endDatePicker.valueProperty().addListener(listener);

        pieChart.setLegendSide(Side.BOTTOM);

        // Push the initial load to the end of the platform thread for better startup and nicer visual effect
        JavaFXUtils.runLater(this::updateChart);
    }

    private void updateChart() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final Account a = accountComboBox.getValue();

        if (a != null) {
            final CurrencyNode defaultCurrency = a.getCurrencyNode();

            final NumberFormat numberFormat = NumericFormats.getFullCommodityFormat(defaultCurrency);

            final ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

            final Map<Tag, BigDecimal> balanceMap = new HashMap<>();

            // Iterate through all the Tags in use
            for (final Tag tag : engine.getTagsInUse()) {

                BigDecimal balance = new BigDecimal(BigInteger.ZERO);

                for (final Account child : a.getChildren()) {
                    balance = balance.add(getSumForTag(tag, child));
                }

                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    balanceMap.put(tag, balance);
                }
            }

            // Sum of all the balances
            final BigDecimal total = balanceMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

            // Iterate and crate each pie slice
            for (final Map.Entry<Tag, BigDecimal> entry : balanceMap.entrySet()) {
                final Tag tag = entry.getKey();
                final BigDecimal balance = entry.getValue();

                final String label = tag.getName() + " - " + numberFormat.format(balance.doubleValue());

                // protect against a div by zero caused by net zero of income and expense
                double value = total.compareTo(BigDecimal.ZERO) == 0 ? 0 :
                        balance.divide(total, MathContext.DECIMAL64).multiply(ONE_HUNDRED).doubleValue();

                final PieChart.Data data = new PieChart.Data(label, value);

                // nodes are created lazily.  Set the user data (Tag) after the node is created
                data.nodeProperty().addListener((observable, oldValue, newValue) -> newValue.setUserData(tag));

                pieChartData.add(data);
            }

            pieChart.setData(pieChartData);

            final NumberFormat percentFormat = NumberFormat.getPercentInstance();
            percentFormat.setMaximumFractionDigits(1);
            percentFormat.setMinimumFractionDigits(1);

            // Install tooltips on the data after it has been added to the chart
            pieChart.getData().forEach(data ->
                    Tooltip.install(data.getNode(), new Tooltip((((Tag) data.getNode().getUserData()).getName()
                            + " - " + percentFormat.format(data.getPieValue() / 100d)))));

            pieChart.setTitle(resources.getString("Title.TransactionTagPieChart"));

            pieChart.centerTitleProperty().set(accountComboBox.getValue().getName());
            pieChart.centerSubTitleProperty().set(numberFormat.format(total));
        } else {
            pieChart.setData(FXCollections.emptyObservableList());
            pieChart.setTitle("No Data");
        }
    }

    private BigDecimal getSumForTag(final Tag tag, final Account account) {

        BigDecimal sum = BigDecimal.ZERO;

        for (final Transaction transaction : account.getTransactions(startDatePicker.getValue(), endDatePicker.getValue())) {
            if (transaction.getTags().contains(tag)) {
                sum = sum.add(transaction.getAmount(account));
            }
        }

        // TODO: exchange rate...
        if (account.getChildCount() > 0) {
            for (Account child : account.getChildren()) {
                sum = sum.add(getSumForTag(tag, child));
            }
        }

        return sum;
    }

    @FXML
    private void handleSaveAction() {
        ChartUtilities.saveChart(chartPane);
    }

    @FXML
    private void handleCopyToClipboard() {
        ChartUtilities.copyToClipboard(chartPane);
    }

    @FXML
    private void handlePrintAction() {
        ChartUtilities.printChart(chartPane);
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parent.get().getWindow()).close();
    }
}
