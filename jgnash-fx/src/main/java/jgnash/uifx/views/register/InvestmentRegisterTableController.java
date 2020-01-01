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
import java.text.Format;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import jgnash.engine.CommodityNode;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.MathConstants;
import jgnash.engine.Transaction;
import jgnash.text.NumericFormats;
import jgnash.time.DateUtils;

/**
 * Investment Register Table with stats controller.
 *
 * @author Craig Cavanaugh
 */
public class InvestmentRegisterTableController extends RegisterTableController {

    @FXML
    private Label cashBalanceLabel;

    @FXML
    private Label marketValueLabel;

    final private double[] PREF_COLUMN_WEIGHTS = {0, 0, 33, 33, 34, 0, 0, 0, 0};

    private static final boolean[] DEFAULT_COLUMN_VISIBILITY = {true, false, true, true, true, true, true, true, true};

    private NumberFormat quantityNumberFormat;

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

        final TableColumn<Transaction, String> typeColumn = new TableColumn<>(columnNames[2]);
        typeColumn.setCellValueFactory(param -> new TransactionTypeWrapper(param.getValue()));
        typeColumn.setCellFactory(cell -> new TransactionStringTableCell());
        tableView.getColumns().add(typeColumn);

        final TableColumn<Transaction, String> investmentColumn = new TableColumn<>(columnNames[3]);
        investmentColumn.setCellValueFactory(param -> new TransactionSymbolWrapper(param.getValue()));
        investmentColumn.setCellFactory(cell -> new TransactionStringTableCell());
        tableView.getColumns().add(investmentColumn);

        final TableColumn<Transaction, String> memoColumn = new TableColumn<>(columnNames[4]);
        memoColumn.setCellValueFactory(param -> new MemoWrapper(param.getValue()));
        memoColumn.setCellFactory(cell -> new TransactionStringTableCell());
        tableView.getColumns().add(memoColumn);

        final TableColumn<Transaction, String> reconciledColumn = new TableColumn<>(columnNames[5]);
        reconciledColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue()
                .getReconciled(account.getValue()).toString()));
        reconciledColumn.setCellFactory(cell -> new TransactionStringTableCell());
        tableView.getColumns().add(reconciledColumn);

        final TableColumn<Transaction, BigDecimal> quantityColumn = new TableColumn<>(columnNames[6]);
        quantityColumn.setCellValueFactory(param -> new QuantityProperty(param.getValue()));
        quantityColumn.setCellFactory(cell -> new InvestmentTransactionQuantityTableCell());
        tableView.getColumns().add(quantityColumn);

        final TableColumn<Transaction, BigDecimal> priceColumn = new TableColumn<>(columnNames[7]);
        priceColumn.setCellValueFactory(param -> new PriceProperty(param.getValue()));
        priceColumn.setCellFactory(cell
                -> new TransactionCommodityFormatTableCell(NumericFormats.getShortCommodityFormat(account.get().getCurrencyNode())));
        tableView.getColumns().add(priceColumn);

        final TableColumn<Transaction, BigDecimal> netColumn = new TableColumn<>(columnNames[8]);
        netColumn.setCellValueFactory(param -> new AmountProperty(param.getValue()));
        netColumn.setCellFactory(cell
                -> new TransactionCommodityFormatTableCell(NumericFormats.getFullCommodityFormat(account.get().getCurrencyNode())));
        tableView.getColumns().add(netColumn);

        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tableViewManager.setColumnFormatFactory(param -> {
            if (param == netColumn) {
                return NumericFormats.getFullCommodityFormat(accountProperty().getValue().getCurrencyNode());
            } else if (param == quantityColumn) {
                return getQuantityColumnFormat();
            } else if (param == priceColumn) {
                return NumericFormats.getShortCommodityFormat(accountProperty().getValue().getCurrencyNode());
            } else if (param == dateColumn) {
                return DateUtils.getShortDateFormatter().toFormat();
            } else if (param == dateTimeColumn) {
                return DateUtils.getShortDateTimeFormatter().toFormat();
            }

            return null;
        });
    }

    private Format getQuantityColumnFormat() {
        if (quantityNumberFormat == null) {
            if (account.get() != null) {
                final int max = account.get().getUsedSecurities().parallelStream().mapToInt(CommodityNode::getScale)
                        .max().orElse(MathConstants.DEFAULT_COMMODITY_PRECISION);

                quantityNumberFormat = NumericFormats.getFixedPrecisionFormat(max);
            }

            // Just return a default for now
            return NumericFormats.getFixedPrecisionFormat(MathConstants.DEFAULT_COMMODITY_PRECISION);
        }
        return quantityNumberFormat;
    }

    private class TransactionTypeWrapper extends SimpleStringProperty {
        TransactionTypeWrapper(final Transaction t) {
            super();

            if (t instanceof InvestmentTransaction) {
                setValue(t.getTransactionType().toString());
            } else if (t.getAmount(account.get()).signum() > 0) {
                setValue(resources.getString("Item.CashDeposit"));
            } else {
                setValue(resources.getString("Item.CashWithdrawal"));
            }
        }
    }

    private static class TransactionSymbolWrapper extends SimpleStringProperty {
        TransactionSymbolWrapper(final Transaction t) {
            super();

            if (t instanceof InvestmentTransaction) {
                setValue(((InvestmentTransaction) t).getSecurityNode().getSymbol());
            } else {
                setValue(null);
            }
        }
    }

    private static class MemoWrapper extends SimpleStringProperty {
        MemoWrapper(final Transaction t) {
            super();
            setValue(t.getMemo());
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
                setValue(t.getAmount(account.get()));
            }
        }
    }
}
