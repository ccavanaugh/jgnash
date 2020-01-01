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
package jgnash.uifx.views.register;

import java.math.BigDecimal;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import jgnash.engine.Account;
import jgnash.engine.TransactionEntry;
import jgnash.text.NumericFormats;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.util.TableViewManager;
import jgnash.uifx.views.main.MainView;
import jgnash.util.NotNull;

/**
 * Abstract dialog for split transactions.
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractTransactionEntryDialog extends Stage {

    private static final double[] PREF_COLUMN_WEIGHTS = {50, 50, 0, 0, 0, 0};

    @FXML
    private Button newButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button deleteAllButton;

    @FXML
    private Button closeButton;

    @FXML
    TableView<TransactionEntry> tableView;

    @FXML
    ResourceBundle resources;

    private final ObjectProperty<Account> account = new SimpleObjectProperty<>();

    private TableViewManager<TransactionEntry> tableViewManager;

    private final ObservableList<TransactionEntry> transactionEntries = FXCollections.observableArrayList();

    private final SortedList<TransactionEntry> sortedList = new SortedList<>(transactionEntries);

    abstract String getPrefNode();

    /**
     * This will be called after the dialog is closed.
     */
    private Runnable closeRunnable;

    ObjectProperty<Account> accountProperty() {
        return account;
    }

    ObservableList<TransactionEntry> getTransactionEntries() {
        return transactionEntries;
    }

    @FXML
    private void initialize() {
        accountProperty().addListener((observable, oldValue, newValue) -> {
            initForm();
            loadTable();
        });

        tableView.setTableMenuButtonVisible(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Modify a {@code TransactionEntry} on selection
        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) { // null can occur when the transaction entry list changes
                modifyTransactionEntry(newValue);
            }
        });

        // repack when the list contents change and this dialog is showing
        transactionEntries.addListener((ListChangeListener<TransactionEntry>) c -> {

            // If the list changes, clear the selection
            tableView.getSelectionModel().clearSelection();

            // Repack when the list contents change and this dialog is showing
            if (isShowing()) {
                tableViewManager.packTable();
            }

            // Force a table view refresh, running totals will need to be recalculated
            JavaFXUtils.runLater(() -> tableView.refresh());
        });

        closeButton.setOnAction(event -> closeAction());
        deleteButton.setOnAction(event -> deleteAction());
        deleteAllButton.setOnAction(event -> deleteAllAction());
        newButton.setOnAction(event -> newAction());

        addEventHandler(WindowEvent.WINDOW_SHOWN, event -> {
            tableViewManager.restoreLayout();   // restore layout and pack after the table is visible
        });
    }

    abstract void initForm();

    abstract void newAction();

    abstract void deleteAction();

    abstract void modifyTransactionEntry(@NotNull final TransactionEntry transactionEntry);

    /**
     * Delete all of the transaction entries.
     */
    private void deleteAllAction() {
        tableView.getSelectionModel().clearSelection();
        transactionEntries.clear();
    }

    private void loadTable() {
        tableViewManager = new TableViewManager<>(tableView, getPrefNode());
        tableViewManager.setColumnWeightFactory(getColumnWeightFactory());
        tableViewManager.setPreferenceKeyFactory(() -> accountProperty().get().getUuid().toString());

        sortedList.comparatorProperty().bind(tableView.comparatorProperty());

        buildTable();

        tableView.setItems(sortedList);
    }

    private Callback<Integer, Double> getColumnWeightFactory() {
        return param -> PREF_COLUMN_WEIGHTS[param];
    }

    String[] getSplitColumnName() {
        return RegisterFactory.getSplitColumnNames(accountProperty().get().getAccountType());
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        final String[] columnNames = getSplitColumnName();

        final TableColumn<TransactionEntry, String> accountColumn = new TableColumn<>(columnNames[0]);
        accountColumn.setCellValueFactory(param -> new AccountNameWrapper(param.getValue()));

        final TableColumn<TransactionEntry, String> reconciledColumn = new TableColumn<>(columnNames[1]);
        reconciledColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().
                getReconciled(account.get()).toString()));

        final TableColumn<TransactionEntry, String> memoColumn = new TableColumn<>(columnNames[2]);
        memoColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getMemo()));

        final Callback<TableColumn<TransactionEntry, BigDecimal>, TableCell<TransactionEntry, BigDecimal>> cellFactory =
                cell -> new TransactionEntryCommodityFormatTableCell(NumericFormats.getShortCommodityFormat(account.get().getCurrencyNode()));

        final TableColumn<TransactionEntry, BigDecimal> increaseColumn = new TableColumn<>(columnNames[3]);
        increaseColumn.setCellValueFactory(param -> new IncreaseAmountProperty(param.getValue().
                getAmount(accountProperty().getValue())));

        increaseColumn.setCellFactory(cellFactory);

        final TableColumn<TransactionEntry, BigDecimal> decreaseColumn = new TableColumn<>(columnNames[4]);
        decreaseColumn.setCellValueFactory(param -> new DecreaseAmountProperty(param.getValue().
                getAmount(accountProperty().getValue())));
        decreaseColumn.setCellFactory(cellFactory);

        final TableColumn<TransactionEntry, BigDecimal> balanceColumn = new TableColumn<>(columnNames[5]);
        balanceColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(getBalanceAt(param.getValue())));
        balanceColumn.setCellFactory(cell -> new TransactionEntryCommodityFormatTableCell(NumericFormats.
                getFullCommodityFormat(account.get().getCurrencyNode())));
        balanceColumn.setSortable(false);   // do not allow a sort on the balance

        tableView.getColumns().addAll(memoColumn, accountColumn, reconciledColumn, increaseColumn, decreaseColumn,
                balanceColumn);

        tableViewManager.setColumnFormatFactory(param -> {
            if (param == balanceColumn) {
                return NumericFormats.getFullCommodityFormat(accountProperty().getValue().getCurrencyNode());
            } else if (param == increaseColumn || param == decreaseColumn) {
                return NumericFormats.getShortCommodityFormat(accountProperty().getValue().getCurrencyNode());
            }

            return null;
        });
    }

    private BigDecimal getBalanceAt(final TransactionEntry transactionEntry) {
        BigDecimal balance = BigDecimal.ZERO;

        final Account account = this.account.get();

        if (account != null) {
            final int index = sortedList.indexOf(transactionEntry);

            for (int i = 0; i <= index; i++) {
                balance = balance.add(sortedList.get(i).getAmount(account));
            }
        }
        return balance;
    }

    private void closeAction() {
        closeButton.getScene().getWindow().hide();

        // call the runnable to indicate closure if not null
        if (closeRunnable != null) {
            closeRunnable.run();
        }
    }

    private class AccountNameWrapper extends SimpleStringProperty {
        AccountNameWrapper(final TransactionEntry t) {
            super();

            final Account creditAccount = t.getCreditAccount();

            if (creditAccount != accountProperty().get()) {
                setValue(creditAccount.getName());
            } else {
                setValue(t.getDebitAccount().getName());
            }
        }
    }

    BigDecimal getBalance() {
        if (sortedList.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return getBalanceAt(sortedList.get(sortedList.size() - 1));
    }

    void show(final Runnable runnable) {
        closeRunnable = runnable;

        // Reset bounds before showing
        StageUtils.addBoundsListener(this, getClass(), MainView.getPrimaryStage());

        super.show();
    }
}
