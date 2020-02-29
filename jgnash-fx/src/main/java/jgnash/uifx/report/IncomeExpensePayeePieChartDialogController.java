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
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.Comparators;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.text.NumericFormats;
import jgnash.uifx.Options;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DoughnutChart;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.util.CollectionUtils;
import jgnash.util.EncodeDecode;
import jgnash.util.NotNull;
import jgnash.util.function.PayeePredicate;

/**
 * Income and Expense Payee Pie Chart.
 *
 * @author Craig Cavanaugh
 * @author Pranay Kumar
 */
public class IncomeExpensePayeePieChartDialogController {

    private static final String LAST_ACCOUNT = "lastAccount";

    private static final String FILTERS = "filters";

    private static final char DELIMITER = 0x25FC;

    private static final int CREDIT = 0;

    private static final int DEBIT = 1;

    private static final int MAX_NAME_LENGTH = 12;

    private static final String ELLIPSIS = "…";

    private final Preferences preferences
            = Preferences.userNodeForPackage(IncomeExpensePayeePieChartDialogController.class)
            .node("IncomeExpensePayeePieChart");

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private  TitledPane titledPane;

    @FXML
    private VBox filtersPane;

    @FXML
    private GridPane chartPane;

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

    private final ChangeListener<String> payeeChangeListener = (observable, oldValue, newValue) -> {
        if (newValue != null) {
            if (newValue.isEmpty()) {
                JavaFXUtils.runLater(this::trimAuxPayeeTextFields);
            } else {
                if (!isEmptyPayeeFieldAvailable()) {
                    JavaFXUtils.runLater(this::insertAuxPayeeTextField);
                }
            }
        }
        JavaFXUtils.runLater(this::updateCharts);

        final List<String> filters = getPayeeTextFields().stream()
                .filter(textField -> !textField.getText().isEmpty())
                .map(TextInputControl::getText).collect(Collectors.toList());

        if (filters.isEmpty()) {
            preferences.remove(FILTERS);
        } else {
            preferences.put(FILTERS, EncodeDecode.encodeStringCollection(filters, DELIMITER));
        }
    };

