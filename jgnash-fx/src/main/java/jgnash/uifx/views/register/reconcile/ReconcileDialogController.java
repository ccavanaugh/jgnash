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
package jgnash.uifx.views.register.reconcile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.text.CommodityFormat;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.views.AccountBalanceDisplayManager;
import jgnash.uifx.views.register.RegisterFactory;

/**
 * Account reconcile dialog.
 *
 * @author Craig Cavanaugh
 */
public class ReconcileDialogController implements MessageListener {

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

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

    private Account account;

    @FXML
    private void initialize() {
        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT, MessageChannel.SYSTEM);

        configureTableView(increaseTableView);
        configureTableView(decreaseTableView);
    }

    void initialize(final Account account, final LocalDate closingDate, final BigDecimal openingBalance,
                    final BigDecimal endingBalance) {

        Objects.requireNonNull(account);
        Objects.requireNonNull(closingDate);
        Objects.requireNonNull(openingBalance);
        Objects.requireNonNull(endingBalance);

        this.account = account;

        final String[] columnNames = RegisterFactory.getCreditDebitTabNames(this.account.getAccountType());

        increaseTitledPane.setText(columnNames[0]);
        decreaseTitledPane.setText(columnNames[1]);
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parentProperty.get().getWindow()).close();
    }

    @FXML
    private void handleDecreaseSelectAllAction() {
    }

    @FXML
    private void handleDecreaseClearAllAction() {
    }

    @FXML
    private void handleIncreaseSelectAllAction() {
    }

    @FXML
    private void handleIncreaseClearAllAction() {
    }

    @FXML
    private void handleOverrideAction() {
        handleCloseAction();
    }

    @FXML
    private void handleFinishLaterAction() {
        handleCloseAction();
    }

    @FXML
    private void handleFinishAction() {
        handleCloseAction();
    }

    private void configureTableView(final TableView<RecTransaction> tableView) {

        tableView.setTableMenuButtonVisible(false);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        final TableColumn<RecTransaction, String> reconciledColumn = new TableColumn<>(resources.getString("Column.Clr"));
        reconciledColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getReconciledState().toString()));
        tableView.getColumns().add(reconciledColumn);

        final TableColumn<RecTransaction, LocalDate> dateColumn = new TableColumn<>(resources.getString("Column.Date"));
        dateColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getDate()));
        dateColumn.setCellFactory(param -> new RecTransactionDateTableCell());
        tableView.getColumns().add(dateColumn);

        //Num
        final TableColumn<RecTransaction, String> numberColumn = new TableColumn<>(resources.getString("Column.Num"));
        numberColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getNumber()));
        tableView.getColumns().add(numberColumn);

        //Payee
        final TableColumn<RecTransaction, String> payeeColumn = new TableColumn<>(resources.getString("Column.Payee"));
        payeeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getPayee()));
        tableView.getColumns().add(payeeColumn);

        //Amount
        final TableColumn<RecTransaction, BigDecimal> amountColumn =
                new TableColumn<>(resources.getString("Column.Amount"));
        amountColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(AccountBalanceDisplayManager.
                convertToSelectedBalanceMode(account.getAccountType(), param.getValue().getAmount(account))));
        amountColumn.setCellFactory(param -> new RecTransactionCommodityFormatTableCell(
                CommodityFormat.getShortNumberFormat(account.getCurrencyNode())));

        tableView.getColumns().add(amountColumn);
    }

    @Override
    public void messagePosted(final Message message) {

    }
}
