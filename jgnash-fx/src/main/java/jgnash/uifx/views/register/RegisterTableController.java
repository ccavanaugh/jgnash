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
package jgnash.uifx.views.register;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
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
import jgnash.text.CommodityFormat;
import jgnash.uifx.Options;
import jgnash.uifx.util.TableViewManager;
import jgnash.uifx.views.AccountBalanceDisplayManager;
import jgnash.uifx.views.recurring.RecurringEntryDialog;
import jgnash.util.ResourceUtils;
import jgnash.util.function.MemoPredicate;
import jgnash.util.function.PayeePredicate;
import jgnash.util.function.ReconciledPredicate;
import jgnash.util.function.TransactionAgePredicate;

/**
 * Abstract Register Table with stats controller.
 *
 * @author Craig Cavanaugh
 */
abstract class RegisterTableController {

    private final static String PREF_NODE_USER_ROOT = "/jgnash/uifx/views/register";

    @FXML
    protected TableView<Transaction> tableView;

    @FXML
    protected Label balanceLabel;

    @FXML
    protected Label accountNameLabel;

    @FXML
    protected ResourceBundle resources;

    @FXML
    protected ComboBox<ReconciledStateEnum> reconciledStateFilterComboBox;

    @FXML
    protected ComboBox<AgeEnum> transactionAgeFilterComboBox;

    @FXML
    protected TextField memoFilterTextField;

    @FXML
    protected TextField payeeFilterTextField;

    /**
     * Active account for the pane.
     */
    final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    final private ReadOnlyObjectWrapper<Transaction> selectedTransactionProperty = new ReadOnlyObjectWrapper<>();

    /**
     * This is the master list of transactions.
     */
    private final ObservableList<Transaction> observableTransactions = FXCollections.observableArrayList();

    /**
     * Filters may be applied to this list.
     */
    private final FilteredList<Transaction> filteredTransactionList
            = new FilteredList<>(observableTransactions, transaction -> true);

    /**
     * Sorted list of transactions.
     */
    final SortedList<Transaction> sortedList = new SortedList<>(filteredTransactionList);

    final private MessageBusHandler messageBusHandler = new MessageBusHandler();

    TableViewManager<Transaction> tableViewManager;

    final private AccountPropertyWrapper accountPropertyWrapper = new AccountPropertyWrapper();

    // Used for selection summary tooltip
    final private IntegerProperty selectionSize = new SimpleIntegerProperty(0);

    // Used for selection summary tooltip
    final private Tooltip selectionSummaryTooltip = new Tooltip();

    // Used for formatting of the selection summary tooltip
    private NumberFormat numberFormat = NumberFormat.getNumberInstance();

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

