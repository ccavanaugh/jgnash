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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.StoredObject;
import jgnash.report.ReportPeriod;
import jgnash.report.ReportPeriodUtils;
import jgnash.text.CommodityFormat;
import jgnash.time.DateUtils;
import jgnash.uifx.Options;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.util.InjectFXML;
import jgnash.util.EncodeDecode;
import jgnash.util.Nullable;

/**
 * Periodic Account Balance Bar Chart.
 *
 * @author Craig Cavanaugh
 */
public class AccountBalanceChartController {

    private static final String CHART_CSS = "jgnash/skin/incomeExpenseBarChart.css";

    private static final String REPORT_PERIOD = "reportPeriod";

    private static final String RUNNING_BALANCE = "runningBalance";

    private static final String MONTHLY_BALANCE = "monthlyBalance";

    private static final String SUB_ACCOUNTS = "subAccounts";

    private static final String SELECTED_ACCOUNTS = "selectedAccounts";

    private static final int BAR_GAP = 1;

    private static final int CATEGORY_GAP = 20;

    private final Preferences preferences = Preferences.userNodeForPackage(AccountBalanceChartController.class)
            .node("AccountBalanceChart");

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private StackPane chartPane;

    @FXML
    private VBox accountComboVBox;

    @FXML
    private RadioButton monthlyBalance;

    @FXML
    private RadioButton runningBalance;

    @FXML
    private AccountComboBox accountComboBox;

    @FXML
    private ComboBox<ReportPeriod> periodComboBox;

    @FXML
    private BarChart<String, Number> barChart;

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    private CheckBox includeSubAccounts;

    @FXML
    private ResourceBundle resources;

    private CurrencyNode defaultCurrency;

    private NumberFormat numberFormat;

    private final Account NOP_ACCOUNT = new Account();

    // List to retain auxiliary AccountComboBoxes
    private final List<AccountComboBox> auxAccountComboBoxList = new ArrayList<>();

    private final ChangeListener<Account> auxListener = (observable, oldValue, newValue) -> {
        if (newValue != null) {
            if (newValue == NOP_ACCOUNT) {
                Platform.runLater(AccountBalanceChartController.this::trimAuxAccountCombos);
            } else {
                if (!isEmptyAccountComboPresent()) {
                    Platform.runLater(() -> addAuxAccountCombo(null));
                }
            }

            Platform.runLater(AccountBalanceChartController.this::updateChart);
            Platform.runLater(AccountBalanceChartController.this::saveSelectedAccounts);
        }
    };

