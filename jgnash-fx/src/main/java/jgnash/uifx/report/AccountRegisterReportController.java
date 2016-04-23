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
package jgnash.uifx.report;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionType;
import jgnash.ui.report.jasper.AbstractReportTableModel;
import jgnash.ui.report.jasper.ColumnHeaderStyle;
import jgnash.ui.report.jasper.ColumnStyle;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.report.jasper.DynamicJasperReport;
import jgnash.uifx.views.register.RegisterFactory;
import jgnash.util.DateUtils;
import jgnash.util.Nullable;
import jgnash.util.ResourceUtils;

import net.sf.jasperreports.engine.JasperPrint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.prefs.Preferences;

/**
 * Account Register Report
 *
 * @author Craig Cavanaugh
 */
public class AccountRegisterReportController extends DynamicJasperReport {

    @FXML
    private TextField memoFilterTextField;

    @FXML
    private TextField payeeFilterTextField;

    @FXML
    AccountComboBox accountComboBox;

    @FXML
    private CheckBox showSplitsCheckBox;

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    private static final String SHOW_SPLITS = "showSplits";

    @FXML
    private void initialize() {
        refreshAccount(accountComboBox.getValue());

        accountComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            refreshAccount(newValue);
            handleRefresh();
        });

        showSplitsCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            handleRefresh();
        });

        startDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            handleRefresh();
        });

        endDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            handleRefresh();
        });

        payeeFilterTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            handleRefresh();
        });

        memoFilterTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            handleRefresh();
        });
    }

    public void setAccount(final Account account) {
        accountComboBox.setValue(account);
    }

    private void refreshAccount(final Account account) {
        if (account != null) {
            if (account.getTransactionCount() > 0) {
                startDatePicker.setValue(account.getTransactionAt(0).getLocalDate());
                endDatePicker.setValue(LocalDate.now());
            }
        }
    }

    private void handleRefresh() {
        final Preferences preferences = getPreferences();

        preferences.putBoolean(SHOW_SPLITS, showSplitsCheckBox.isSelected());

        if (refreshCallBackProperty().get() != null) {
            refreshCallBackProperty().get().run();
        }
    }

    private ReportModel createReportModel(final LocalDate startDate, final LocalDate endDate) {
        return new ReportModel(accountComboBox.getValue(), showSplitsCheckBox.isSelected(), startDate, endDate,
                memoFilterTextField.getText(), payeeFilterTextField.getText());
    }

    @Override
    public JasperPrint createJasperPrint(boolean formatForCSV) {
        if (accountComboBox.getValue() != null) {
            return createJasperPrint(createReportModel(startDatePicker.getValue(), endDatePicker.getValue()), formatForCSV);
        }
        return null;
    }

    @Override
    public String getReportName() {
        if (accountComboBox.getValue() != null) {
            return accountComboBox.getValue().getName();
        }
        return "";
    }

    @Override
    protected String getGrandTotalLegend() {
        return null;
    }

    @Override
    protected String getGroupFooterLabel() {
        return rb.getString("Word.Totals");
    }

    private static class ReportModel extends AbstractReportTableModel {

        private static final String INDENT_PREFIX = "  - ";

        private final boolean showSplits;

        private final boolean sumAmounts;

        private final String split = ResourceUtils.getString("Button.Splits");

        private final Account account;

        private final ObservableList<Row> rows = FXCollections.observableArrayList();

        private final FilteredList<Row> filteredList = new FilteredList<>(rows);

        private String[] columnNames = RegisterFactory.getColumnNames(AccountType.BANK);

        private static final ColumnStyle[] columnStyles = new ColumnStyle[]{ColumnStyle.SHORT_DATE, ColumnStyle.STRING,
                ColumnStyle.STRING, ColumnStyle.STRING, ColumnStyle.STRING, ColumnStyle.STRING,
                ColumnStyle.SHORT_AMOUNT, ColumnStyle.SHORT_AMOUNT, ColumnStyle.AMOUNT_SUM};

        ReportModel(@Nullable final Account account, final boolean showSplits, final LocalDate startDate,
                    final LocalDate endDate, final String memoFilter, final String payeeFilter) {
            this.account = account;
            this.showSplits = showSplits;

            sumAmounts = (memoFilter != null && !memoFilter.isEmpty())
                    || (payeeFilter != null && !payeeFilter.isEmpty());

            filteredList.setPredicate(new TransactionAfterDatePredicate(startDate)
                    .and(new TransactionBeforeDatePredicate(endDate))
                    .and(new MemoPredicate(memoFilter))
                    .and(new PayeePredicate(payeeFilter)));

            loadAccount();
        }

        @Override
        public boolean isColumnFixedWidth(final int columnIndex) {
            switch (columnIndex) {
                case 0:
                case 5:
                case 6:
                case 7:
                case 8:
                    return true;
                default:
                    return false;
            }
        }

        void loadAccount() {
            if (account != null) {
                if (sumAmounts) {   // dump the running total column as it does not make sense when filtering
                    final String[] base = RegisterFactory.getColumnNames(account.getAccountType());
                    columnNames = Arrays.copyOfRange(base, 0, base.length - 1);
                } else {
                    columnNames = RegisterFactory.getColumnNames(account.getAccountType());
                }

                rows.clear();

                for (final Transaction transaction : account.getSortedTransactionList()) {
                    if (showSplits && transaction.getTransactionType() == TransactionType.SPLITENTRY
                            && transaction.getCommonAccount() == account) {
                        rows.add(new Row(transaction, -1));
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
            return account.getCurrencyNode();
        }

        @Override
        public ColumnStyle getColumnStyle(final int columnIndex) {

            // Override defaults is summing the accounts
            if (sumAmounts && columnIndex == columnNames.length) {
                return ColumnStyle.GROUP_NO_HEADER;
            } else if (columnStyles[columnIndex] == ColumnStyle.SHORT_AMOUNT && sumAmounts) {
                return ColumnStyle.AMOUNT_SUM;
            }

            return columnStyles[columnIndex];
        }

        @Override
        public ColumnHeaderStyle getColumnHeaderStyle(final int columnIndex) {
            if (sumAmounts && columnIndex == columnNames.length) {
                return ColumnHeaderStyle.LEFT;
            }

            if (columnIndex < 6) {
                return ColumnHeaderStyle.LEFT;
            }

            return ColumnHeaderStyle.RIGHT;
        }

        @Override
        public int getRowCount() {
            return filteredList.size();
        }

        @Override
        public int getColumnCount() {
            if (sumAmounts) {
                return columnNames.length + 1;
            }
            return columnNames.length;
        }

        @Override
        public String getColumnName(final int columnIndex) {
            if (sumAmounts && columnIndex == columnNames.length) {
                return "group";
            }
            return columnNames[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {

            if (sumAmounts && columnIndex == columnNames.length) { // group column
                return String.class;
            }

            switch (columnIndex) {
                case 0:
                    return LocalDate.class;
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    return String.class;
                default:
                    return BigDecimal.class;
            }
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            return filteredList.get(rowIndex).getValueAt(columnIndex);
        }

        class TransactionAfterDatePredicate implements Predicate<Row> {

            private final LocalDate localDate;

            TransactionAfterDatePredicate(final LocalDate localDate) {
                this.localDate = localDate;
            }

            @Override
            public boolean test(final Row row) {
                return DateUtils.after(row.transaction.getLocalDate(), localDate);
            }
        }

        class TransactionBeforeDatePredicate implements Predicate<Row> {

            private final LocalDate localDate;

            TransactionBeforeDatePredicate(final LocalDate localDate) {
                this.localDate = localDate;
            }

            @Override
            public boolean test(final Row row) {
                return DateUtils.before(row.transaction.getLocalDate(), localDate);
            }
        }

        class PayeePredicate implements Predicate<Row> {

            private final String filter;

            PayeePredicate(final String payee) {
                filter = payee;
            }

            @Override
            public boolean test(final Row row) {
                return row.transaction.getPayee().toLowerCase(Locale.getDefault()).contains(filter);
            }
        }

        class MemoPredicate implements Predicate<Row> {

            private final String filter;

            MemoPredicate(final String memo) {
                filter = memo;
            }

            @Override
            public boolean test(final Row row) {
                return row.transaction.getMemo().toLowerCase(Locale.getDefault()).contains(filter);
            }
        }

        private class Row {
            private final Transaction transaction;
            private final BigDecimal amount;
            private final int signum;
            private TransactionEntry transactionEntry;


            Row(final Transaction transaction, final int entry) {
                this.transaction = transaction;

                if (entry >= 0) {
                    transactionEntry = transaction.getTransactionEntries().get(entry);
                    amount = transactionEntry.getAmount(account);
                } else {
                    amount = transaction.getAmount(account);
                }

                signum = amount.signum();
            }

            Object getValueAt(final int columnIndex) {

                if (sumAmounts && columnIndex == columnNames.length) {
                    return "group";
                }

                if (transactionEntry == null) {
                    switch (columnIndex) {
                        case 0:
                            return transaction.getLocalDate();
                        case 1:
                            return transaction.getNumber();
                        case 2:
                            return transaction.getPayee();
                        case 3:
                            return transaction.getMemo(account);
                        case 4:
                            if (transaction.getTransactionType() == TransactionType.SPLITENTRY
                                    && transaction.getCommonAccount() == account) {
                                return "[ " + transaction.size() + " " + split + " ]";
                            } else {
                                final TransactionEntry entry = transaction.getTransactionEntries().get(0);

                                if (entry.getCreditAccount() != account) {
                                    return entry.getCreditAccount().getName();
                                }
                                return entry.getDebitAccount().getName();
                            }
                        case 5:
                            return transaction.getReconciled(account) != ReconciledState.NOT_RECONCILED
                                    ? transaction.getReconciled(account).toString() : null;
                        case 6:
                            if (signum >= 0) {
                                return amount;
                            }
                            return null;
                        case 7:
                            if (signum < 0) {
                                return amount.abs();
                            }
                            return null;
                        case 8:
                            return account.getBalanceAt(transaction);
                        default:
                            return null;
                    }
                } else {    // detailed split
                    switch (columnIndex) {
                        case 3:
                            return transactionEntry.getMemo();
                        case 4:
                            if (transactionEntry.getCreditAccount() != account) {
                                return INDENT_PREFIX + transactionEntry.getCreditAccount().getName();
                            }
                            return INDENT_PREFIX + transactionEntry.getDebitAccount().getName();
                        case 5:
                            return transaction.getReconciled(account) != ReconciledState.NOT_RECONCILED
                                    ? transaction.getReconciled(account).toString() : null;
                        case 6:
                            if (signum >= 0) {
                                return amount;
                            }
                            return null;
                        case 7:
                            if (signum < 0) {
                                return amount.abs();
                            }
                            return null;
                        default:
                            return null;
                    }
                }
            }
        }
    }
}
