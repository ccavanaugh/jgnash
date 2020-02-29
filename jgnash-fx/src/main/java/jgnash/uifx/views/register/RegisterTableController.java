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
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.collections.WeakSetChangeListener;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Callback;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.ReconciledState;
import jgnash.engine.Tag;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionType;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.engine.recurring.Reminder;
import jgnash.resource.util.ResourceUtils;
import jgnash.text.NumericFormats;
import jgnash.time.DateUtils;
import jgnash.uifx.Options;
import jgnash.uifx.control.TableViewEx;
import jgnash.uifx.skin.StyleClass;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.util.TableViewManager;
import jgnash.uifx.views.AccountBalanceDisplayManager;
import jgnash.uifx.views.AccountBalanceDisplayMode;
import jgnash.uifx.views.recurring.RecurringEntryDialog;
import jgnash.util.function.MemoPredicate;
import jgnash.util.function.PayeePredicate;
import jgnash.util.function.ReconciledPredicate;
import jgnash.util.function.TagPredicate;
import jgnash.util.function.TransactionAgePredicate;

/**
 * Abstract Register Table with stats controller.
 *
 * @author Craig Cavanaugh
 */
abstract class RegisterTableController {

    private static final String PREF_NODE_USER_ROOT = "/jgnash/uifx/views/register";

    /**
     * Active account for the pane.
     */
    final ObjectProperty<Account> account = new SimpleObjectProperty<>();

    private final ReadOnlyObjectWrapper<Transaction> selectedTransaction = new ReadOnlyObjectWrapper<>();

    /**
     * This is the master list of transactions.
     */
    private final ObservableList<Transaction> observableTransactions = FXCollections.observableArrayList();

    /**
     * Filters may be applied to this list.
     */
    private final FilteredList<Transaction> filteredTransactionList = new FilteredList<>(observableTransactions,
            transaction -> true);

    /**
     * Sorted list of transactions.
     */
    final SortedList<Transaction> sortedList = new SortedList<>(filteredTransactionList);

    private final MessageBusHandler messageBusHandler = new MessageBusHandler();

    private final AccountPropertyWrapper accountPropertyWrapper = new AccountPropertyWrapper();

    // Used for selection summary tooltip
    private final IntegerProperty selectionSize = new SimpleIntegerProperty(0);

    // Used for selection summary tooltip
    private final Tooltip selectionSummaryTooltip = new Tooltip();

    /**
     * Listens for changes to the font scale
     */
    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<Number> fontScaleListener;

    @FXML
    protected TableViewEx<Transaction> tableView;

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

    @FXML
    private TransactionTagPane tagPane;

    TableViewManager<Transaction> tableViewManager;

    // Used for formatting of the selection summary tooltip
    private NumberFormat numberFormat = NumberFormat.getNumberInstance();

    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<Object> filterChangeListener;

    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<Number> selectionSizeListener;

    @SuppressWarnings("FieldCanBeLocal")
    private SetChangeListener<Tag> tagSetChangeListener;

    @SuppressWarnings("FieldCanBeLocal")
    private ListChangeListener<Transaction> selectedItemsListener;

    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<AccountBalanceDisplayMode> accountBalanceDisplayModeChangeListener;

    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<String> formatListener;

    @FXML
    void initialize() {

        // table view displays the sorted list of data. The comparator property must be
        // bound
        tableView.setItems(sortedList);
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());

        // register the copy to clipboard function
        tableView.setClipBoardStringFunction(this::transactionToExcel);

        // Bind the account property
        getAccountPropertyWrapper().accountProperty().bind(account);

        accountNameLabel.textProperty().bind(getAccountPropertyWrapper().accountNameProperty());
        balanceLabel.textProperty().bind(getAccountPropertyWrapper().accountBalanceProperty());

