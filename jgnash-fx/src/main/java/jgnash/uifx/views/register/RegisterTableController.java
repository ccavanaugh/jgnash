/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.uifx.views.register;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import jgnash.engine.Account;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.text.CommodityFormat;
import jgnash.uifx.utils.TableViewManager;
import jgnash.util.DateUtils;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * Register Table with stats controller
 *
 * TODO Transaction lists need to update when account changes
 * TODO Add context menu for reconcile state
 *
 * @author Craig Cavanaugh
 */
public class RegisterTableController implements Initializable {

    final private double[] PREF_COLUMN_WEIGHTS = {0, 0, 33, 33, 33, 0, 0, 0, 0};

    final private static String PREF_NODE_USER_ROOT = "/jgnash/uifx/views/register";

    final private AccountPropertyWrapper accountPropertyWrapper = new AccountPropertyWrapper();

    private ResourceBundle resources;

    @FXML
    private TableView<Transaction> tableView;

    @FXML
    private Label reconciledBalanceLabel;

    @FXML
    private Label balanceLabel;

    @FXML
    private Label accountNameLabel;

    /**
     * Active account for the pane
     */
    final private ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private TableViewManager<Transaction> tableViewManager;

    final private ObservableList<Transaction> observableTransactions = FXCollections.observableArrayList();

    final private SortedList<Transaction> sortedList = new SortedList<>(observableTransactions);

    ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    AccountPropertyWrapper getAccountPropertyWrapper() {
        return accountPropertyWrapper;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.resources = resources;

        tableView.setTableMenuButtonVisible(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Bind the account property
        getAccountPropertyWrapper().getAccountProperty().bind(accountProperty);

        // Bind label test to the account property wrapper
        accountNameLabel.textProperty().bind(getAccountPropertyWrapper().getAccountNameProperty());
        balanceLabel.textProperty().bind(getAccountPropertyWrapper().getAccountBalanceProperty());
        reconciledBalanceLabel.textProperty().bind(getAccountPropertyWrapper().getReconciledAmountProperty());

        tableViewManager = new TableViewManager<>(tableView, PREF_NODE_USER_ROOT);
        tableViewManager.setColumnWeightFactory(param -> PREF_COLUMN_WEIGHTS[param]);
        tableViewManager.setPreferenceKeyFactory(() -> getAccountProperty().get().getUuid());

        sortedList.comparatorProperty().bind(tableView.comparatorProperty());

        buildTable();

        // Load the table on change
        getAccountProperty().addListener((observable, oldValue, newValue) -> loadTable());
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        final TableColumn<Transaction, Date> dateColumn = new TableColumn<>(resources.getString("Column.Date"));
        dateColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getDate()));
        dateColumn.setCellFactory(cell -> new TransactionDateTableCell());

