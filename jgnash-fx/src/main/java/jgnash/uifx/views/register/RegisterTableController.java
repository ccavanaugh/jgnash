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
import java.util.Date;
import java.util.ResourceBundle;

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.uifx.control.TransactionDateTableCell;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Register Pane
 *
 * @author Craig Cavanaugh
 */
public class RegisterTableController implements Initializable {

    private final AccountPropertyWrapper accountPropertyWrapper = new AccountPropertyWrapper();

    private ResourceBundle resources;

    @FXML
    private TableView<Transaction> tableView;

    @FXML
    private Label reconciledBalanceLabel;

    @FXML
    private Label balanceLabel;

    @FXML
    private Label accountNameLabel;

    /**
     * Active account for the pane
     */
    private ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    AccountPropertyWrapper getAccountPropertyWrapper() {
        return accountPropertyWrapper;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.resources = resources;

        tableView.setTableMenuButtonVisible(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Bind the account property
        getAccountPropertyWrapper().getAccountProperty().bind(accountProperty);

        // Bind label test to the account property wrapper
        accountNameLabel.textProperty().bind(getAccountPropertyWrapper().getAccountNameProperty());
        balanceLabel.textProperty().bind(getAccountPropertyWrapper().getAccountBalanceProperty());
        reconciledBalanceLabel.textProperty().bind(getAccountPropertyWrapper().getReconciledAmountProperty());

        buildTable();

        // Load the table on change
        getAccountProperty().addListener((observable, oldValue, newValue) -> loadTable());
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        final TableColumn<Transaction, Date> dateColumn = new TableColumn<>(resources.getString("Column.Date"));
        dateColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper(param.getValue().getDate()));
        dateColumn.setCellFactory(cell -> new TransactionDateTableCell());

        final TableColumn<Transaction, String> numberColumn = new TableColumn<>(resources.getString("Column.Num"));
        numberColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getNumber()));

        final TableColumn<Transaction, String> payeeColumn = new TableColumn<>(resources.getString("Column.Payee"));
        payeeColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getPayee()));

        final TableColumn<Transaction, String> memoColumn = new TableColumn<>(resources.getString("Column.Memo"));
        memoColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getMemo()));

        tableView.getColumns().addAll(dateColumn, numberColumn, payeeColumn, memoColumn);
    }

    private void loadTable() {
        tableView.getItems().clear();

        if (accountProperty.get() != null) {
            tableView.getItems().addAll(accountProperty.get().getSortedTransactionList());
        }
    }
}