        tableView.setTableMenuButtonVisible(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // hide the horizontal scrollbar and prevent ghosting
        tableView.getStylesheets().addAll(StyleClass.HIDE_HORIZONTAL_CSS);

        // Load the table on change and set the row factory if the account in not locked
        accountProperty().addListener((observable, oldValue, newValue) -> {
            loadAccount();

            if (!newValue.isLocked()) {
                tableView.setRowFactory(new TransactionRowFactory());
            }

            numberFormat = NumericFormats.getFullCommodityFormat(newValue.getCurrencyNode());
        });

        selectedTransaction.bind(tableView.getSelectionModel().selectedItemProperty());

        // Update the selection size property when the selection list changes
        selectedItemsListener = c -> selectionSize.set(tableView.getSelectionModel().getSelectedIndices().size());

        tableView.getSelectionModel().getSelectedItems().addListener(new WeakListChangeListener<>(selectedItemsListener));

        // updates a tooltip based on current selection
        selectionSizeListener = (observable, oldValue, newValue) -> {
            if ((Integer) newValue > 1) {
                final List<Transaction> transactions = new ArrayList<>(
                        tableView.getSelectionModel().getSelectedItems());
                BigDecimal total = BigDecimal.ZERO;

                for (final Transaction transaction : transactions) {
                    if (transaction != null) {
                        total = total.add(transaction.getAmount(account.get()));
                    }
                }
                selectionSummaryTooltip.setText(numberFormat.format(AccountBalanceDisplayManager
                                                                            .convertToSelectedBalanceMode(account.get().getAccountType(), total)));
            } else {
                selectionSummaryTooltip.setText(null);
            }
        };

        selectionSize.addListener(new WeakChangeListener<>(selectionSizeListener));

        accountBalanceDisplayModeChangeListener = (observable, oldValue, newValue) -> tableView.refresh();

        // For the table view to refresh itself if the mode changes
        AccountBalanceDisplayManager.accountBalanceDisplayMode()
                .addListener(new WeakChangeListener<>(accountBalanceDisplayModeChangeListener));

        reconciledStateFilterComboBox.getItems().addAll(ReconciledStateEnum.values());
        reconciledStateFilterComboBox.setValue(ReconciledStateEnum.ALL);

        transactionAgeFilterComboBox.getItems().addAll(AgeEnum.values());
        transactionAgeFilterComboBox.setValue(AgeEnum.ALL);

        filterChangeListener = (observable, oldValue, newValue) -> handleFilterChange();
        tagSetChangeListener = change -> handleFilterChange();

        reconciledStateFilterComboBox.valueProperty().addListener(new WeakChangeListener<>(filterChangeListener));
        transactionAgeFilterComboBox.valueProperty().addListener(new WeakChangeListener<>(filterChangeListener));

        // Rebuild filters when regex properties change
        Options.regexForFiltersProperty().addListener(new WeakChangeListener<>(filterChangeListener));

        if (memoFilterTextField != null) { // memo filter may not have been initialized for all register types
            memoFilterTextField.textProperty().addListener(new WeakChangeListener<>(filterChangeListener));
        }

        if (payeeFilterTextField != null) { // payee filter may not have been initialized for all register types
            payeeFilterTextField.textProperty().addListener(new WeakChangeListener<>(filterChangeListener));
        }

        tagPane.getSelectedTags().addListener(new WeakSetChangeListener<>(tagSetChangeListener));

        // Repack the table if the font scale changes, must use a weak listener to prevent memory leaks
        fontScaleListener = (observable, oldValue, newValue) -> tableViewManager.packTable();
        ThemeManager.fontScaleProperty().addListener(new WeakChangeListener<>(fontScaleListener));

        // Listen for changes to formatting preferences and force and update
        formatListener = (observable, oldValue, newValue) -> {
            tableView.refresh();
            JavaFXUtils.runLater(tableViewManager::packTable);
        };

        Options.fullNumericFormatProperty().addListener(new WeakChangeListener<>(formatListener));
        Options.shortNumericFormatProperty().addListener(new WeakChangeListener<>(formatListener));
        Options.shortDateFormatProperty().addListener(new WeakChangeListener<>(formatListener));

        // Listen for transaction events
        MessageBus.getInstance().registerListener(messageBusHandler, MessageChannel.TRANSACTION);
    }

