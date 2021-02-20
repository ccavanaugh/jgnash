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
package jgnash.uifx.wizard.imports;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import jgnash.convert.importat.BayesImportClassifier;
import jgnash.convert.importat.GenericImport;
import jgnash.convert.importat.ImportBank;
import jgnash.convert.importat.ImportFilter;
import jgnash.convert.importat.ImportState;
import jgnash.convert.importat.ImportTransaction;
import jgnash.convert.importat.ImportUtils;
import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionType;
import jgnash.resource.util.ResourceUtils;
import jgnash.resource.util.TextResource;
import jgnash.uifx.Options;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.BigDecimalTableCell;
import jgnash.uifx.control.ShortDateTableCell;
import jgnash.uifx.control.wizard.AbstractWizardPaneController;
import jgnash.uifx.resource.font.MaterialDesignLabel;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.util.TableViewManager;
import jgnash.util.Nullable;

/**
 * Import Wizard, imported transaction wizard.
 *
 * @author Craig Cavanaugh
 */
public class ImportPageTwoController extends AbstractWizardPaneController<ImportWizard.Settings> {

    private static final String PREF_NODE = "/jgnash/uifx/wizard/imports";

    private static final double[] PREF_COLUMN_WEIGHTS = {0, 0, 0, 50, 50, 0, 0, 0, 0, 0};

    private static final double[] MIN_COLUMN_WIDTHS = {0, 0, 0, 0, 0, 90, 90, 90, 0, 0};

    private final SimpleBooleanProperty valid = new SimpleBooleanProperty(false);

    private final NumberFormat numberFormat = NumberFormat.getNumberInstance();

    private static final Account NOP_EXPENSE_ACCOUNT = new Account();

    @FXML
    private TextFlow textFlow;

    @FXML
    private TableView<ImportTransaction> tableView;

    @FXML
    private Button deleteButton;

    @FXML
    private ResourceBundle resources;

    private TableViewManager<ImportTransaction> tableViewManager;

    private TableColumn<ImportTransaction, Account> incomeAccountColumn;

    private TableColumn<ImportTransaction, Account> expenseAccountColumn;

    private TableColumn<ImportTransaction, String> typeColumn;

    private Account baseAccount = null;

    private Account lastAccount;

    private Account lastGainsAccount;

    private Account lastFeesAccount;

    static {
        NOP_EXPENSE_ACCOUNT.setName("…");   // universal N/A for tabular data
    }

    @FXML
    private void initialize() {
        textFlow.getChildren().addAll(new Text(TextResource.getString("ImportTwo.txt")));

        deleteButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());

        tableView.setTableMenuButtonVisible(false);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setEditable(true);

        tableView.getItems().addListener((ListChangeListener<ImportTransaction>) c ->
                valid.set(tableView.getItems().size() > 0));

        buildTableView();

        tableViewManager = new TableViewManager<>(tableView, PREF_NODE);
        tableViewManager.setColumnWeightFactory(column -> PREF_COLUMN_WEIGHTS[column]);
        tableViewManager.setMinimumColumnWidthFactory(column -> MIN_COLUMN_WIDTHS[column]);

