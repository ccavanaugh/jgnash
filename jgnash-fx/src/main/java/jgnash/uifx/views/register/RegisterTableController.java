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

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;

import jgnash.engine.Account;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.uifx.util.TableViewManager;

/**
 * Abstract Register Table with stats controller
 *
 * @author Craig Cavanaugh
 */
public abstract class RegisterTableController implements Initializable {

    final protected static String PREF_NODE_USER_ROOT = "/jgnash/uifx/views/register";

    @FXML
    protected TableView<Transaction> tableView;

    @FXML
    protected Label balanceLabel;

    @FXML
    protected Label accountNameLabel;

    /**
     * Active account for the pane
     */
    final protected ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    final protected ObservableList<Transaction> observableTransactions = FXCollections.observableArrayList();

    final protected SortedList<Transaction> sortedList = new SortedList<>(observableTransactions);

    final private MessageBusHandler messageBusHandler = new MessageBusHandler();

    protected TableViewManager<Transaction> tableViewManager;

    protected ResourceBundle resources;

    final private AccountPropertyWrapper accountPropertyWrapper = new AccountPropertyWrapper();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.resources = resources;

        // Bind the account property
        getAccountPropertyWrapper().getAccountProperty().bind(accountProperty);

        accountNameLabel.textProperty().bind(getAccountPropertyWrapper().getAccountNameProperty());
        balanceLabel.textProperty().bind(getAccountPropertyWrapper().getAccountBalanceProperty());

        tableView.setTableMenuButtonVisible(true);
        tableView.setRowFactory(new TransactionRowFactory());
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Load the table on change
        getAccountProperty().addListener((observable, oldValue, newValue) -> loadAccount());

        // Listen for engine events
        MessageBus.getInstance().registerListener(messageBusHandler, MessageChannel.TRANSACTION);
    }

    private void loadAccount() {
        tableViewManager = new TableViewManager<>(tableView, PREF_NODE_USER_ROOT);
        tableViewManager.setColumnWeightFactory(getColumnWeightFactory());
        tableViewManager.setPreferenceKeyFactory(() -> getAccountProperty().get().getUuid());

        sortedList.comparatorProperty().bind(tableView.comparatorProperty());

        buildTable();
        loadTable();
    }

    abstract Callback<Integer, Double> getColumnWeightFactory();

    protected ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    protected AccountPropertyWrapper getAccountPropertyWrapper() {
        return accountPropertyWrapper;
    }

    abstract protected void buildTable();

    protected void loadTable() {
        observableTransactions.clear();

        if (accountProperty.get() != null) {
            observableTransactions.addAll(accountProperty.get().getSortedTransactionList());

            tableView.setItems(sortedList);
            tableViewManager.restoreLayout();   // required to table view manager is to work
        }
    }

    protected void deleteTransactions() {
        final List<Transaction> transactionList = tableView.getSelectionModel().getSelectedItems();

        RegisterActions.deleteTransactionAction(transactionList.toArray(new Transaction[transactionList.size()]));
    }

    private class TransactionRowFactory implements Callback<TableView<Transaction>, TableRow<Transaction>> {

        @Override
        public TableRow<Transaction> call(final TableView<Transaction> param) {

            final TableRow<Transaction> row = new TableRow<>();
            final ContextMenu rowMenu = new ContextMenu();

            final Menu markedAs = new Menu(resources.getString("Menu.MarkAs.Name"));
            final MenuItem markAsClearedItem = new MenuItem(resources.getString("Menu.Cleared.Name"));
            markAsClearedItem.setOnAction(event -> RegisterActions.reconcileTransactionAction(accountProperty.get(), row.getItem(), ReconciledState.CLEARED));

            final MenuItem markAsReconciledItem = new MenuItem(resources.getString("Menu.Reconciled.Name"));
            markAsReconciledItem.setOnAction(event -> RegisterActions.reconcileTransactionAction(accountProperty.get(), row.getItem(), ReconciledState.RECONCILED));

            final MenuItem markAsUnreconciledItem = new MenuItem(resources.getString("Menu.Unreconciled.Name"));
            markAsUnreconciledItem.setOnAction(event -> RegisterActions.reconcileTransactionAction(accountProperty.get(), row.getItem(), ReconciledState.NOT_RECONCILED));

            markedAs.getItems().addAll(markAsClearedItem, markAsReconciledItem, markAsUnreconciledItem);

            // TODO Connect to dialogs, checks, and configuration
            final MenuItem duplicateItem = new MenuItem(resources.getString("Menu.Duplicate.Name"));
            final MenuItem jumpItem = new MenuItem(resources.getString("Menu.Jump.Name"));

            final MenuItem deleteItem = new MenuItem(resources.getString("Menu.Delete.Name"));
            deleteItem.setOnAction(event -> deleteTransactions());

            rowMenu.getItems().addAll(markedAs, new SeparatorMenuItem(), duplicateItem, jumpItem, new SeparatorMenuItem(), deleteItem);

            // only display context menu for non-null items:
            row.contextMenuProperty().bind(
                    Bindings.when(Bindings.isNotNull(row.itemProperty()))
                            .then(rowMenu)
                            .otherwise((ContextMenu) null));

            return row;
        }
    }

    private class MessageBusHandler implements MessageListener {

        @SuppressWarnings("SuspiciousMethodCalls")
        @Override
        public void messagePosted(final Message event) {
            final Account account = accountProperty.getValue();

            if (account != null) {
                if (event.getObject(MessageProperty.ACCOUNT).equals(account)) {
                    switch (event.getEvent()) {
                        case TRANSACTION_REMOVE:
                            Platform.runLater(() -> observableTransactions.remove(event.getObject(MessageProperty.TRANSACTION)));
                            break;
                        case TRANSACTION_ADD:
                            Platform.runLater(() -> {
                                observableTransactions.addAll((Transaction)event.getObject(MessageProperty.TRANSACTION));
                                FXCollections.sort(observableTransactions);
                            });
                            break;
                        default:
                    }
                }
            }
        }
    }
}