    private void handleFilterChange() {
        Predicate<Transaction> predicate = new ReconciledPredicate(account.get(),
                reconciledStateFilterComboBox.valueProperty().get().getReconciledState()).and(
                new TransactionAgePredicate(transactionAgeFilterComboBox.valueProperty().get().getChronoUnit(),
                        transactionAgeFilterComboBox.valueProperty().get().getAge()));

        if (memoFilterTextField != null) {
            predicate = predicate
                                .and(new MemoPredicate(memoFilterTextField.getText(), Options.regexForFiltersProperty().get()));
        }

        if (payeeFilterTextField != null) {
            predicate = predicate
                                .and(new PayeePredicate(payeeFilterTextField.getText(), Options.regexForFiltersProperty().get()));
        }

        predicate = predicate.and(new TagPredicate(tagPane.getSelectedTags()));

        filteredTransactionList.setPredicate(predicate);
    }

    private void loadAccount() {
        tableViewManager = new TableViewManager<>(tableView, PREF_NODE_USER_ROOT);
        tableViewManager.setPreferenceKeyFactory(() -> accountProperty().get().getUuid().toString());
        tableViewManager.setColumnWeightFactory(getColumnWeightFactory());
        tableViewManager.setDefaultColumnVisibilityFactory(getColumnVisibilityFactory());
        tableViewManager.manualColumnPackingProperty().bind(Options.autoPackTablesProperty().not());

        buildTable();

        /*
         * push to the front of the application thread to ensure table build is complete
         * before data is loaded this prevents inconsistent and random behavior for
         * column sizing
         */
        JavaFXUtils.runNow(this::loadTable);
    }

    abstract Callback<Integer, Double> getColumnWeightFactory();

    abstract Callback<Integer, Boolean> getColumnVisibilityFactory();

    ObjectProperty<Account> accountProperty() {
        return account;
    }

    ReadOnlyObjectProperty<Transaction> selectedTransactionProperty() {
        return selectedTransaction.getReadOnlyProperty();
    }

    void clearTableSelection() {
        tableView.getSelectionModel().clearSelection();
    }

    AccountPropertyWrapper getAccountPropertyWrapper() {
        return accountPropertyWrapper;
    }

