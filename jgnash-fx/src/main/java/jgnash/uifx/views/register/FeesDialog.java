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

import java.math.BigDecimal;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Callback;

import jgnash.engine.Account;
import jgnash.engine.TransactionEntry;
import jgnash.text.CommodityFormat;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.TableViewManager;
import jgnash.util.NotNull;
import jgnash.util.ResourceUtils;

/**
 * Split Transaction entry dialog
 *
 * @author Craig Cavanaugh
 */
class FeesDialog extends Stage {

    private static final String PREF_NODE_USER_ROOT = "/jgnash/uifx/views/register/fees";

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
    private TableView<TransactionEntry> tableView;

    @FXML
    private StackPane formPane;

    @FXML
    private ResourceBundle resources;

    private FeeTransactionEntrySlipController debitController;

    private final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private TableViewManager<TransactionEntry> tableViewManager;

    private final ObservableList<TransactionEntry> transactionEntries = FXCollections.observableArrayList();

    private final SortedList<TransactionEntry> sortedList = new SortedList<>(transactionEntries);

    FeesDialog() {
        FXMLUtils.loadFXML(this, "FeesDialog.fxml", ResourceUtils.getBundle());

        setTitle(ResourceUtils.getBundle().getString("Title.InvFees"));

        setOnShowing(event -> {
            tableViewManager.restoreLayout();   // restore layout and pack after the table is visible
        });
    }

    ObjectProperty<Account> accountProperty() {
        return accountProperty;
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
        tableView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TransactionEntry>() {
            @Override
            public void changed(final ObservableValue<? extends TransactionEntry> observable, final TransactionEntry oldValue, final TransactionEntry newValue) {
                if (newValue != null) { // null can occur when the transaction entry list changes
                    modifyTransactionEntry(newValue);
                }
            }
        });

        // If the list changes, clear the selection
        transactionEntries.addListener((ListChangeListener<TransactionEntry>) c -> tableView.getSelectionModel().clearSelection());

        // repack when the list contents change and this dialog is showing
        transactionEntries.addListener((ListChangeListener<TransactionEntry>) c -> {
            if (FeesDialog.this.isShowing()) {
                tableViewManager.packTable();
            }
        });

        closeButton.setOnAction(event -> closeAction());
        deleteButton.setOnAction(event -> deleteAction());
        deleteAllButton.setOnAction(event -> deleteAllAction());
        newButton.setOnAction(event -> newAction());
    }

    private void newAction() {
        debitController.clearForm();
        tableView.getSelectionModel().clearSelection();
    }

    private void deleteAction() {
        final TransactionEntry entry = tableView.getSelectionModel().getSelectedItem();
        if (entry != null) {
            tableView.getSelectionModel().clearSelection();
            debitController.clearForm();
            transactionEntries.remove(entry);
        }
    }

    /**
     * Delete all of the transaction entries
     */
    private void deleteAllAction() {
        tableView.getSelectionModel().clearSelection();
        transactionEntries.clear();
    }

    private void modifyTransactionEntry(@NotNull final TransactionEntry transactionEntry) {
        debitController.modifyTransactionEntry(transactionEntry);
    }

    private void loadTable() {
        tableViewManager = new TableViewManager<>(tableView, PREF_NODE_USER_ROOT);
        tableViewManager.setColumnWeightFactory(getColumnWeightFactory());
        tableViewManager.setPreferenceKeyFactory(() -> accountProperty().get().getUuid());

        sortedList.comparatorProperty().bind(tableView.comparatorProperty());

        buildTable();

        tableView.setItems(sortedList);
    }

    Callback<Integer, Double> getColumnWeightFactory() {
        return param -> PREF_COLUMN_WEIGHTS[param];
    }
    
    @SuppressWarnings("unchecked")
	private void buildTable() {
        final String[] columnNames = RegisterFactory.getSplitColumnNames(accountProperty().get().getAccountType());

        final TableColumn<TransactionEntry, String> accountColumn = new TableColumn<>(columnNames[0]);
        accountColumn.setCellValueFactory(param -> new AccountNameWrapper(param.getValue()));

        final TableColumn<TransactionEntry, String> reconciledColumn = new TableColumn<>(columnNames[1]);
        reconciledColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getReconciled(accountProperty.get()).toString()));

        final TableColumn<TransactionEntry, String> memoColumn = new TableColumn<>(columnNames[2]);
        memoColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getMemo()));

        final TableColumn<TransactionEntry, BigDecimal> increaseColumn = new TableColumn<>(columnNames[3]);
        increaseColumn.setCellValueFactory(param -> new IncreaseAmountProperty(param.getValue().getAmount(accountProperty().getValue())));
        increaseColumn.setCellFactory(cell -> new TransactionEntryCommodityFormatTableCell(CommodityFormat.getShortNumberFormat(accountProperty.get().getCurrencyNode())));

        final TableColumn<TransactionEntry, BigDecimal> decreaseColumn = new TableColumn<>(columnNames[4]);
        decreaseColumn.setCellValueFactory(param -> new DecreaseAmountProperty(param.getValue().getAmount(accountProperty().getValue())));
        decreaseColumn.setCellFactory(cell -> new TransactionEntryCommodityFormatTableCell(CommodityFormat.getShortNumberFormat(accountProperty.get().getCurrencyNode())));

        final TableColumn<TransactionEntry, BigDecimal> balanceColumn = new TableColumn<>(columnNames[5]);
        balanceColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(getBalanceAt(param.getValue())));
        balanceColumn.setCellFactory(cell -> new TransactionEntryCommodityFormatTableCell(CommodityFormat.getFullNumberFormat(accountProperty.get().getCurrencyNode())));
        balanceColumn.setSortable(false);   // do not allow a sort on the balance
            
        tableView.getColumns().addAll(memoColumn, accountColumn, reconciledColumn, increaseColumn, decreaseColumn, balanceColumn);

        tableViewManager.setColumnFormatFactory(param -> {
            if (param == balanceColumn) {
                return CommodityFormat.getFullNumberFormat(accountProperty().getValue().getCurrencyNode());
            } else if (param == increaseColumn || param == decreaseColumn) {
                return CommodityFormat.getShortNumberFormat(accountProperty().getValue().getCurrencyNode());
            }

            return null;
        });
    }

    private void initForm() {
        debitController = FXMLUtils.loadFXML(o -> formPane.getChildren().addAll((Node) o),
                "FeeTransactionEntrySlip.fxml", resources);

        debitController.getAccountProperty().bind(accountProperty);
        debitController.getTransactionEntryListProperty().setValue(transactionEntries);
    }

    private BigDecimal getBalanceAt(final TransactionEntry transactionEntry) {
        BigDecimal balance = BigDecimal.ZERO;

        final Account account = accountProperty.get();

        if (account != null) {
            final int index = sortedList.indexOf(transactionEntry);

            for (int i = 0; i <= index; i++) {
                balance = balance.add(sortedList.get(i).getAmount(account));
            }
        }
        return balance;
    }

    BigDecimal getBalance() {
        if (sortedList.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return getBalanceAt(sortedList.get(sortedList.size() - 1));
    }

    private void closeAction() {
        closeButton.getScene().getWindow().hide();
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
}