    @FXML
    public void initialize() {
        accountComboBox.setPredicate(AccountComboBox.getShowAllPredicate());

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        periodComboBox.getItems().addAll(ReportPeriod.MONTHLY, ReportPeriod.QUARTERLY);
        periodComboBox.setValue(ReportPeriod.values()[preferences.getInt(REPORT_PERIOD,
                ReportPeriod.MONTHLY.ordinal())]);

        defaultCurrency = engine.getDefaultCurrency();
        numberFormat = CommodityFormat.getFullNumberFormat(defaultCurrency);

        barChart.getStylesheets().addAll(CHART_CSS);
        barChart.getYAxis().setLabel(defaultCurrency.getSymbol());
        barChart.barGapProperty().set(BAR_GAP);
        barChart.setCategoryGap(CATEGORY_GAP);
        barChart.setLegendVisible(false);
        barChart.getXAxis().setLabel(resources.getString("Column.Period"));
        barChart.getYAxis().setLabel(resources.getString("Column.Balance") + " : " + defaultCurrency.getSymbol());

        // Respect animation preference
        barChart.animatedProperty().setValue(Options.animationsEnabledProperty().get());

        startDatePicker.setValue(DateUtils.getFirstDayOfTheMonth(endDatePicker.getValue().minusMonths(12)));

        // Force a defaults
        includeSubAccounts.setSelected(preferences.getBoolean(SUB_ACCOUNTS, true));
        runningBalance.setSelected(preferences.getBoolean(RUNNING_BALANCE, true));
        monthlyBalance.setSelected(preferences.getBoolean(MONTHLY_BALANCE, false));

        restoreSelectedAccounts();

        accountComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                defaultCurrency = newValue.getCurrencyNode();
                numberFormat = CommodityFormat.getFullNumberFormat(defaultCurrency);

                Platform.runLater(AccountBalanceChartController.this::updateChart);
                Platform.runLater(AccountBalanceChartController.this::saveSelectedAccounts);
            }
        });

        // Generic listener.  No super efficient but reduces listener count
        final ChangeListener<Object> listener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                Platform.runLater(AccountBalanceChartController.this::updateChart);

                preferences.putBoolean(MONTHLY_BALANCE, monthlyBalance.isSelected());
                preferences.putBoolean(RUNNING_BALANCE, runningBalance.isSelected());
                preferences.putBoolean(SUB_ACCOUNTS, includeSubAccounts.isSelected());
                preferences.putInt(REPORT_PERIOD, periodComboBox.getValue().ordinal());
            }
        };

        addAuxAccountCombo(null);   // load the initial aux account combo

        periodComboBox.valueProperty().addListener(listener);
        startDatePicker.valueProperty().addListener(listener);
        endDatePicker.valueProperty().addListener(listener);
        runningBalance.selectedProperty().addListener(listener);
        monthlyBalance.selectedProperty().addListener(listener);
        includeSubAccounts.selectedProperty().addListener(listener);

        // Push the initial load to the end of the platform thread for better startup and a nicer visual effect
        Platform.runLater(this::updateChart);
    }

    /**
     * Stores a list of selected accounts.
     */
    private void saveSelectedAccounts() {
        final List<String> accounts
                = getSelectedAccounts().stream().map(StoredObject::getUuid).collect(Collectors.toList());

        preferences.put(SELECTED_ACCOUNTS, EncodeDecode.encodeStringCollection(accounts));
    }

    private void restoreSelectedAccounts() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final List<String> accountIds
                = new ArrayList<>(EncodeDecode.decodeStringCollection(preferences.get(SELECTED_ACCOUNTS, "")));

        if (!accountIds.isEmpty()) {
            // set Primary account
            Account account = engine.getAccountByUuid(accountIds.get(0));
            if (account != null) {
                accountComboBox.setValue(account);
            }

            if (accountIds.size() > 1) {
                for (int i = 1; i < accountIds.size(); i++) {
                    account = engine.getAccountByUuid(accountIds.get(i));
                    if (account != null) {
                        addAuxAccountCombo(account);
                    }
                }
            }
        }

        trimAuxAccountCombos();
    }

    private void addAuxAccountCombo(@Nullable Account account) {

        final AccountComboBox auxComboBox = new AccountComboBox();
        auxComboBox.setMaxWidth(Double.MAX_VALUE);
        auxComboBox.setPredicate(AccountComboBox.getShowAllPredicate());
        auxComboBox.getUnfilteredItems().add(0, NOP_ACCOUNT);
        auxComboBox.setValue(account == null ? NOP_ACCOUNT : account);
        auxComboBox.valueProperty().addListener(auxListener);

        auxAccountComboBoxList.add(auxComboBox);
        accountComboVBox.getChildren().add(auxComboBox);
    }

    private void trimAuxAccountCombos() {

        final List<AccountComboBox> empty = auxAccountComboBoxList.stream()
                .filter(accountComboBox -> accountComboBox.getValue() == NOP_ACCOUNT).collect(Collectors.toList());

        // Reverse order so we leave the last empty at the bottom
        Collections.reverse(empty);

        // work backwards through the list to avoid use of an iterator, and leave at least one empty account combo
        for (int i = empty.size() - 1; i > 0; i--) {
            final AccountComboBox accountComboBox = empty.get(i);

            accountComboVBox.getChildren().remove(accountComboBox);
            auxAccountComboBoxList.remove(accountComboBox);
            accountComboBox.valueProperty().removeListener(auxListener);
        }
    }

    private boolean isEmptyAccountComboPresent() {
        boolean result = false;

        for (final AccountComboBox accountComboBox : auxAccountComboBoxList) {
            if (accountComboBox.getValue() == NOP_ACCOUNT) {
                result = true;
                break;
            }
        }

        return result;
    }

    private Collection<Account> getSelectedAccounts() {
        // Use a list for consistent sort order
        final List<Account> accountList = new ArrayList<>();
        accountList.add(accountComboBox.getValue());

        for (final AccountComboBox auxAccountComboBox : auxAccountComboBoxList) {
            final Account account = auxAccountComboBox.getValue();
            if (account != NOP_ACCOUNT && !accountList.contains(account)) {
                accountList.add(account);
            }
        }

        return accountList;
    }

    private void updateChart() {
        barChart.getData().clear();

        final List<ReportPeriodUtils.Descriptor> descriptors = ReportPeriodUtils.getDescriptors(
                periodComboBox.getValue(), startDatePicker.getValue(), endDatePicker.getValue());

        // Create a set of accounts to display
        final Collection<Account> selectedAccounts = getSelectedAccounts();

        barChart.setLegendVisible(selectedAccounts.size() > 1);

        for (final Account account : selectedAccounts) {

            final XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(account.getName());
            barChart.getData().add(series);

            for (final ReportPeriodUtils.Descriptor descriptor : descriptors) {
                final BigDecimal income;

                if (!includeSubAccounts.isSelected()) {

                    if (runningBalance.isSelected()) {
                        income = account.getBalance(descriptor.getEndDate());
                    } else {
                        income = account.getBalance(descriptor.getStartDate(), descriptor.getEndDate());
                    }
                } else {
                    if (runningBalance.isSelected()) {
                        income = account.getTreeBalance(descriptor.getEndDate(), account.getCurrencyNode());
                    } else {
                        income = account.getTreeBalance(descriptor.getStartDate(), descriptor.getEndDate(),
                                account.getCurrencyNode());
                    }
                }

                series.getData().add(new XYChart.Data<>(descriptor.getLabel(), income));
            }

            for (final XYChart.Data<String, Number> data : series.getData()) {
                Tooltip.install(data.getNode(), new Tooltip(numberFormat.format(data.getYValue())));
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
        ((Stage) parentProperty.get().getWindow()).close();
    }
}