    /**
     * Scrolls the view to ensure selection visibility. If possible, the next index
     * is shown for improved visual appearance.
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
        JavaFXUtils.runLater(tableView::requestFocus);
    }

    protected abstract void buildTable();

    private void loadTable() {
        observableTransactions.clear();

        if (account.get() != null) {
            observableTransactions.addAll(account.get().getSortedTransactionList());

            JavaFXUtils.runLater(() -> { // table view many not be ready, push to end of the Platform thread
                tableViewManager.restoreLayout(); // required for table view manager to work
                tableView.scrollTo(observableTransactions.size()); // scroll to the end of the table

                // formats have changed, force a full recalculation
                if (Options.getLastFormatChange() >= tableViewManager.getTimeStamp()) {
                    tableViewManager.packTable();
                }
            });
        }
    }

    void manuallyPackTable() {
        tableViewManager.packTable();
    }

    List<Transaction> getSelectedTransactions() {
        return tableView.getSelectionModel().getSelectedItems();
    }

    void deleteTransactions() {
        final List<Transaction> transactionList = tableView.getSelectionModel().getSelectedItems();

        RegisterActions.deleteTransactionAction(transactionList.toArray(new Transaction[0]));
    }

    private void duplicateTransactions() {
        final List<Transaction> transactionList = tableView.getSelectionModel().getSelectedItems();

        RegisterActions.duplicateTransaction(account.get(), transactionList);
    }

    private void handleCreateNewReminder() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final Reminder reminder = Engine.createDefaultReminder(selectedTransaction.get(), account.get());

        final Optional<Reminder> optional = RecurringEntryDialog.showAndWait(reminder);

        optional.ifPresent(engine::addReminder);
    }

    void handleJumpAction() {
        Transaction t = selectedTransaction.get();

        if (t != null) {
            if (t.getTransactionType() == TransactionType.DOUBLEENTRY) {
                final Set<Account> set = t.getAccounts();
                set.stream().filter(a -> !account.get().equals(a))
                        .forEach(account -> jump(account, t));
            } else if (t.getTransactionType() == TransactionType.SPLITENTRY) {
                final Account common = t.getCommonAccount();

                if (!account.get().equals(common)) {
                    jump(common, t);
                }
            } else if (t instanceof InvestmentTransaction) {
                final Account invest = ((InvestmentTransaction) t).getInvestmentAccount();

                if (!account.get().equals(invest)) {
                    jump(invest, t);
                }
            }
        }
    }

    private void jump(final Account account, final Transaction transaction) {
        JavaFXUtils.runAndWait(() -> {  // ensure the stage is shown before showing the transaction
            RegisterStage registerStage = RegisterStage.getRegisterStage(account);
            JavaFXUtils.runLater(() -> registerStage.show(transaction));
        });
    }

    private void handleCopyToClipboard() {
        final List<Transaction> transactionList = tableView.getSelectionModel().getSelectedItems();

        if (transactionList.size() > 0) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            final StringBuilder builder = new StringBuilder();

            for (final Transaction transaction : transactionList) {
                builder.append(transactionToExcel(transaction));
                builder.append('\n');
            }

            content.putString(builder.toString());
            clipboard.setContent(content);
        }
    }

    private String transactionToExcel(final Transaction transaction) {
        final StringBuilder builder = new StringBuilder();
        final DateTimeFormatter dateFormatter = DateUtils.getExcelDateFormatter();

        String account;

        if (transaction instanceof InvestmentTransaction) {
            account = (((InvestmentTransaction) transaction).getInvestmentAccount().getName());
        } else {
            int count = transaction.size();
            if (count > 1) {
                account = "[ " + count + " " + resources.getString("Button.Splits") + " ]";
            } else {
                Account creditAccount = transaction.getTransactionEntries().get(0).getCreditAccount();
                if (creditAccount != accountProperty().get()) {
                    account = creditAccount.getName();
                } else {
                    account = transaction.getTransactionEntries().get(0).getDebitAccount().getName();
                }
            }
        }

        // date, number, payee, memo, account, clr, amount, timestamp
        builder.append(dateFormatter.format(transaction.getLocalDate()));
        builder.append('\t');
        builder.append(transaction.getNumber());
        builder.append('\t');
        builder.append(transaction.getPayee());
        builder.append('\t');
        builder.append(transaction.getMemo());
        builder.append('\t');
        builder.append(account);
        builder.append('\t');
        builder.append(transaction.getReconciled(transaction.getCommonAccount()).toString());
        builder.append('\t');
        builder.append(transaction.getAmount(transaction.getCommonAccount()).toPlainString());

        return builder.toString();
    }

    @FXML
    protected void handleResetFilters() {

        JavaFXUtils.runLater(() -> {
            transactionAgeFilterComboBox.setValue(AgeEnum.ALL);
            reconciledStateFilterComboBox.setValue(ReconciledStateEnum.ALL);

            if (memoFilterTextField != null) {
                memoFilterTextField.setText("");
            }

            if (payeeFilterTextField != null) {
                payeeFilterTextField.setText("");
            }

            tagPane.clearSelectedTags();
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

    private class TransactionRowFactory implements Callback<TableView<Transaction>, TableRow<Transaction>> {

        @Override
        public TableRow<Transaction> call(final TableView<Transaction> param) {

            final TableRow<Transaction> row = new TableRow<>();
            final ContextMenu rowMenu = new ContextMenu();

            final Menu markedAs = new Menu(resources.getString("Menu.MarkAs.Name"));
            final MenuItem markAsClearedItem = new MenuItem(resources.getString("Menu.Cleared.Name"));
            markAsClearedItem.setOnAction(event -> RegisterActions.reconcileTransactionAction(account.get(),
                    row.getItem(), ReconciledState.CLEARED));

            final MenuItem markAsReconciledItem = new MenuItem(resources.getString("Menu.Reconciled.Name"));
            markAsReconciledItem.setOnAction(event -> RegisterActions.reconcileTransactionAction(account.get(),
                    row.getItem(), ReconciledState.RECONCILED));

            final MenuItem markAsUnreconciledItem = new MenuItem(resources.getString("Menu.Unreconciled.Name"));
            markAsUnreconciledItem.setOnAction(event -> RegisterActions.reconcileTransactionAction(account.get(),
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

            final MenuItem copyClipBoardItem = new MenuItem(resources.getString("Menu.CopyToClipboard.Name"));
            copyClipBoardItem.setOnAction(event -> handleCopyToClipboard());

            rowMenu.getItems().addAll(markedAs, new SeparatorMenuItem(), duplicateItem, jumpItem,
                    new SeparatorMenuItem(), deleteItem, new SeparatorMenuItem(), reminderItem, new SeparatorMenuItem(),
                    copyClipBoardItem);

            // only display context menu for non-null items:
            row.contextMenuProperty().bind(
                    Bindings.when(Bindings.isNotNull(row.itemProperty())).then(rowMenu).otherwise((ContextMenu) null));

            // only display the tooltip if the selection size is greater than one
            row.tooltipProperty().bind(Bindings.when(Bindings.greaterThan(selectionSize, 1))
                                               .then(selectionSummaryTooltip).otherwise((Tooltip) null));

            return row;
        }
    }

    private class MessageBusHandler implements MessageListener {

        /**
         * Limits number of refresh and packTable calls while ensuring the most recent
         * is executed.
         */
        private final ThreadPoolExecutor updateTableExecutor;

