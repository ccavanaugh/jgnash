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
import java.time.LocalDate;
import java.time.LocalDateTime;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.text.NumericFormats;
import jgnash.uifx.views.AccountBalanceDisplayManager;
import jgnash.time.DateUtils;

/**
 * Register Table with stats controller.
 *
 * @author Craig Cavanaugh
 */
public class BasicRegisterTableController extends RegisterTableController {

    @FXML
    private Label reconciledBalanceLabel;

    private static final double[] PREF_COLUMN_WEIGHTS = {0, 0, 0, 30, 40, 30, 0, 0, 0, 0};

    private static final boolean[] DEFAULT_COLUMN_VISIBILITY = {true, false, true, true, true, true, true, true, true, true};

    @FXML
    @Override
    void initialize() {
        super.initialize();

        // Bind the reconciledBalance Label to the account property wrapper
        reconciledBalanceLabel.textProperty().bind(getAccountPropertyWrapper().reconciledAmountProperty());
    }

    @Override
    Callback<Integer, Double> getColumnWeightFactory() {
        return param -> PREF_COLUMN_WEIGHTS[param];
    }

    @Override
    Callback<Integer, Boolean> getColumnVisibilityFactory() {
        return param -> DEFAULT_COLUMN_VISIBILITY[param];
    }

    @Override
    protected void buildTable() {
        final String[] columnNames = RegisterFactory.getColumnNames(accountProperty().get().getAccountType());

        final TableColumn<Transaction, LocalDate> dateColumn = new TableColumn<>(columnNames[0]);
        dateColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getLocalDate()));
        dateColumn.setCellFactory(cell -> new TransactionDateTableCell());
        tableView.getColumns().add(dateColumn);

        final TableColumn<Transaction, LocalDateTime> dateTimeColumn = new TableColumn<>(columnNames[1]);
        dateTimeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getTimestamp()));
        dateTimeColumn.setCellFactory(cell -> new TransactionDateTimeTableCell());
        tableView.getColumns().add(dateTimeColumn);

        final TableColumn<Transaction, String> numberColumn = new TableColumn<>(columnNames[2]);
        numberColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getNumber()));
        numberColumn.setCellFactory(cell -> new TransactionStringTableCell());
        tableView.getColumns().add(numberColumn);

        final TableColumn<Transaction, String> payeeColumn = new TableColumn<>(columnNames[3]);
        payeeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getPayee()));
        payeeColumn.setCellFactory(cell -> new TransactionStringTableCell());
        tableView.getColumns().add(payeeColumn);

        final TableColumn<Transaction, String> memoColumn = new TableColumn<>(columnNames[4]);
        memoColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getMemo()));
        memoColumn.setCellFactory(cell -> new TransactionStringTableCell());
        tableView.getColumns().add(memoColumn);

        final TableColumn<Transaction, String> accountColumn = new TableColumn<>(columnNames[5]);
        accountColumn.setCellValueFactory(param -> new AccountNameWrapper(param.getValue()));
        accountColumn.setCellFactory(cell -> new TransactionStringTableCell());
        tableView.getColumns().add(accountColumn);

        final TableColumn<Transaction, String> reconciledColumn = new TableColumn<>(columnNames[6]);
        reconciledColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue()
                .getReconciled(account.get()).toString()));
        reconciledColumn.setCellFactory(cell -> new TransactionStringTableCell());
        tableView.getColumns().add(reconciledColumn);

        final Callback<TableColumn<Transaction, BigDecimal>, TableCell<Transaction, BigDecimal>> shortDecimalCellFactory =
                cell -> new TransactionCommodityFormatTableCell(NumericFormats
                        .getShortCommodityFormat(account.get().getCurrencyNode()));

        final TableColumn<Transaction, BigDecimal> increaseColumn = new TableColumn<>(columnNames[7]);
        increaseColumn.setCellValueFactory(param -> new IncreaseAmountProperty(param.getValue()
                .getAmount(accountProperty().getValue())));
        increaseColumn.setCellFactory(shortDecimalCellFactory);
        tableView.getColumns().add(increaseColumn);

        final TableColumn<Transaction, BigDecimal> decreaseColumn = new TableColumn<>(columnNames[8]);
        decreaseColumn.setCellValueFactory(param -> new DecreaseAmountProperty(param.getValue()
                .getAmount(accountProperty().getValue())));
        decreaseColumn.setCellFactory(shortDecimalCellFactory);
        tableView.getColumns().add(decreaseColumn);

        final TableColumn<Transaction, BigDecimal> balanceColumn = new TableColumn<>(columnNames[9]);
        balanceColumn.setCellValueFactory(param -> {
            final AccountType accountType = accountProperty().getValue().getAccountType();

            return new SimpleObjectProperty<>(AccountBalanceDisplayManager.
                    convertToSelectedBalanceMode(accountType, getBalanceAt(param.getValue())));
        });

        balanceColumn.setCellFactory(cell -> new TransactionCommodityFormatTableCell(NumericFormats
                .getFullCommodityFormat(account.get().getCurrencyNode())));
        balanceColumn.setSortable(false);   // do not allow a sort on the balance
        tableView.getColumns().add(balanceColumn);

        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tableViewManager.setColumnFormatFactory(param -> {
            if (param == balanceColumn) {
                return NumericFormats.getFullCommodityFormat(accountProperty().getValue().getCurrencyNode());
            } else if (param == increaseColumn || param == decreaseColumn) {
                return NumericFormats.getShortCommodityFormat(accountProperty().getValue().getCurrencyNode());
            } else if (param == dateColumn) {
                return DateUtils.getShortDateFormatter().toFormat();
            } else if (param == dateTimeColumn) {
                return DateUtils.getShortDateTimeFormatter().toFormat();
            }

            return null;
        });
    }

    private BigDecimal getBalanceAt(final Transaction transaction) {
        BigDecimal balance = BigDecimal.ZERO;

        final Account account = this.account.get();

        if (account != null) {
            final int index = sortedList.indexOf(transaction);

            for (int i = 0; i <= index; i++) {
                balance = balance.add(sortedList.get(i).getAmount(account));
            }
        }
        return balance;
    }

    private class AccountNameWrapper extends SimpleStringProperty {
        final String split = resources.getString("Button.Splits");

        AccountNameWrapper(final Transaction t) {
            super();

            if (t instanceof InvestmentTransaction) {
                setValue(((InvestmentTransaction) t).getInvestmentAccount().getName());
            } else {
                int count = t.size();
                if (count > 1) {
                    setValue("[ " + count + " " + split + " ]");
                } else {
                    Account creditAccount = t.getTransactionEntries().get(0).getCreditAccount();
                    if (creditAccount != account.get()) {
                        setValue(creditAccount.getName());
                    } else {
                        setValue(t.getTransactionEntries().get(0).getDebitAccount().getName());
                    }
                }
            }
        }
    }
}
