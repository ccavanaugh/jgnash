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
package jgnash.uifx.views.register.reconcile;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import jgnash.engine.Account;
import jgnash.engine.MathConstants;
import jgnash.engine.RecTransaction;
import jgnash.engine.ReconcileManager;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.text.CommodityFormat;
import jgnash.time.DateUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.BigDecimalTableCell;
import jgnash.uifx.control.ShortDateTableCell;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.TableViewManager;
import jgnash.uifx.views.AccountBalanceDisplayManager;
import jgnash.uifx.views.register.RegisterFactory;
import jgnash.util.NotNull;
import jgnash.util.Nullable;

/**
 * Account reconcile dialog.
 *
 * @author Craig Cavanaugh
 */
public class ReconcileDialogController implements MessageListener {

    private static final String INCREASE_KEY = "increase";

    private static final String DECREASE_KEY = "decrease";

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private Button finishButton;

    @FXML
    private ResourceBundle resources;

    @FXML
    private TitledPane decreaseTitledPane;

    @FXML
    private TitledPane increaseTitledPane;

    @FXML
    private Label increaseTotalLabel;

    @FXML
    private Label decreaseTotalLabel;

    @FXML
    private TableView<RecTransaction> increaseTableView;

    @FXML
    private TableView<RecTransaction> decreaseTableView;

    @FXML
    private Label openingBalanceLabel;

    @FXML
    private Label targetBalanceLabel;

    @FXML
    private Label reconciledBalanceLabel;

    @FXML
    private Label differenceLabel;

    private static final double[] PREF_COLUMN_WEIGHTS = {0, 0, 0, 100, 0};

    private Account account;

    private LocalDate closingDate;

    private BigDecimal openingBalance;

    private BigDecimal endingBalance;

    private final ObservableList<RecTransaction> transactions = FXCollections.observableArrayList();

    private final FilteredList<RecTransaction> increaseList = new FilteredList<>(transactions);

    private final FilteredList<RecTransaction> decreaseList = new FilteredList<>(transactions);

    private TableViewManager<RecTransaction> increaseTableViewManager;

    private TableViewManager<RecTransaction> decreaseTableViewManager;

    private NumberFormat numberFormat;

    private final SimpleBooleanProperty reconciled = new SimpleBooleanProperty(false);

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    private static final String PREF_NODE = "/jgnash/uifx/views/register/reconcile";

    @FXML
    private void initialize() {
        parent.addListener((observable, oldValue, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().get().addEventHandler(WindowEvent.WINDOW_SHOWN,
                        event -> Platform.runLater(() -> {
                            increaseTableViewManager.restoreLayout();
                            decreaseTableViewManager.restoreLayout();
                        }));
            }
        });

        // Selection listener that toggles the reconciled state
        class ToggleStateChangeListener implements ChangeListener<RecTransaction> {

            private final TableView<RecTransaction> tableView;

            private ToggleStateChangeListener(final TableView<RecTransaction> tableView) {
                this.tableView = tableView;
            }

            @Override
            public void changed(final ObservableValue<? extends RecTransaction> observable,
                                final RecTransaction oldValue, final RecTransaction newValue) {
                if (newValue != null) {
                    if (newValue.getReconciledState() == ReconciledState.RECONCILED) {
                        newValue.setReconciledState(ReconciledState.NOT_RECONCILED);
                    } else {
                        newValue.setReconciledState(ReconciledState.RECONCILED);
                    }
                    tableView.refresh();
                    Platform.runLater(() -> tableView.getSelectionModel().clearSelection());
                    updateCalculatedValues();
                }
            }
        }

        // toggle the selection state
        increaseTableView.getSelectionModel().selectedItemProperty()
                .addListener(new ToggleStateChangeListener(increaseTableView));

        // toggle the selection state
        decreaseTableView.getSelectionModel().selectedItemProperty()
                .addListener(new ToggleStateChangeListener(decreaseTableView));

