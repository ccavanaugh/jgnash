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
import java.time.LocalDate;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.text.CommodityFormat;
import jgnash.util.DateUtils;

/**
 * Investment Register Table with stats controller
 * <p/>
 *
 * @author Craig Cavanaugh
 */
public class InvestmentRegisterTableController extends RegisterTableController {

    @FXML
    private Label cashBalanceLabel;

    @FXML
    private Label marketValueLabel;

    final private double[] PREF_COLUMN_WEIGHTS = { 0, 50, 50, 0, 0, 0, 0 };

    @FXML
    @Override
    void initialize() {
        super.initialize();

        // Bind the label text to the account property wrapper
        cashBalanceLabel.textProperty().bind(getAccountPropertyWrapper().cashBalanceProperty());
        marketValueLabel.textProperty().bind(getAccountPropertyWrapper().marketValueProperty());
    }

    @Override
    Callback<Integer, Double> getColumnWeightFactory() {
        return param -> PREF_COLUMN_WEIGHTS[param];
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void buildTable() {
        final String[] columnNames = RegisterFactory.getColumnNames(getAccountProperty().get().getAccountType());

        final TableColumn<Transaction, LocalDate> dateColumn = new TableColumn<>(columnNames[0]);
        dateColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getLocalDate()));
        dateColumn.setCellFactory(cell -> new TransactionDateTableCell());

        final TableColumn<Transaction, String> typeColumn = new TableColumn<>(columnNames[1]);
        typeColumn.setCellValueFactory(param -> new TransactionTypeWrapper(param.getValue()));
        typeColumn.setCellFactory(cell -> new TransactionStringTableCell());

        final TableColumn<Transaction, String> memoColumn = new TableColumn<>(columnNames[2]);
        memoColumn.setCellValueFactory(param -> new TransactionSymbolWrapper(param.getValue()));
        memoColumn.setCellFactory(cell -> new TransactionStringTableCell());

        final TableColumn<Transaction, String> reconciledColumn = new TableColumn<>(columnNames[3]);
        reconciledColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getReconciled(accountProperty.getValue()).toString()));
        reconciledColumn.setCellFactory(cell -> new TransactionStringTableCell());

        final TableColumn<Transaction, BigDecimal> quantityColumn = new TableColumn<>(columnNames[4]);
        quantityColumn.setCellValueFactory(param -> new QuantityProperty(param.getValue()));
        quantityColumn.setCellFactory(cell -> new InvestmentTransactionQuantityTableCell());

        final TableColumn<Transaction, BigDecimal> priceColumn = new TableColumn<>(columnNames[5]);
        priceColumn.setCellValueFactory(param -> new PriceProperty(param.getValue()));
        priceColumn.setCellFactory(cell -> new TransactionCommodityFormatTableCell(CommodityFormat.getShortNumberFormat(accountProperty.get().getCurrencyNode())));

        final TableColumn<Transaction, BigDecimal> netColumn = new TableColumn<>(columnNames[6]);
        netColumn.setCellValueFactory(param -> new AmountProperty(param.getValue()));
        netColumn.setCellFactory(cell -> new TransactionCommodityFormatTableCell(CommodityFormat.getShortNumberFormat(accountProperty.get().getCurrencyNode())));

        tableView.getColumns().addAll(dateColumn, typeColumn, memoColumn, reconciledColumn, quantityColumn, priceColumn, netColumn);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tableViewManager.setColumnFormatFactory(param -> {
            if (param == netColumn) {
                return CommodityFormat.getFullNumberFormat(getAccountProperty().getValue().getCurrencyNode());
            } else if (param == quantityColumn || param == priceColumn) {
                return CommodityFormat.getShortNumberFormat(getAccountProperty().getValue().getCurrencyNode());
            } else if (param == dateColumn) {
                return DateUtils.getShortDateTimeFormat().toFormat();
            }

            return null;
        });
    }

    private class TransactionTypeWrapper extends SimpleStringProperty {
        TransactionTypeWrapper(final Transaction t) {
            super();

            if (t instanceof InvestmentTransaction) {
                setValue(t.getTransactionType().toString());
            } else if (t.getAmount(accountProperty.get()).signum() > 0) {
                setValue(resources.getString("Item.CashDeposit"));
            } else {
                setValue(resources.getString("Item.CashWithdrawal"));
            }
        }
    }

    private class TransactionSymbolWrapper extends SimpleStringProperty {
        TransactionSymbolWrapper(final Transaction t) {
            super();

            if (t instanceof InvestmentTransaction) {
                setValue(((InvestmentTransaction) t).getSecurityNode().getSymbol());
            } else if (t.getAmount(accountProperty.get()).signum() > 0) {
                setValue(t.getMemo());
            }
        }
    }

    private static class QuantityProperty extends SimpleObjectProperty<BigDecimal> {
        QuantityProperty(final Transaction t) {
            if (t instanceof InvestmentTransaction) {
                final BigDecimal quantity = ((InvestmentTransaction) t).getQuantity();

                if (quantity.compareTo(BigDecimal.ZERO) != 0) {
                    setValue(quantity);
                }
            } else {
                setValue(null);
            }
        }
    }

    private static class PriceProperty extends SimpleObjectProperty<BigDecimal> {
        PriceProperty(final Transaction t) {
            if (t instanceof InvestmentTransaction) {
                final BigDecimal price = ((InvestmentTransaction) t).getPrice();

                if (price.compareTo(BigDecimal.ZERO) != 0) {
                    setValue(price);
                }
            } else {
                setValue(null);
            }
        }
    }

    private class AmountProperty extends SimpleObjectProperty<BigDecimal> {
        AmountProperty(final Transaction t) {
            if (t instanceof InvestmentTransaction) {
                setValue(((InvestmentTransaction) t).getNetCashValue());
            } else {
                setValue(t.getAmount(accountProperty.get()));
            }
        }
    }
}