        MessageBusHandler() {
            updateTableExecutor = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.DAYS,
                    new ArrayBlockingQueue<>(1));

            updateTableExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        }

        private void refreshTable() {
            updateTableExecutor.execute(() -> {
                tableView.refresh();
                tableViewManager.packTable();
            });
        }

        @Override
        public void messagePosted(final Message event) {
            final Account acc = RegisterTableController.this.account.getValue();

            if (acc != null && event.getObject(MessageProperty.ACCOUNT).equals(acc)) {
                switch (event.getEvent()) {
                    case TRANSACTION_REMOVE:
                        final Transaction removedTransaction = event.getObject(MessageProperty.TRANSACTION);

                        // clear the selection if the transaction is currently selected
                        if (tableView.getSelectionModel().getSelectedItems().contains(removedTransaction)) {
                            JavaFXUtils.runLater(RegisterTableController.this::clearTableSelection);
                        }

                        /*
                         * push removal to the end of the application thread to ensure the table
                         * selection is cleared first to prevent an IndexOfOutBoundsException
                         */
                        JavaFXUtils.runLater(() -> observableTransactions.remove(removedTransaction));

                        // this will force the running balance to recalculate
                        refreshTable();

                        break;
                    case TRANSACTION_ADD:
                        final Transaction addedTransaction = event.getObject(MessageProperty.TRANSACTION);

                        JavaFXUtils.runLater(() -> {

                            final int index = Collections.binarySearch(observableTransactions, addedTransaction,
                                    tableView.getComparator());

                            if (index < 0) {
                                observableTransactions.add(-index - 1, addedTransaction);
                            }

                            // scroll to the new transaction
                            JavaFXUtils.runLater(() -> scrollToTransaction(addedTransaction));

                            // this will force the running balance to recalculate
                            refreshTable();
                        });

                        break;
                    default:
                }

            }
        }
    }
}