            numberFormat = CommodityFormat.getFullNumberFormat(newValue.getCurrencyNode());
        });

        selectedTransactionProperty.bind(tableView.getSelectionModel().selectedItemProperty());

        // Update the selection size property when the selection list changes
        tableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Transaction>) c ->
                selectionSize.set(tableView.getSelectionModel().getSelectedIndices().size()));

        selectionSize.addListener((observable, oldValue, newValue) -> {
            if ((Integer) newValue > 1) {
                final List<Transaction> transactions = new ArrayList<>(tableView.getSelectionModel().getSelectedItems());
                BigDecimal total = BigDecimal.ZERO;

                for (final Transaction transaction : transactions) {
                    if (transaction != null) {
                        total = total.add(transaction.getAmount(accountProperty.get()));
                    }
                }
                selectionSummaryTooltip.setText(numberFormat.format(AccountBalanceDisplayManager.
                        convertToSelectedBalanceMode(accountProperty.get().getAccountType(), total)));
            } else {
                selectionSummaryTooltip.setText(null);
            }
        });

        // For the table view to refresh itself if the mode changes
        AccountBalanceDisplayManager.accountBalanceDisplayMode().addListener((observable, oldValue, newValue)
                -> tableView.refresh());

        reconciledStateFilterComboBox.getItems().addAll(ReconciledStateEnum.values());
        reconciledStateFilterComboBox.setValue(ReconciledStateEnum.ALL);

        transactionAgeFilterComboBox.getItems().addAll(AgeEnum.values());
        transactionAgeFilterComboBox.setValue(AgeEnum.ALL);

        final ChangeListener<Object> filterChangeListener = (observable, oldValue, newValue) -> handleFilterChange();

        reconciledStateFilterComboBox.valueProperty().addListener(filterChangeListener);
        transactionAgeFilterComboBox.valueProperty().addListener(filterChangeListener);

        // Rebuild filters when regex properties change
        Options.regexForFiltersProperty().addListener(filterChangeListener);

        if (memoFilterTextField != null) {  // memo filter may not have been initialized for all register types
            memoFilterTextField.textProperty().addListener(filterChangeListener);
        }

        if (payeeFilterTextField != null) { // payee filter may not have been initialized for all register types
            payeeFilterTextField.textProperty().addListener(filterChangeListener);
        }

        // Listen for engine events
        MessageBus.getInstance().registerListener(messageBusHandler, MessageChannel.TRANSACTION);
    }

    private void handleFilterChange() {

        Predicate<Transaction> predicate = new ReconciledPredicate(accountProperty.get(),
                reconciledStateFilterComboBox.valueProperty().get().getReconciledState())
                .and(new TransactionAgePredicate(transactionAgeFilterComboBox.valueProperty().get().getChronoUnit(),
                        transactionAgeFilterComboBox.valueProperty().get().getAge()));

        if (memoFilterTextField != null) {
            predicate = predicate.and(new MemoPredicate(memoFilterTextField.getText(),
                    Options.regexForFiltersProperty().get()));
        }

        if (payeeFilterTextField != null) {
            predicate = predicate.and(new PayeePredicate(payeeFilterTextField.getText(),
                    Options.regexForFiltersProperty().get()));
        }

        filteredTransactionList.setPredicate(predicate);
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

        if (index > 0) {
            tableView.scrollTo(index - 1);
        } else {
            tableView.scrollTo(transaction);
        }
    }

    /**
     * Ensures the transaction is visible and selects it.
     *
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
            markAsClearedItem.setOnAction(event -> RegisterActions.reconcileTransactionAction(accountProperty.get(),
                    row.getItem(), ReconciledState.CLEARED));

            final MenuItem markAsReconciledItem = new MenuItem(resources.getString("Menu.Reconciled.Name"));
            markAsReconciledItem.setOnAction(event -> RegisterActions.reconcileTransactionAction(accountProperty.get(),
                    row.getItem(), ReconciledState.RECONCILED));

            final MenuItem markAsUnreconciledItem = new MenuItem(resources.getString("Menu.Unreconciled.Name"));
            markAsUnreconciledItem.setOnAction(event -> RegisterActions.reconcileTransactionAction(accountProperty.get(),
                    row.getItem(), ReconciledState.NOT_RECONCILED));

            markedAs.getItems().addAll(markAsClearedItem, markAsReconciledItem, markAsUnreconciledItem);

            final MenuItem duplicateItem = new MenuItem(resources.getString("Menu.Duplicate.Name"));
            duplicateItem.setOnAction(event -> duplicateTransactions());

            final MenuItem jumpItem = new MenuItem(resources.getString("Menu.Jump.Name"));
            jumpItem.setOnAction(event -> handleJumpAction());

            final MenuItem deleteItem = new MenuItem(resources.getString("Menu.Delete.Name"));
            deleteItem.setOnAction(event -> deleteTransactions());

            final MenuItem reminderItem = new MenuItem(resources.getString("Menu.NewReminder.Name"));
            reminderItem.setOnAction(event -> handleCreateNewReminder());

            rowMenu.getItems().addAll(markedAs, new SeparatorMenuItem(), duplicateItem, jumpItem,
                    new SeparatorMenuItem(), deleteItem, new SeparatorMenuItem(), reminderItem);

            // only display context menu for non-null items:
            row.contextMenuProperty().bind(
                    Bindings.when(Bindings.isNotNull(row.itemProperty()))
                            .then(rowMenu)
                            .otherwise((ContextMenu) null));

            // only display the tooltip if the selection size is greater than one
            row.tooltipProperty().bind(
                    Bindings.when(Bindings.greaterThan(selectionSize, 1))
                            .then(selectionSummaryTooltip)
                            .otherwise((Tooltip) null));

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

                            Platform.runLater(() -> {
                                observableTransactions.remove(event.getObject(MessageProperty.TRANSACTION));
                                tableView.refresh();  // this will for the running balance to recalculate
                            });
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

                                // this will for the running balance to recalculate
                                Platform.runLater(() -> tableView.refresh());
                            });
                            break;
                        default:
                    }
                }
            }
        }
    }

    @FXML
    protected void handleResetFilters() {

        Platform.runLater(() -> {
            transactionAgeFilterComboBox.setValue(AgeEnum.ALL);
            reconciledStateFilterComboBox.setValue(ReconciledStateEnum.ALL);

            if (memoFilterTextField != null) {
                memoFilterTextField.setText("");
            }

            if (payeeFilterTextField != null) {
                payeeFilterTextField.setText("");
            }
        });
    }

    private enum ReconciledStateEnum {
        ALL(ResourceUtils.getString("Button.AnyStatus"), null),
        CLEARED(ResourceUtils.getString("Button.Cleared"), ReconciledState.CLEARED),
        NOT_RECONCILED(ResourceUtils.getString("Button.NotReconciled"), ReconciledState.NOT_RECONCILED),
        RECONCILED(ResourceUtils.getString("Button.Reconciled"), ReconciledState.RECONCILED);

        private final String description;

        private final ReconciledState reconciledState;

        ReconciledStateEnum(final String description, final ReconciledState reconciledState) {
            this.description = description;
            this.reconciledState = reconciledState;
        }

        public ReconciledState getReconciledState() {
            return reconciledState;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private enum AgeEnum {
        ALL(ResourceUtils.getString("Button.AllDates"), ChronoUnit.FOREVER, 0),
        Year(ResourceUtils.getString("Button.Last12Months"), ChronoUnit.MONTHS, 12),
        _120(ResourceUtils.getString("Button.Last120Days"), ChronoUnit.DAYS, 120),
        _90(ResourceUtils.getString("Button.Last90Days"), ChronoUnit.DAYS, 90),
        _60(ResourceUtils.getString("Button.Last60Days"), ChronoUnit.DAYS, 60),
        _30(ResourceUtils.getString("Button.Last30Days"), ChronoUnit.DAYS, 30);

        private final String description;
        private final int age;
        private final ChronoUnit chronoUnit;

        AgeEnum(final String description, final ChronoUnit chronoUnit, final int age) {
            this.description = description;
            this.chronoUnit = chronoUnit;
            this.age = age;
        }

        public int getAge() {
            return age;
        }

        public ChronoUnit getChronoUnit() {
            return chronoUnit;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