        finishButton.disableProperty().bind(reconciled.not());
    }

    private Callback<Integer, Double> getColumnWeightFactory() {
        return param -> PREF_COLUMN_WEIGHTS[param];
    }

    void initialize(final Account account, final LocalDate closingDate, final BigDecimal openingBalance,
                    final BigDecimal endingBalance) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(closingDate);
        Objects.requireNonNull(openingBalance);
        Objects.requireNonNull(endingBalance);

        this.account = account;
        this.closingDate = closingDate;
        this.endingBalance = endingBalance;
        this.openingBalance = openingBalance;

        numberFormat = CommodityFormat.getShortNumberFormat(account.getCurrencyNode());

        final String[] columnNames = RegisterFactory.getCreditDebitTabNames(account.getAccountType());

        increaseTitledPane.setText(columnNames[0]);
        decreaseTitledPane.setText(columnNames[1]);

        increaseList.setPredicate(recTransaction -> recTransaction.getAmount(account).signum() >= 0);
        decreaseList.setPredicate(recTransaction -> recTransaction.getAmount(account).signum() < 0);

        openingBalanceLabel.setText(numberFormat.format(openingBalance));
        targetBalanceLabel.setText(numberFormat.format(endingBalance));

        loadTables();

        MessageBus.getInstance().registerListener(this, MessageChannel.TRANSACTION);
    }

    private void loadTables() {
        increaseTableViewManager = new TableViewManager<>(increaseTableView, PREF_NODE);
        increaseTableViewManager.setColumnWeightFactory(getColumnWeightFactory());
        increaseTableViewManager.setPreferenceKeyFactory(() -> INCREASE_KEY);

        decreaseTableViewManager = new TableViewManager<>(decreaseTableView, PREF_NODE);
        decreaseTableViewManager.setColumnWeightFactory(getColumnWeightFactory());
        decreaseTableViewManager.setPreferenceKeyFactory(() -> DECREASE_KEY);

        transactions.addAll(account.getSortedTransactionList().stream().filter(this::reconcilable)
                .map(transaction -> new RecTransaction(transaction, transaction.getReconciled(account)))
                .collect(Collectors.toList()));

        configureTableView(increaseTableView, increaseTableViewManager);
        configureTableView(decreaseTableView, decreaseTableViewManager);

        increaseTableView.setItems(increaseList);
        decreaseTableView.setItems(decreaseList);

        updateCalculatedValues();
    }

    @FXML
    private void handleCloseAction() {
        MessageBus.getInstance().unregisterListener(this, MessageChannel.TRANSACTION);
        ((Stage) parent.get().getWindow()).close();
    }

    @FXML
    private void handleDecreaseSelectAllAction() {
        setReconciledState(decreaseList, ReconciledState.RECONCILED, decreaseTableView);
    }

    @FXML
    private void handleDecreaseClearAllAction() {
        setReconciledState(decreaseList, ReconciledState.NOT_RECONCILED, decreaseTableView);
    }

    @FXML
    private void handleIncreaseSelectAllAction() {
        setReconciledState(increaseList, ReconciledState.RECONCILED, increaseTableView);
    }

    @FXML
    private void handleIncreaseClearAllAction() {
        setReconciledState(increaseList, ReconciledState.NOT_RECONCILED, increaseTableView);
    }

    @FXML
    private void handleFinishLaterAction() {
        final Task<Void> commitTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage(resources.getString("Message.PleaseWait"));
                updateProgress(-1, Long.MAX_VALUE);

                ReconcileManager.reconcileTransactions(account, transactions, ReconciledState.CLEARED);
                return null;
            }
        };

        new Thread(commitTask).start();

        StaticUIMethods.displayTaskProgress(commitTask);

        handleCloseAction();
    }

    @FXML
    private void handleFinishAction() {

        final Task<Void> commitTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage(resources.getString("Message.PleaseWait"));
                updateProgress(-1, Long.MAX_VALUE);

                ReconcileManager.reconcileTransactions(account, transactions, ReconciledState.RECONCILED);
                ReconcileManager.setAccountDateAttribute(account, Account.RECONCILE_LAST_SUCCESS_DATE, closingDate);
                return null;
            }
        };

        new Thread(commitTask).start();

        StaticUIMethods.displayTaskProgress(commitTask);

        handleCloseAction();
    }

    private void setReconciledState(final List<RecTransaction> transactionList, final ReconciledState reconciledState,
                                    final TableView<RecTransaction> tableView) {
        readWriteLock.readLock().lock();
        try {
            for (final RecTransaction recTransaction : transactionList) {
                recTransaction.setReconciledState(reconciledState);
            }
            tableView.refresh();
            updateCalculatedValues();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    private void updateCalculatedValues() {
        new Thread(() -> {

            final BigDecimal increaseAmount = getReconciledTotal(increaseList);
            final BigDecimal decreaseAmount = getReconciledTotal(decreaseList);

            Platform.runLater(() -> increaseTotalLabel
                    .setText(numberFormat.format(increaseAmount)));

            Platform.runLater(() -> decreaseTotalLabel
                    .setText(numberFormat.format(decreaseAmount)));

            final BigDecimal reconciledBalance = increaseAmount.add(decreaseAmount).add(openingBalance);

            Platform.runLater(() -> reconciledBalanceLabel
                    .setText(numberFormat.format(reconciledBalance)));

            // need to round of the values for difference to work (investment accounts)
            final int scale = account.getCurrencyNode().getScale();

            final BigDecimal difference = endingBalance.subtract(reconciledBalance).abs()
                    .setScale(scale, MathConstants.roundingMode);

            Platform.runLater(() -> differenceLabel
                    .setText(numberFormat.format(difference)));

            reconciled.set(difference.compareTo(BigDecimal.ZERO) == 0);
        }).start();
    }

    private BigDecimal getReconciledTotal(final List<RecTransaction> list) {
        BigDecimal sum = BigDecimal.ZERO;

        readWriteLock.readLock().lock();

        try {
            for (final RecTransaction t : list) {
                if (t.getReconciledState() != ReconciledState.NOT_RECONCILED) {
                    sum = sum.add(t.getAmount(account));
                }
            }
        } finally {
            readWriteLock.readLock().unlock();
        }

        return AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), sum);
    }

    private void configureTableView(final TableView<RecTransaction> tableView, final TableViewManager<RecTransaction> tableViewManager) {
        tableView.setTableMenuButtonVisible(false);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        final TableColumn<RecTransaction, String> reconciledColumn = new TableColumn<>(resources.getString("Column.Clr"));
        reconciledColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getReconciledState().toString()));
        tableView.getColumns().add(reconciledColumn);

        final TableColumn<RecTransaction, LocalDate> dateColumn = new TableColumn<>(resources.getString("Column.Date"));
        dateColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getDate()));
        dateColumn.setCellFactory(param -> new ShortDateTableCell<>());
        tableView.getColumns().add(dateColumn);

        final TableColumn<RecTransaction, String> numberColumn = new TableColumn<>(resources.getString("Column.Num"));
        numberColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getNumber()));
        tableView.getColumns().add(numberColumn);

        final TableColumn<RecTransaction, String> payeeColumn = new TableColumn<>(resources.getString("Column.Payee"));
        payeeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getPayee()));
        tableView.getColumns().add(payeeColumn);

        final TableColumn<RecTransaction, BigDecimal> amountColumn =
                new TableColumn<>(resources.getString("Column.Amount"));
        amountColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(AccountBalanceDisplayManager.
                convertToSelectedBalanceMode(account.getAccountType(), param.getValue().getAmount(account))));
        amountColumn.setCellFactory(param -> new BigDecimalTableCell<>(
                CommodityFormat.getShortNumberFormat(account.getCurrencyNode())));

        tableView.getColumns().add(amountColumn);

        tableViewManager.setColumnFormatFactory(param -> {
            if (param == amountColumn && account != null) {
                return CommodityFormat.getShortNumberFormat(account.getCurrencyNode());
            }
            return null;
        });
    }

    private boolean reconcilable(final Transaction t) {
        return DateUtils.before(t.getLocalDate(), closingDate) && t.getReconciled(account) != ReconciledState.RECONCILED;
    }

    @Nullable
    private synchronized RecTransaction findTransaction(@NotNull final Transaction t) {
        readWriteLock.readLock().lock();

        try {
            for (final RecTransaction tran : transactions) {
                if (tran.getTransaction() == t) {
                    return tran;
                }
            }
            return null;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void messagePosted(final Message message) {
        if (account != null && account.equals(message.getObject(MessageProperty.ACCOUNT))) {
            final Transaction transaction = message.getObject(MessageProperty.TRANSACTION);

            if (transaction != null) {
                switch (message.getEvent()) {
                    case TRANSACTION_REMOVE:
                        final RecTransaction trans = findTransaction(transaction);

                        if (trans != null) {
                            readWriteLock.writeLock().lock();
                            try {
                                transactions.removeAll(trans);
                                updateCalculatedValues();
                            } finally {
                                readWriteLock.writeLock().unlock();
                            }
                        }
                        break;
                    case TRANSACTION_ADD:
                        if (reconcilable(transaction)) {
                            readWriteLock.writeLock().lock();
                            try {
                                transactions.add(new RecTransaction(transaction, transaction.getReconciled(account)));
                                FXCollections.sort(transactions);
                                updateCalculatedValues();
                            } finally {
                                readWriteLock.writeLock().unlock();
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