        updateDescriptor();
    }

    private void buildTableView() {

        final TableColumn<ImportTransaction, ImportState> stateColumn = new TableColumn<>();
        stateColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getState()));

        stateColumn.setCellFactory(param -> {
            TableCell<ImportTransaction, ImportState> cell =
                    new ImportStateTableCell();

            cell.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                if (event.getClickCount() > 1) {
                    final ImportTransaction t = tableView.getItems().get(((TableCell<?, ?>) event.getSource())
                            .getTableRow().getIndex());

                    switch (t.getState()) {
                        case EQUAL:
                            t.setState(ImportState.NOT_EQUAL);
                            break;
                        case NOT_EQUAL:
                            t.setState(ImportState.EQUAL);
                            break;
                        case NEW:
                            t.setState(ImportState.IGNORE);
                            break;
                        case IGNORE:
                            t.setState(ImportState.NEW);
                            break;
                    }

                    JavaFXUtils.runLater(tableView::refresh);
                }
            });
            return cell;
        });

        tableView.getColumns().add(stateColumn);

        final TableColumn<ImportTransaction, LocalDate> dateColumn =
                new TableColumn<>(resources.getString("Column.Date"));
        dateColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getDatePosted()));
        dateColumn.setCellFactory(param -> new ShortDateTableCell<>());
        tableView.getColumns().add(dateColumn);

        final TableColumn<ImportTransaction, String> numberColumn =
                new TableColumn<>(resources.getString("Column.Num"));
        numberColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getCheckNumber()));
        tableView.getColumns().add(numberColumn);

        final TableColumn<ImportTransaction, String> payeeColumn =
                new TableColumn<>(resources.getString("Column.Payee"));
        payeeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getPayee()));
        tableView.getColumns().add(payeeColumn);

        final TableColumn<ImportTransaction, String> memoColumn =
                new TableColumn<>(resources.getString("Column.Memo"));
        memoColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getMemo()));
        tableView.getColumns().add(memoColumn);

        final TableColumn<ImportTransaction, Account> accountColumn =
                new TableColumn<>(resources.getString("Column.Account"));

        accountColumn.setCellValueFactory(param -> {
            if (param.getValue() != null && param.getValue().getAccount() != null) {
                return new SimpleObjectProperty<>(param.getValue().getAccount());
            }
            return null;
        });
        accountColumn.setCellFactory(param -> new AccountComboBoxTableCell<>());
        accountColumn.setEditable(true);

        accountColumn.setOnEditCommit(event -> {
            event.getTableView().getItems().get(event.getTablePosition().getRow()).setAccount(event.getNewValue());
            lastAccount = event.getNewValue();
            JavaFXUtils.runLater(tableViewManager::packTable);
        });
        tableView.getColumns().add(accountColumn);


        incomeAccountColumn = new TableColumn<>(resources.getString("Column.Income"));
        incomeAccountColumn.setCellValueFactory(param -> {
            if (param.getValue() != null && param.getValue().getGainsAccount() != null) {
                return new SimpleObjectProperty<>(param.getValue().getGainsAccount());
            }
            return null;
        });
        incomeAccountColumn.setCellFactory(param -> new IncomeAccountComboBoxTableCell<>());
        incomeAccountColumn.setEditable(true);
        incomeAccountColumn.setOnEditCommit(event -> {
            event.getTableView().getItems().get(event.getTablePosition().getRow()).setGainsAccount(event.getNewValue());
            lastGainsAccount = event.getNewValue();
            JavaFXUtils.runLater(tableViewManager::packTable);
        });
        tableView.getColumns().add(incomeAccountColumn);

        expenseAccountColumn = new TableColumn<>(resources.getString("Column.Expense"));
        expenseAccountColumn.setCellValueFactory(param -> {
            if (param.getValue() != null && param.getValue().getFeesAccount() != null) {
                if (param.getValue().getFees().compareTo(BigDecimal.ZERO) != 0) {
                    return new SimpleObjectProperty<>(param.getValue().getFeesAccount());
                }
                
				return new SimpleObjectProperty<>(NOP_EXPENSE_ACCOUNT);  // nop account
            }
            return null;
        });
        expenseAccountColumn.setCellFactory(param -> new ExpenseAccountComboBoxTableCell<>());
        expenseAccountColumn.setEditable(true);
        expenseAccountColumn.setOnEditCommit(event -> {
            event.getTableView().getItems().get(event.getTablePosition().getRow()).setFeesAccount(event.getNewValue());
            JavaFXUtils.runLater(tableViewManager::packTable);
        });
        tableView.getColumns().add(expenseAccountColumn);


        final TableColumn<ImportTransaction, BigDecimal> amountColumn =
                new TableColumn<>(resources.getString("Column.Amount"));

        amountColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getAmount()));
        amountColumn.setCellFactory(param -> new BigDecimalTableCell<>(numberFormat));

        amountColumn.setCellFactory(param -> {
            final TableCell<ImportTransaction, BigDecimal> cell = new BigDecimalTableCell<>(numberFormat);

            // add tool tip
            cell.indexProperty().addListener((observable, oldValue, newValue) -> {
                final int index = newValue.intValue();

                if (index >= 0 && index < tableView.itemsProperty().get().size()) {
                    cell.setTooltip(new Tooltip(tableView.itemsProperty().get().get(index).getToolTip()));
                }
            });

            return cell;
        });


        tableView.getColumns().add(amountColumn);


        typeColumn = new TableColumn<>(resources.getString("Column.Type"));
        typeColumn.setCellValueFactory(param -> {

            TransactionType transactionType = TransactionType.SINGLENTRY;

            if (param.getValue().isInvestmentTransaction()) {
                transactionType = param.getValue().getTransactionType();
            } else if (!param.getValue().getAccount().equals(baseAccount)) {
                transactionType = TransactionType.DOUBLEENTRY;
            }

            return new SimpleStringProperty(transactionType.toString());
        });
        tableView.getColumns().add(typeColumn);
    }

    @Override
    public void putSettings(final Map<ImportWizard.Settings, Object> map) {
        map.put(ImportWizard.Settings.TRANSACTIONS, new ArrayList<>(tableView.getItems()));
    }

    @Override
    public void getSettings(final Map<ImportWizard.Settings, Object> map) {

        @SuppressWarnings("unchecked")
        final ImportBank<ImportTransaction> bank = (ImportBank<ImportTransaction>) map.get(ImportWizard.Settings.BANK);

        if (bank != null) {
            final List<ImportTransaction> list = bank.getTransactions();

            baseAccount = (Account) map.get(ImportWizard.Settings.ACCOUNT);

            final CurrencyNode currencyNode = baseAccount.getCurrencyNode();

            // rescale for consistency
            numberFormat.setMinimumFractionDigits(currencyNode.getScale());
            numberFormat.setMaximumFractionDigits(currencyNode.getScale());

            // List of enabled import filters
            final List<ImportFilter> importFilterList = ImportFilter.getEnabledImportFilters();

            // set to sane account assuming it's going to be a single entry
            for (final ImportTransaction t : list) {

                // Process transactions with the import filter
                for (final ImportFilter importFilter : importFilterList) {
                    importFilter.acceptTransaction(t);  // pass the import transaction for manipulation by the script
                    t.setMemo(importFilter.processMemo(t.getMemo()));
                    t.setPayee(importFilter.processPayee(t.getPayee()));
                }

                if (t.getTransactionType() != TransactionType.REINVESTDIV) {
                    t.setAccount(baseAccount);
                }

                if (t.isInvestmentTransaction()) {
                    switch (t.getTransactionType()) {
                        case BUYSHARE:
                            t.setFeesAccount(baseAccount);
                            break;
                        case SELLSHARE:
                        case REINVESTDIV:
                            t.setFeesAccount(baseAccount);
                            t.setGainsAccount(baseAccount);
                            break;
                        case DIVIDEND:
                            t.setGainsAccount(baseAccount);
                            break;
                        default:
                    }
                }

                t.setState(ImportState.NEW);  // force reset
            }

            incomeAccountColumn.setVisible(bank.isInvestmentAccount());
            expenseAccountColumn.setVisible(bank.isInvestmentAccount());
            typeColumn.setVisible(bank.isInvestmentAccount());

            // match up any pre-existing transactions
            GenericImport.matchTransactions(list, baseAccount);

            // classify the transactions
            if (Options.globalBayesProperty().get()) {
                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                Objects.requireNonNull(engine);

                final List<Transaction> transactions = engine.getTransactions();
                transactions.sort(null);

                BayesImportClassifier.classifyTransactions(list, transactions, baseAccount);
            } else {
                BayesImportClassifier.classifyTransactions(list, baseAccount.getSortedTransactionList(), baseAccount);
            }

            // override the classifier if an account has been specified already
            for (final ImportTransaction importTransaction : list) {
                final Account account = ImportUtils.matchAccount(importTransaction);

                if (account != null) {
                    importTransaction.setAccount(account);
                }
            }

            tableView.getItems().setAll(list);
            FXCollections.sort(tableView.getItems());

            tableViewManager.restoreLayout();
        }

        JavaFXUtils.runLater(tableViewManager::packTable);

        updateDescriptor();
    }

    @Override
    public boolean isPaneValid() {
        return valid.getValue();
    }

    @Override
    public String toString() {
        return "2. " + ResourceUtils.getString("Title.ModImportTrans");
    }

    @FXML
    private void handleDeleteAction() {
        tableView.getItems().removeAll(tableView.getSelectionModel().getSelectedItems());
    }

    private static class ImportStateTableCell extends TableCell<ImportTransaction, ImportState> {
        @Override
        public void updateItem(final ImportState item, final boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (item != null) {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setText(null);

                switch (item) {
                    case IGNORE:
                        setGraphic(new StackPane(new MaterialDesignLabel(MaterialDesignLabel.MDIcon.MINUS_CIRCLE)));
                        break;
                    case NEW:
                    case NOT_EQUAL:
                        setGraphic(new StackPane(new MaterialDesignLabel(MaterialDesignLabel.MDIcon.PLUS_CIRCLE)));
                        break;
                    case EQUAL:
                        setGraphic(new StackPane(new Label("=")));
                        break;
                }
            }
        }
    }

    class AccountComboBoxTableCell<S> extends TableCell<S, Account> {

        private final AccountComboBox comboBox;

        private boolean firstPassEdit = false;

        AccountComboBoxTableCell() {
            this.getStyleClass().add("combo-box-table-cell");

            comboBox = new AccountComboBox();

            comboBox.setMaxWidth(Double.MAX_VALUE);

            comboBox.setOnHidden(event -> {
                if (isEditing()) {
                    firstPassEdit = true;
                    lastAccount = comboBox.getValue();
                    commitEdit(comboBox.getValue());
                }
            });
        }

        @Override
        public void startEdit() {
            final TableRow<?> row = getTableRow();

            if (row != null) {
                final ImportTransaction importTransaction = (ImportTransaction) row.getItem();

                if (!importTransaction.isInvestmentTransaction()) {
                    editableProperty().setValue(true);
                } else {    // reinvested dividends do not have a cash account
                    editableProperty().setValue(importTransaction.getTransactionType() != TransactionType.REINVESTDIV);
                }
            }

            if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
                return;
            }

            if (!firstPassEdit && lastAccount != null) {
                comboBox.getSelectionModel().select(lastAccount);
            } else {
                comboBox.getSelectionModel().select(getItem());
            }

            super.startEdit();
            setText(null);
            setGraphic(comboBox);
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();

            if (getItem() != null) {
                setText(getItem().getName());
            }

            setGraphic(null);
        }

        @Override
        public void updateItem(@Nullable final Account item, final boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    comboBox.getSelectionModel().select(getItem());
                    setText(null);
                    setGraphic(comboBox);
                } else {
                    setText(getItem().getName());
                    setGraphic(null);
                }
            }
        }
    }

    class IncomeAccountComboBoxTableCell<S extends ImportTransaction> extends TableCell<S, Account> {

        private final AccountComboBox comboBox;

        private boolean firstPassEdit = false;

        IncomeAccountComboBoxTableCell() {
            this.getStyleClass().add("combo-box-table-cell");

            comboBox = new AccountComboBox();

            comboBox.setPredicate(AccountComboBox.getDefaultPredicate()
                    .and(account -> account.getAccountType() == AccountType.INCOME || account == baseAccount));

            comboBox.setMaxWidth(Double.MAX_VALUE);

            comboBox.setOnHidden(event -> {
                if (isEditing()) {
                    firstPassEdit = true;
                    lastGainsAccount = comboBox.getValue();
                    commitEdit(comboBox.getValue());
                }
            });
        }

        @Override
        public void startEdit() {
            final TableRow<?> row = getTableRow();

            if (row != null) {
                final ImportTransaction importTransaction = (ImportTransaction) row.getItem();

                editableProperty().setValue(importTransaction.getTransactionType() == TransactionType.SELLSHARE
                        || importTransaction.getTransactionType() == TransactionType.DIVIDEND
                        || importTransaction.getTransactionType() == TransactionType.REINVESTDIV);
            }

            if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
                return;
            }

            if (!firstPassEdit && lastGainsAccount != null) {
                comboBox.getSelectionModel().select(lastGainsAccount);
            } else {
                comboBox.getSelectionModel().select(getItem());
            }

            super.startEdit();
            setText(null);
            setGraphic(comboBox);
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();

            if (getItem() != null) {
                setText(getItem().getName());
            }

            setGraphic(null);
        }

        @Override
        public void updateItem(@Nullable final Account item, final boolean empty) {
            super.updateItem(item, empty);

            TableRow<?> row = getTableRow();

            if (empty || item == null || row == null) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    comboBox.getSelectionModel().select(getItem());
                    setText(null);
                    setGraphic(comboBox);
                } else {
                    setText(getItem().getName());
                    setGraphic(null);
                }
            }
        }
    }

    class ExpenseAccountComboBoxTableCell<S extends ImportTransaction> extends TableCell<S, Account> {

        private final AccountComboBox comboBox;

        private boolean firstPassEdit = false;

        ExpenseAccountComboBoxTableCell() {
            this.getStyleClass().add("combo-box-table-cell");

            comboBox = new AccountComboBox();
            comboBox.getUnfilteredItems().addAll(NOP_EXPENSE_ACCOUNT);
            comboBox.setPredicate(AccountComboBox.getDefaultPredicate()
                    .and(account -> account.getAccountType() == AccountType.EXPENSE || account == baseAccount
                            || account == NOP_EXPENSE_ACCOUNT));

            comboBox.setMaxWidth(Double.MAX_VALUE);

            comboBox.setOnHidden(event -> {
                if (isEditing()) {
                    firstPassEdit = true;
                    lastFeesAccount = comboBox.getValue();
                    commitEdit(comboBox.getValue());
                }
            });
        }

        @Override
        public void startEdit() {
            final TableRow<?> row = getTableRow();

            if (row != null) {
                final ImportTransaction importTransaction = (ImportTransaction) row.getItem();

                editableProperty().setValue(importTransaction.getFees().compareTo(BigDecimal.ZERO) != 0
                        && (importTransaction.getTransactionType() == TransactionType.SELLSHARE
                        || importTransaction.getTransactionType() == TransactionType.BUYSHARE
                        || importTransaction.getTransactionType() == TransactionType.REINVESTDIV));

            }

            if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
                return;
            }

            if (!firstPassEdit && lastFeesAccount != null) {
                comboBox.getSelectionModel().select(lastFeesAccount);
            } else {
                comboBox.getSelectionModel().select(getItem());
            }

            super.startEdit();
            setText(null);
            setGraphic(comboBox);
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();

            if (getItem() != null) {
                setText(getItem().getName());
            }

            setGraphic(null);
        }

        @Override
        public void updateItem(@Nullable final Account item, final boolean empty) {
            super.updateItem(item, empty);

            TableRow<?> row = getTableRow();

            if (empty || item == null || row == null) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    comboBox.getSelectionModel().select(getItem());
                    setText(null);
                    setGraphic(comboBox);
                } else {
                    setText(getItem().getName());
                    setGraphic(null);
                }
            }
        }
    }
}
