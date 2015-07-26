/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionType;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.engine.recurring.Reminder;
import jgnash.uifx.util.TableViewManager;
import jgnash.uifx.views.recurring.RecurringEntryDialog;

/**
 * Abstract Register Table with stats controller
 *
 * @author Craig Cavanaugh
 */
public abstract class RegisterTableController {

    private final static String PREF_NODE_USER_ROOT = "/jgnash/uifx/views/register";

    @FXML
    protected TableView<Transaction> tableView;

    @FXML
    protected Label balanceLabel;

    @FXML
    protected Label accountNameLabel;

    @FXML
    protected ResourceBundle resources;

    /**
     * Active account for the pane
     */
    final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    final private ReadOnlyObjectWrapper<Transaction> selectedTransactionProperty = new ReadOnlyObjectWrapper<>();

    private final ObservableList<Transaction> observableTransactions = FXCollections.observableArrayList();

    final SortedList<Transaction> sortedList = new SortedList<>(observableTransactions);

    final private MessageBusHandler messageBusHandler = new MessageBusHandler();

    TableViewManager<Transaction> tableViewManager;

    final private AccountPropertyWrapper accountPropertyWrapper = new AccountPropertyWrapper();

    @FXML
    void initialize() {
        // Bind the account property
        getAccountPropertyWrapper().accountProperty().bind(accountProperty);

        accountNameLabel.textProperty().bind(getAccountPropertyWrapper().accountNameProperty());
        balanceLabel.textProperty().bind(getAccountPropertyWrapper().accountBalanceProperty());

        tableView.setTableMenuButtonVisible(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Load the table on change and set the row factory if the account in not locked
        getAccountProperty().addListener((observable, oldValue, newValue) -> {
            loadAccount();

            if (!newValue.isLocked()) {
                tableView.setRowFactory(new TransactionRowFactory());
            }
        });

        selectedTransactionProperty.bind(tableView.getSelectionModel().selectedItemProperty());

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

    ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    ReadOnlyObjectProperty<Transaction> getSelectedTransactionProperty() {
        return selectedTransactionProperty.getReadOnlyProperty();
    }

    void clearTableSelection() {
        tableView.getSelectionModel().clearSelection();
    }

    AccountPropertyWrapper getAccountPropertyWrapper() {
        return accountPropertyWrapper;
    }

    /**
     * Scrolls the view to ensure selection visibility.  If possible, the next index is shown for improved
     * visual appearance.
     *
     * @param transaction transaction to show in table
     */
    private void scrollToTransaction(final Transaction transaction) {
        final int index = tableView.getItems().indexOf(transaction);

        if (index < tableView.getItems().size()) {
            tableView.scrollTo(index + 1);
        } else {
            tableView.scrollTo(transaction);
        }
    }

    /**
     * Ensures the transaction is visible and selects it
     * @param transaction Transaction that needs to be visible in the view
     */
    void selectTransaction(final Transaction transaction) {
        scrollToTransaction(transaction);
        tableView.getSelectionModel().select(transaction);

        // The table needs to be focused for the row selection to highlight
        Platform.runLater(tableView::requestFocus);
    }

    abstract protected void buildTable();

    private void loadTable() {
        observableTransactions.clear();

        if (accountProperty.get() != null) {
            observableTransactions.addAll(accountProperty.get().getSortedTransactionList());

            tableView.setItems(sortedList);
            tableViewManager.restoreLayout();   // required to table view manager is to work

            tableView.scrollTo(observableTransactions.size());  // scroll to the end of the table
        }
    }

    List<Transaction> getSelectedTransactions() {
        return tableView.getSelectionModel().getSelectedItems();
    }

    void deleteTransactions() {
        final List<Transaction> transactionList = tableView.getSelectionModel().getSelectedItems();

        RegisterActions.deleteTransactionAction(transactionList.toArray(new Transaction[transactionList.size()]));
    }

    private void duplicateTransactions() {
        final List<Transaction> transactionList = tableView.getSelectionModel().getSelectedItems();

        RegisterActions.duplicateTransaction(accountProperty.get(), transactionList);
    }

    private void handleCreateNewReminder() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final Reminder reminder = engine.createDefaultReminder(selectedTransactionProperty.get(), accountProperty.get());

        final Optional<Reminder> optional = RecurringEntryDialog.showAndWait(reminder);

        if (optional.isPresent()) {
            engine.addReminder(optional.get());
        }
    }

    void handleJumpAction() {
        Transaction t = selectedTransactionProperty.get();

        if (t != null) {
            if (t.getTransactionType() == TransactionType.DOUBLEENTRY) {
                final Set<Account> set = t.getAccounts();
                set.stream().filter(a -> !accountProperty.get().equals(a)).forEach(a -> RegisterStage.getRegisterStage(a).show(t));
            } else if (t.getTransactionType() == TransactionType.SPLITENTRY) {
                final Account common = t.getCommonAccount();

                if (!accountProperty.get().equals(common)) {
                    RegisterStage.getRegisterStage(common).show(t);
                }
            } else if (t instanceof InvestmentTransaction) {
                final Account invest = ((InvestmentTransaction) t).getInvestmentAccount();

                if (!accountProperty.get().equals(invest)) {
                    RegisterStage.getRegisterStage(invest).show(t);
                }
            }
        }
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

            final MenuItem duplicateItem = new MenuItem(resources.getString("Menu.Duplicate.Name"));
            duplicateItem.setOnAction(event -> duplicateTransactions());

            final MenuItem jumpItem = new MenuItem(resources.getString("Menu.Jump.Name"));
            jumpItem.setOnAction(event -> handleJumpAction());

            final MenuItem deleteItem = new MenuItem(resources.getString("Menu.Delete.Name"));
            deleteItem.setOnAction(event -> deleteTransactions());

            final MenuItem reminderItem = new MenuItem(resources.getString("Menu.NewReminder.Name"));
            reminderItem.setOnAction(event -> handleCreateNewReminder());

            rowMenu.getItems().addAll(markedAs, new SeparatorMenuItem(), duplicateItem, jumpItem, new SeparatorMenuItem(), deleteItem, new SeparatorMenuItem(), reminderItem);

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
                            // clear the selection of the transaction is currently selected
                            if (tableView.getSelectionModel().getSelectedItems().contains(event.getObject(MessageProperty.TRANSACTION))) {
                                Platform.runLater(RegisterTableController.this::clearTableSelection);
                            }

                            Platform.runLater(() -> observableTransactions.remove(event.getObject(MessageProperty.TRANSACTION)));
                            break;
                        case TRANSACTION_ADD:
                            Platform.runLater(() -> {
                                final Transaction transaction = event.getObject(MessageProperty.TRANSACTION);

                                final int index = Collections.binarySearch(observableTransactions, transaction, tableView.getComparator());

                                if (index < 0) {
                                    observableTransactions.add(-index - 1, transaction);
                                }

                                // scroll to the new transaction
                                scrollToTransaction(event.getObject(MessageProperty.TRANSACTION));
                            });
                            break;
                        default:
                    }
                }
            }
        }
    }
}