    @FXML
    public void initialize() {

        // Respect animation preference
        debitPieChart.animatedProperty().set(Options.animationsEnabledProperty().get());
        creditPieChart.animatedProperty().set(Options.animationsEnabledProperty().get());

        creditPieChart.centerTitleProperty().set(resources.getString("Column.Credit"));
        debitPieChart.centerTitleProperty().set(resources.getString("Column.Debit"));

        accountComboBox.setPredicate(AccountComboBox.getShowAllPredicate());

        if (preferences.get(LAST_ACCOUNT, null) != null) {
            final String uuid = preferences.get(LAST_ACCOUNT, "");

            if (!uuid.isEmpty()) {

                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                Objects.requireNonNull(engine);

                final Account account = engine.getAccountByUuid(UUID.fromString(uuid));

                if (account != null) {
                    accountComboBox.setValue(account);
                }
            }
        }

        startDatePicker.setValue(endDatePicker.getValue().minusYears(1));

        final ChangeListener<Object> listener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateCharts();
                preferences.put(LAST_ACCOUNT, accountComboBox.getValue().getUuid().toString());
            }
        };

        accountComboBox.valueProperty().addListener(listener);
        startDatePicker.valueProperty().addListener(listener);
        endDatePicker.valueProperty().addListener(listener);

        creditPieChart.setLegendSide(Side.BOTTOM);
        debitPieChart.setLegendSide(Side.BOTTOM);

        // Load in the first aux payee text field
        insertAuxPayeeTextField();

        restoreFilters();

        // Expand the titled pane if filters are being used
        JavaFXUtils.runLater(() -> {
            if (getPayeeTextFields().size() > 1) {
               titledPane.setExpanded(true);
            }
        });

        // Push the initial load to the end of the platform thread for better startup and nicer visual effect
        JavaFXUtils.runLater(this::updateCharts);
    }

    private void restoreFilters() {
        final List<String> filters
                = new ArrayList<>(EncodeDecode.decodeStringCollection(preferences.get(FILTERS, ""), DELIMITER));

        // Need to reverse the order since we are inserting at the top
        Collections.reverse(filters);

        filters.forEach(this::insertAuxPayeeTextField);
    }

    private void insertAuxPayeeTextField() {
        final TextField payeeField = new TextField();
        payeeField.textProperty().addListener(payeeChangeListener);
        filtersPane.getChildren().add(payeeField);
    }

    private void insertAuxPayeeTextField(final String filter) {
        final TextField payeeField = new TextField(filter);
        payeeField.textProperty().addListener(payeeChangeListener);
        filtersPane.getChildren().add(0, payeeField);
    }

    private void trimAuxPayeeTextFields() {

        final List<TextField> empty = filtersPane.getChildren().stream().filter(TextField.class::isInstance)
                                                 .filter(node -> ((TextField) node).getText().isEmpty())
                                                 .map(TextField.class::cast).collect(Collectors.toList());

        // Reverse order so we leave the last empty at the bottom
        Collections.reverse(empty);

        for (int i = empty.size() - 1; i > 0; i--) {
            final TextField textField = empty.get(i);

            textField.textProperty().removeListener(payeeChangeListener);
            filtersPane.getChildren().remove(textField);
        }
    }

    private boolean isEmptyPayeeFieldAvailable() {
        boolean result = false;

        for (TextField textField : getPayeeTextFields()) {
            if (textField.getText().isEmpty()) {
                result = true;
                break;
            }
        }

        return result;
    }

    private List<TextField> getPayeeTextFields() {
        return filtersPane.getChildren().stream()
                          .filter(TextField.class::isInstance).map(TextField.class::cast)
                          .collect(Collectors.toList());
    }

    private void updateCharts() {
        final Account account = accountComboBox.getValue();

        if (account != null) {
            final ObservableList<PieChart.Data>[] chartData = createPieDataSet(account);

            creditPieChart.setData(chartData[CREDIT]);
            debitPieChart.setData(chartData[DEBIT]);

            final NumberFormat numberFormat = NumericFormats.getFullCommodityFormat(account.getCurrencyNode());

            // Calculate the totals for percentage value
            final double creditTotal = chartData[CREDIT].parallelStream().mapToDouble(PieChart.Data::getPieValue).sum();
            final double debitTotal = chartData[DEBIT].parallelStream().mapToDouble(PieChart.Data::getPieValue).sum();

            final NumberFormat percentFormat = NumberFormat.getPercentInstance();
            percentFormat.setMaximumFractionDigits(1);
            percentFormat.setMinimumFractionDigits(1);

            // Install tooltips on the data after it has been added to the chart
            creditPieChart.getData().forEach(data ->
                    Tooltip.install(data.getNode(), new Tooltip((data.getNode().getUserData()
                            + "\n" + numberFormat.format(data.getPieValue()) + "(" +
                            percentFormat.format(data.getPieValue() / creditTotal)) + ")")));

            // Install tooltips on the data after it has been added to the chart
            debitPieChart.getData().forEach(data ->
                    Tooltip.install(data.getNode(), new Tooltip(((data.getNode().getUserData())
                            + "\n" + numberFormat.format(data.getPieValue()) + "(" +
                            percentFormat.format(data.getPieValue() / debitTotal)) + ")")));

            creditPieChart.centerSubTitleProperty().set(numberFormat.format(creditTotal));
            debitPieChart.centerSubTitleProperty().set(numberFormat.format(debitTotal));
        } else {
            creditPieChart.setData(FXCollections.emptyObservableList());
            creditPieChart.setTitle("No Data");

            debitPieChart.setData(FXCollections.emptyObservableList());
            debitPieChart.setTitle("No Data");
        }
    }


    private ObservableList<PieChart.Data>[] createPieDataSet(@NotNull final Account account) {

        @SuppressWarnings("unchecked")
        final ObservableList<PieChart.Data>[] chartData = new ObservableList[2];

        chartData[CREDIT] = FXCollections.observableArrayList();
        chartData[DEBIT] = FXCollections.observableArrayList();

        final Map<String, BigDecimal> names = new HashMap<>();

        final List<TranTuple> list = getTransactions(account, new ArrayList<>(), startDatePicker.getValue(),
                endDatePicker.getValue());

        final CurrencyNode currency = account.getCurrencyNode();

        // Create a list of predicates
        final List<Predicate<Transaction>> predicates = getPayeeTextFields().stream()
                .filter(textField -> !textField.getText().isEmpty())
                .map(textField -> new PayeePredicate(textField.getText(), Options.regexForFiltersProperty().get()))
                .collect(Collectors.toList());

        // Iterate through the list and add up filtered payees
        for (final TranTuple tranTuple : list) {
            final String payee = tranTuple.transaction.getPayee();
            BigDecimal sum = tranTuple.transaction.getAmount(tranTuple.account);

            sum = sum.multiply(tranTuple.account.getCurrencyNode().getExchangeRate(currency));

            boolean keep = false;

            if (predicates.isEmpty()) {
                keep = true;
            } else {
                for (final Predicate<Transaction> predicate : predicates) {
                    if (predicate.test(tranTuple.transaction)) {
                        keep = true;
                        break;
                    }
                }
            }

            if (keep) {
                if (names.containsKey(payee)) {
                    sum = sum.add(names.get(payee));
                }

                names.put(payee, sum);
            }
        }

        final Map<String, BigDecimal> sortedNames = CollectionUtils.sortMapByValue(names);

        for (final Map.Entry<String, BigDecimal> entry : sortedNames.entrySet()) {
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
        }
        
		return string.substring(0, MAX_NAME_LENGTH - 1) + ELLIPSIS;
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
