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
package jgnash.uifx.report.jasper;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;

import jgnash.engine.Account;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionType;
import jgnash.ui.report.jasper.AbstractReportTableModel;
import jgnash.ui.report.jasper.ColumnHeaderStyle;
import jgnash.ui.report.jasper.ColumnStyle;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DatePickerEx;
import jgnash.util.Nullable;

import net.sf.jasperreports.engine.JasperPrint;

/**
 * Account Register Report
 *
 * @author Craig Cavanaugh
 */
public class AccountRegisterReport extends DynamicJasperReport {

    @FXML
    AccountComboBox accountComboBox;

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    private ReportModel reportModel = new ReportModel();

    private boolean showSplits;

    @FXML
    private void initialize() {
        accountComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            reportModel.loadAccount(newValue);
        });
    }

    @Override
    public JasperPrint createJasperPrint(boolean formatForCSV) {
        return null;
    }

    @Override
    public String getReportName() {
        return accountComboBox.getValue().getName();
    }

    @Override
    protected String getGrandTotalLegend() {
        return null;
    }

    @Override
    protected String getGroupFooterLabel() {
        return rb.getString("Word.Totals");
    }

    private class ReportModel extends AbstractReportTableModel {

        final ObservableList<Row> rows = FXCollections.observableArrayList();

        final FilteredList<Row> filteredList = new FilteredList<>(rows);

        void loadAccount(@Nullable final Account account) {

            rows.clear();

            if (account != null) {
                for (Transaction transaction : account.getSortedTransactionList()) {
                    if (showSplits && transaction.getTransactionType() == TransactionType.SPLITENTRY) {
                        List<TransactionEntry> transactionEntries = transaction.getTransactionEntries();
                        for (int i = 0; i < transactionEntries.size(); i++) {
                            rows.add(new Row(transaction, i));
                        }
                    } else {
                        rows.add(new Row(transaction, -1));
                    }
                }
            }
        }

        @Override
        public CurrencyNode getCurrency() {
            return accountComboBox.getValue().getCurrencyNode();
        }

        @Override
        public ColumnStyle getColumnStyle(int columnIndex) {
            return null;
        }

        @Override
        public ColumnHeaderStyle getColumnHeaderStyle(int columnIndex) {
            return null;
        }

        @Override
        public int getRowCount() {
            return filteredList.size();
        }

        @Override
        public int getColumnCount() {
            return 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return filteredList.get(rowIndex).getValueAt(columnIndex);
        }
    }

    private class Row {
        private Transaction transaction;

        private TransactionEntry transactionEntry;

        Row(final Transaction transaction, final int entry) {
            this.transaction = transaction;

            if (entry >= 0) {
                transactionEntry = transaction.getTransactionEntries().get(entry);
            }
        }

        Object getValueAt(int columnIndex) {
            return null;
        }
    }
}