        final TableColumn<Transaction, String> numberColumn = new TableColumn<>(resources.getString("Column.Num"));
        numberColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getNumber()));
        numberColumn.setCellFactory(cell -> new TransactionStringTableCell());

        final TableColumn<Transaction, String> payeeColumn = new TableColumn<>(resources.getString("Column.Payee"));
        payeeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getPayee()));
        payeeColumn.setCellFactory(cell -> new TransactionStringTableCell());

        final TableColumn<Transaction, String> memoColumn = new TableColumn<>(resources.getString("Column.Memo"));
        memoColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getMemo()));
        memoColumn.setCellFactory(cell -> new TransactionStringTableCell());

        final TableColumn<Transaction, String> accountColumn = new TableColumn<>(resources.getString("Column.Account"));
        accountColumn.setCellValueFactory(param -> new AccountNameWrapper(param.getValue()));
        accountColumn.setCellFactory(cell -> new TransactionStringTableCell());

        final TableColumn<Transaction, String> reconciledColumn = new TableColumn<>(resources.getString("Column.Clr"));
        reconciledColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getReconciled(accountProperty.getValue()).toString()));
        reconciledColumn.setCellFactory(cell -> new TransactionStringTableCell());

        final TableColumn<Transaction, BigDecimal> increaseColumn = new TableColumn<>(resources.getString("Column.Increase"));
        increaseColumn.setCellValueFactory(param -> new IncreaseAmountProperty(param.getValue().getAmount(getAccountProperty().getValue())));
        increaseColumn.setCellFactory(cell -> new TransactionCommodityFormatTableCell(CommodityFormat.getShortNumberFormat(accountProperty.get().getCurrencyNode())));

        final TableColumn<Transaction, BigDecimal> decreaseColumn = new TableColumn<>(resources.getString("Column.Decrease"));
        decreaseColumn.setCellValueFactory(param -> new DecreaseAmountProperty(param.getValue().getAmount(getAccountProperty().getValue())));
        decreaseColumn.setCellFactory(cell -> new TransactionCommodityFormatTableCell(CommodityFormat.getShortNumberFormat(accountProperty.get().getCurrencyNode())));

        final TableColumn<Transaction, BigDecimal> balanceColumn = new TableColumn<>(resources.getString("Column.Balance"));
        balanceColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(getBalanceAt(param.getValue())));
        balanceColumn.setCellFactory(cell -> new TransactionCommodityFormatTableCell(CommodityFormat.getFullNumberFormat(accountProperty.get().getCurrencyNode())));
        balanceColumn.setSortable(false);   // do not allow a sort on the balance

        tableView.getColumns().addAll(dateColumn, numberColumn, payeeColumn, memoColumn, accountColumn, reconciledColumn, increaseColumn, decreaseColumn, balanceColumn);

        tableViewManager.setColumnFormatFactory(param -> {
            if (param == balanceColumn) {
                return CommodityFormat.getFullNumberFormat(getAccountProperty().getValue().getCurrencyNode());
            } else if (param == increaseColumn || param == decreaseColumn) {
                return CommodityFormat.getShortNumberFormat(getAccountProperty().getValue().getCurrencyNode());
            } else if (param == dateColumn) {
                return DateUtils.getShortDateFormat();
            }

            return null;
        });
    }

    private BigDecimal getBalanceAt(final Transaction transaction) {
        BigDecimal balance = BigDecimal.ZERO;

        final Account account = accountProperty.get();

        if (account != null) {
            final int index = sortedList.indexOf(transaction);

            for (int i = 0; i <= index; i++) {
                balance = balance.add(sortedList.get(i).getAmount(account));
            }
        }
        return balance;
    }

    private void loadTable() {
        observableTransactions.clear();

        if (accountProperty.get() != null) {
            observableTransactions.addAll(accountProperty.get().getSortedTransactionList());

            tableView.setItems(sortedList);
            tableViewManager.restoreLayout();   // required to table view manager is to work
        }
    }

    public void packTable() {
        tableViewManager.packTable();
    }

    private static class IncreaseAmountProperty extends SimpleObjectProperty<BigDecimal> {
        IncreaseAmountProperty(final BigDecimal value) {
            if (value.signum() >= 0) {
                setValue(value);
            } else {
                setValue(null);
            }
        }
    }

    private static class DecreaseAmountProperty extends SimpleObjectProperty<BigDecimal> {
        DecreaseAmountProperty(final BigDecimal value) {
            if (value.signum() < 0) {
                setValue(value.abs());
            } else {
                setValue(null);
            }
        }
    }

    private class AccountNameWrapper extends SimpleStringProperty {

        final String split = resources.getString("Button.Splits");

        AccountNameWrapper(final Transaction t) {
            super();

            if (t instanceof InvestmentTransaction) {
                setValue(((InvestmentTransaction) t).getInvestmentAccount().getName());
            } else {
                int count = t.size();
                if (count > 1) {
                    setValue("[ " + count + " " + split + " ]");
                } else {
                    Account creditAccount = t.getTransactionEntries().get(0).getCreditAccount();
                    if (creditAccount != accountProperty.get()) {
                        setValue(creditAccount.getName());
                    } else {
                        setValue(t.getTransactionEntries().get(0).getDebitAccount().getName());
                    }
                }
            }
        }
    }
}
