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

import java.text.NumberFormat;
import java.time.LocalDate;
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
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.text.NumericFormats;
import jgnash.uifx.Options;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DoughnutChart;
import jgnash.uifx.resource.cursor.CustomCursor;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.util.function.ParentAccountPredicate;

/**
 * Income and Expense Pie Chart.
 *
 * @author Craig Cavanaugh
 */
public class IncomeExpensePieChartDialogController {

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

    private boolean nodeFocused = false;

    private static final String LAST_ACCOUNT = "lastAccount";

    @FXML
    public void initialize() {

        // Respect animation preference
        pieChart.animatedProperty().set(Options.animationsEnabledProperty().get());

        accountComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.getParent().getAccountType() != AccountType.ROOT) {
                pieChart.setCursor(CustomCursor.getZoomOutCursor());
            } else {
                pieChart.setCursor(Cursor.DEFAULT);
            }
        });

        final Preferences preferences = Preferences.userNodeForPackage(IncomeExpensePieChartDialogController.class)
                .node("IncomeExpensePieChart");

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

        // zoom out
        pieChart.setOnMouseClicked(event -> {
            if (!nodeFocused && accountComboBox.getValue().getParent().getAccountType() != AccountType.ROOT) {
                accountComboBox.setValue(accountComboBox.getValue().getParent());
            }
        });

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

            double total = a.getTreeBalance(startDatePicker.getValue(), endDatePicker.getValue(),
                    defaultCurrency).doubleValue();

            for (final Account child : a.getChildren()) {
                double balance = child.getTreeBalance(startDatePicker.getValue(), endDatePicker.getValue(),
                        defaultCurrency).doubleValue();

                if (balance > 0 || balance < 0) {
                    final String label = child.getName() + " - " + numberFormat.format(balance);
                    final PieChart.Data data = new PieChart.Data(label, balance / total * 100);

                    // nodes are created lazily.  Set the user data (Account) after the node is created
                    data.nodeProperty().addListener((observable, oldValue, newValue) -> newValue.setUserData(child));

                    pieChartData.add(data);
                }
            }

            pieChart.setData(pieChartData);

            final NumberFormat percentFormat = NumberFormat.getPercentInstance();
            percentFormat.setMaximumFractionDigits(1);
            percentFormat.setMinimumFractionDigits(1);

            // Install tooltips on the data after it has been added to the chart
            pieChart.getData().forEach(data ->
                    Tooltip.install(data.getNode(), new Tooltip((((Account) data.getNode().getUserData()).getName()
                            + " - " + percentFormat.format(data.getPieValue() / 100d)))));

            // Indicate the node can be clicked on to zoom into the next account level
            for (final PieChart.Data data : pieChart.getData()) {
                data.getNode().setOnMouseEntered(event -> {
                    final Account account = (Account) data.getNode().getUserData();
                    if (account.isParent()) {
                        data.getNode().setCursor(CustomCursor.getZoomInCursor());
                    } else {
                        data.getNode().setCursor(Cursor.DEFAULT);
                    }

                    nodeFocused = true;
                });

                data.getNode().setOnMouseExited(event -> nodeFocused = false);

                // zoom in on click if this is a parent account
                data.getNode().setOnMouseClicked(event -> {
                    if (data.getNode().getUserData() != null) {
                        if (((Account) data.getNode().getUserData()).isParent()) {
                            accountComboBox.setValue((Account) data.getNode().getUserData());
                        }
                    }
                });
            }

            final String title;

            // pick an appropriate title
            switch (a.getAccountType()) {
                case EXPENSE:
                    title = resources.getString("Title.PercentExpense");
                    break;
                case INCOME:
                    title = resources.getString("Title.PercentIncome");
                    break;
                default:
                    title = resources.getString("Title.PercentDist");
                    break;
            }

            pieChart.setTitle(title);

            pieChart.centerTitleProperty().set(accountComboBox.getValue().getName());
            pieChart.centerSubTitleProperty().set(numberFormat.format(total));

            // abs() on all values won't work if children aren't of uniform sign,
            // then again, this chart is not right to display those trees
            //boolean negate = total != null && total.signum() < 0;
        } else {
            pieChart.setData(FXCollections.emptyObservableList());
            pieChart.setTitle("No Data");
        }
    }

    void setParameters(final AccountType accountType, final LocalDate startDate, final LocalDate endDate) {
        //PK: dates can be directly set
        startDatePicker.setValue(startDate);
        endDatePicker.setValue(endDate);

        //PK: We assume that the root of all income or expense accounts is one account under the root. Select that
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        for ( final Account comboAccount : accountComboBox.getItems() ) {
            if ( (accountType == comboAccount.getAccountType()) && (comboAccount.getParent() == engine.getRootAccount()) ) {
                accountComboBox.setValue(comboAccount);
                break;
            }
        }
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
