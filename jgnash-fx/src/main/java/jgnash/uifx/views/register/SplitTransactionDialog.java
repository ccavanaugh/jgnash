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

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

import jgnash.engine.Account;
import jgnash.engine.TransactionEntry;
import jgnash.text.CommodityFormat;
import jgnash.uifx.MainApplication;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.util.TableViewManager;
import jgnash.util.ResourceUtils;

/**
 * Split Transaction entry dialog
 *
 * @author Craig Cavanaugh
 */
public class SplitTransactionDialog extends Stage implements Initializable {

    private static final String PREF_NODE_USER_ROOT = "/jgnash/uifx/views/register/splits";

    private static final double[] PREF_COLUMN_WEIGHTS = {50, 0, 50, 0, 0, 0};

    @FXML
    private Button newButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button deleteAllButton;

    @FXML
    private Button okButton;

    @FXML
    private Button cancelButton;

    @FXML
    private TableView<TransactionEntry> tableView;

    @FXML
    private TabPane tabPane;

    Tab creditTab;

    Tab debitTab;

    private ResourceBundle resources;

    final private ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    protected TableViewManager<TransactionEntry> tableViewManager;

    final protected ObservableList<TransactionEntry> observableTransactionEntries = FXCollections.observableArrayList();

    final protected SortedList<TransactionEntry> sortedList = new SortedList<>(observableTransactionEntries);

    public SplitTransactionDialog() {
        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SplitTransactionDialog.fxml"), ResourceUtils.getBundle());
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (final IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
        }

        initOwner(MainApplication.getPrimaryStage());
        initStyle(StageStyle.DECORATED);
        initModality(Modality.APPLICATION_MODAL);
        setTitle(ResourceUtils.getBundle().getString("Title.SpitTran"));

        StageUtils.addBoundsListener(this, SplitTransactionDialog.class);
    }

    public ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    public ObservableList<TransactionEntry> getTransactionEntries() {
        return observableTransactionEntries;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.resources = resources;

        getAccountProperty().addListener((observable, oldValue, newValue) -> {
            initTabs();
            loadTable();
        });

        tableView.setTableMenuButtonVisible(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        okButton.setOnAction(event -> okAction());
        cancelButton.setOnAction(event -> cancelAction());
    }

    private void loadTable() {
        tableViewManager = new TableViewManager<>(tableView, PREF_NODE_USER_ROOT);
        tableViewManager.setColumnWeightFactory(getColumnWeightFactory());
        tableViewManager.setPreferenceKeyFactory(() -> getAccountProperty().get().getUuid());

        sortedList.comparatorProperty().bind(tableView.comparatorProperty());

        buildTable();

        tableView.setItems(sortedList);
        tableViewManager.restoreLayout();   // required to table view manager is to work
    }

    Callback<Integer, Double> getColumnWeightFactory() {
        return param -> PREF_COLUMN_WEIGHTS[param];
    }

    private void buildTable() {
        final String[] columnNames = RegisterFactory.getSplitColumnNames(getAccountProperty().get().getAccountType());

        final TableColumn<TransactionEntry, String> accountColumn = new TableColumn<>(columnNames[0]);
        accountColumn.setCellValueFactory(param -> new AccountNameWrapper(param.getValue()));

        final TableColumn<TransactionEntry, String> reconciledColumn = new TableColumn<>(columnNames[1]);
        reconciledColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getReconciled(accountProperty.get()).toString()));

        final TableColumn<TransactionEntry, String> memoColumn = new TableColumn<>(columnNames[2]);
        memoColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getMemo()));

        final TableColumn<TransactionEntry, BigDecimal> increaseColumn = new TableColumn<>(columnNames[3]);
        increaseColumn.setCellValueFactory(param -> new IncreaseAmountProperty(param.getValue().getAmount(getAccountProperty().getValue())));
        increaseColumn.setCellFactory(cell -> new TransactionEntryCommodityFormatTableCell(CommodityFormat.getShortNumberFormat(accountProperty.get().getCurrencyNode())));

        final TableColumn<TransactionEntry, BigDecimal> decreaseColumn = new TableColumn<>(columnNames[4]);
        decreaseColumn.setCellValueFactory(param -> new DecreaseAmountProperty(param.getValue().getAmount(getAccountProperty().getValue())));
        decreaseColumn.setCellFactory(cell -> new TransactionEntryCommodityFormatTableCell(CommodityFormat.getShortNumberFormat(accountProperty.get().getCurrencyNode())));

        final TableColumn<TransactionEntry, BigDecimal> balanceColumn = new TableColumn<>(columnNames[5]);
        balanceColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(getBalanceAt(param.getValue())));
        balanceColumn.setCellFactory(cell -> new TransactionEntryCommodityFormatTableCell(CommodityFormat.getFullNumberFormat(accountProperty.get().getCurrencyNode())));
        balanceColumn.setSortable(false);   // do not allow a sort on the balance

        tableView.getColumns().addAll(accountColumn, reconciledColumn, memoColumn, increaseColumn, decreaseColumn, balanceColumn);
    }

    private void initTabs() {
        final String[] tabNames = RegisterFactory.getCreditDebitTabNames(getAccountProperty().get().getAccountType());

        creditTab = new Tab(tabNames[0]);

        SplitTransactionPaneController creditController = FXMLUtils.loadFXML(o -> {
            creditTab.setContent((Node) o);
        }, "SplitTransactionPane.fxml", resources);

        creditController.setPanelType(PanelType.INCREASE);
        creditController.getAccountProperty().setValue(getAccountProperty().getValue());

        debitTab = new Tab(tabNames[1]);

        SplitTransactionPaneController debitController = FXMLUtils.loadFXML(o -> {
            debitTab.setContent((Node) o);
        }, "SplitTransactionPane.fxml", resources);

        debitController.setPanelType(PanelType.DECREASE);
        debitController.getAccountProperty().setValue(getAccountProperty().getValue());

        tabPane.getTabs().addAll(creditTab, debitTab);
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

    private void okAction() {
        ((Stage) okButton.getScene().getWindow()).close();
    }

    private void cancelAction() {
        ((Stage) cancelButton.getScene().getWindow()).close();
    }

    private class AccountNameWrapper extends SimpleStringProperty {
        AccountNameWrapper(final TransactionEntry t) {
            super();

            final Account creditAccount = t.getCreditAccount();

            if (creditAccount != getAccountProperty().get()) {
                setValue(creditAccount.getName());
            } else {
                setValue(t.getDebitAccount().getName());
            }
        }
    }
}
