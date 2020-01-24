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
package jgnash.uifx.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionType;
import jgnash.report.pdf.Report;
import jgnash.report.table.AbstractReportTableModel;
import jgnash.report.table.ColumnStyle;
import jgnash.report.table.Row;
import jgnash.resource.util.ResourceUtils;
import jgnash.time.DateUtils;
import jgnash.uifx.views.register.RegisterFactory;
import jgnash.util.Nullable;

/**
 * Account Register Report Model
 *
 * @author Craig Cavanaugh
 */
public class AccountRegisterReport extends Report {

    AccountRegisterReport() {
        setForceGroupPagination(false);
    }

    public static AbstractReportTableModel createReportModel(final Account account, final LocalDate startDate,
                                                      final LocalDate endDate, final boolean showSplits,
                                                      final String memoFilter, final String payeeFilter,
                                                      final boolean showTimeStamp) {

        if (account.getAccountType().getAccountGroup() == AccountGroup.INVEST) {
            return new AccountRegisterReport.InvestmentAccountReportModel(account, startDate, endDate, memoFilter,
                    showTimeStamp);
        }

        return new AccountRegisterReport.AccountReportModel(account, showSplits, startDate, endDate, memoFilter,
                payeeFilter, showTimeStamp);

    }

    private static class AccountReportModel extends AbstractReportTableModel {

        private static final String INDENT_PREFIX = "  - ";

        private static final String SPLIT = ResourceUtils.getString("Button.Splits");

        private final boolean showSplits;

        private final boolean sumAmounts;

        private final Account account;

        private final ObservableList<Row<Transaction>> transactionRows = FXCollections.observableArrayList();

        private final FilteredList<Row<Transaction>> filteredList = new FilteredList<>(transactionRows);

        private String[] columnNames = RegisterFactory.getColumnNames(AccountType.BANK);

        private final boolean showTimestamp;

        private static final ColumnStyle[] columnStyles = new ColumnStyle[]{ColumnStyle.SHORT_DATE, ColumnStyle.TIMESTAMP,
                ColumnStyle.STRING, ColumnStyle.STRING, ColumnStyle.STRING, ColumnStyle.STRING, ColumnStyle.STRING,
                ColumnStyle.SHORT_AMOUNT, ColumnStyle.SHORT_AMOUNT, ColumnStyle.BALANCE};

        AccountReportModel(@Nullable final Account account, final boolean showSplits, final LocalDate startDate,
                           final LocalDate endDate, final String memoFilter, final String payeeFilter,
                           final boolean showTimestamp) {
            this.account = account;
            this.showSplits = showSplits;

            sumAmounts = (memoFilter != null && !memoFilter.isEmpty())
                    || (payeeFilter != null && !payeeFilter.isEmpty());

            filteredList.setPredicate(new TransactionAfterDatePredicate(startDate)
                    .and(new TransactionBeforeDatePredicate(endDate))
                    .and(new MemoPredicate(memoFilter))
                    .and(new PayeePredicate(payeeFilter)));

            this.showTimestamp = showTimestamp;

            loadAccount();
        }

        @Override
        public String getTitle() {
            return account.getName();
        }

        @Override
        public String getSubTitle() {
            return null;
        }

        @Override
        public String getGroupFooterLabel() {
            return rb.getString("Word.Totals");
        }

        @Override
        public int[] getColumnsToHide() {

            if (!showTimestamp && sumAmounts) {
                return new int[]{1, 9};
            } else if (!showTimestamp) {
                return new int[]{1};
            }

            return super.getColumnsToHide();
        }

        @Override
        public boolean isColumnFixedWidth(final int columnIndex) {
            switch (columnIndex) {
                case 0:
                case 1:
                case 2:
                case 6:
                case 7:
                case 8:
                case 9:
                    return true;
                default:
                    return false;
            }
        }

        public float getColumnWidthWeight(int columnIndex) {
            switch (columnIndex) {
                case 3:
                case 5:
                    return 30;
                case 4:
                    return 40;
                default:
                    return 0;
            }
        }

        private void loadAccount() {
            if (account != null) {
                if (sumAmounts) {   // dump the running total column as it does not make sense when filtering
                    final String[] base = RegisterFactory.getColumnNames(account.getAccountType());
                    columnNames = Arrays.copyOfRange(base, 0, base.length - 1);
                } else {
                    columnNames = RegisterFactory.getColumnNames(account.getAccountType());
                }

                for (final Transaction transaction : account.getSortedTransactionList()) {
                    if (showSplits && transaction.getTransactionType() == TransactionType.SPLITENTRY
                            && transaction.getCommonAccount() == account) {
                        transactionRows.add(new TransactionRow(transaction, -1));
                        List<TransactionEntry> transactionEntries = transaction.getTransactionEntries();
                        for (int i = 0; i < transactionEntries.size(); i++) {
                            transactionRows.add(new TransactionRow(transaction, i));
                        }
                    } else {
                        transactionRows.add(new TransactionRow(transaction, -1));
                    }
                }
            }
        }

        @Override
        public CurrencyNode getCurrencyNode() {
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
                    return LocalDateTime.class;
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                    return String.class;
                default:
                    return BigDecimal.class;
            }
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            return filteredList.get(rowIndex).getValueAt(columnIndex);
        }

        private class TransactionRow extends Row<Transaction> {
            private final BigDecimal amount;
            private final int signum;
            private final TransactionEntry transactionEntry;

            TransactionRow(final Transaction transaction, final int entry) {
                super(transaction);

                if (entry >= 0) {
                    transactionEntry = transaction.getTransactionEntries().get(entry);
                    amount = transactionEntry.getAmount(account);
                } else {
                    transactionEntry = null;
                    amount = transaction.getAmount(account);
                }

                signum = amount.signum();
            }

            @Override
            public Object getValueAt(final int columnIndex) {

                if (sumAmounts && columnIndex == columnNames.length) {
                    return "group";
                }

                final Transaction transaction = getValue();

                if (transactionEntry == null) {
                    switch (columnIndex) {
                        case 0:
                            return transaction.getLocalDate();
                        case 1:
                            return transaction.getTimestamp();
                        case 2:
                            return transaction.getNumber();
                        case 3:
                            return transaction.getPayee();
                        case 4:
                            return transaction.getMemo(account);
                        case 5:
                            final int count = transaction.size();

                            if (count > 1) {
                                return "[ " + count + " " + SPLIT + " ]";
                            }

                            final TransactionEntry entry = transaction.getTransactionEntries().get(0);

                            if (entry.getCreditAccount() != account) {
                                return entry.getCreditAccount().getName();
                            }
                            return entry.getDebitAccount().getName();
                        case 6:
                            return transaction.getReconciled(account) != ReconciledState.NOT_RECONCILED
                                    ? transaction.getReconciled(account).toString() : null;
                        case 7:
                            if (signum >= 0) {
                                return amount;
                            }
                            return null;
                        case 8:
                            if (signum < 0) {
                                return amount.abs();
                            }
                            return null;
                        case 9:
                            return account.getBalanceAt(transaction);
                        default:
                            return null;
                    }
                }

                switch (columnIndex) {
                    case 4:
                        return transactionEntry.getMemo();
                    case 5:
                        if (transactionEntry.getCreditAccount() != account) {
                            return INDENT_PREFIX + transactionEntry.getCreditAccount().getName();
                        }
                        return INDENT_PREFIX + transactionEntry.getDebitAccount().getName();
                    case 6:
                        return transaction.getReconciled(account) != ReconciledState.NOT_RECONCILED
                                ? transaction.getReconciled(account).toString() : null;
                    case 7:
                        if (signum >= 0) {
                            return amount;
                        }
                        return null;
                    case 8:
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

    private static class InvestmentAccountReportModel extends AbstractReportTableModel {

        private final ResourceBundle resources = ResourceUtils.getBundle();

        private final Account account;

        private final ObservableList<Row<Transaction>> transactionRows = FXCollections.observableArrayList();

        private final FilteredList<Row<Transaction>> filteredList = new FilteredList<>(transactionRows);

        private String[] columnNames = RegisterFactory.getColumnNames(AccountType.INVEST);

        private static final ColumnStyle[] columnStyles = new ColumnStyle[]{ColumnStyle.SHORT_DATE, ColumnStyle.TIMESTAMP,
                ColumnStyle.STRING, ColumnStyle.STRING, ColumnStyle.STRING, ColumnStyle.STRING, ColumnStyle.SHORT_AMOUNT,
                ColumnStyle.SHORT_AMOUNT, ColumnStyle.AMOUNT_SUM};

        private final boolean showTimestamp;

        InvestmentAccountReportModel(@Nullable final Account account, final LocalDate startDate,
                                     final LocalDate endDate, final String memoFilter, final boolean showTimestamp) {
            this.account = account;

            filteredList.setPredicate(new TransactionAfterDatePredicate(startDate)
                    .and(new TransactionBeforeDatePredicate(endDate))
                    .and(new MemoPredicate(memoFilter)));

            this.showTimestamp = showTimestamp;

            loadAccount();
        }

        @Override
        public String getTitle() {
            return account.getName();
        }

        @Override
        public String getSubTitle() {
            return null;
        }

        @Override
        public int[] getColumnsToHide() {
            if (showTimestamp) {
                return super.getColumnsToHide();
            }

            return new int[]{1};
        }

        @Override
        public boolean isColumnFixedWidth(final int columnIndex) {
            switch (columnIndex) {
                case 0:
                case 1:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                    return true;
                default:
                    return false;
            }
        }

        private void loadAccount() {
            if (account != null) {
                columnNames = RegisterFactory.getColumnNames(account.getAccountType());

                transactionRows.addAll(account.getSortedTransactionList().stream()
                        .map(TransactionRow::new).collect(Collectors.toList()));
            }
        }

        @Override
        public CurrencyNode getCurrencyNode() {
            return account.getCurrencyNode();
        }

        @Override
        public ColumnStyle getColumnStyle(final int columnIndex) {
            return columnStyles[columnIndex];
        }

        @Override
        public int getRowCount() {
            return filteredList.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(final int columnIndex) {
            return columnNames[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return LocalDate.class;
                case 1:
                    return LocalDateTime.class;
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

        private class TransactionRow extends Row<Transaction> {

            TransactionRow(final Transaction transaction) {
                super(transaction);
            }

            @Override
            public Object getValueAt(final int columnIndex) {
                final Transaction transaction = getValue();

                switch (columnIndex) {
                    case 0:
                        return transaction.getLocalDate();
                    case 1:
                        return transaction.getTimestamp();
                    case 2:
                        if (transaction instanceof InvestmentTransaction) {
                            return transaction.getTransactionType().toString();
                        } else if (transaction.getAmount(account).signum() > 0) {
                            return resources.getString("Item.CashDeposit");
                        } else {
                            return resources.getString("Item.CashWithdrawal");
                        }
                    case 3:
                        if (transaction instanceof InvestmentTransaction) {
                            return ((InvestmentTransaction) transaction).getSecurityNode().getSymbol();
                        }
                        return null;
                    case 4:
                        return transaction.getMemo(account);
                    case 5:
                        return transaction.getReconciled(account) != ReconciledState.NOT_RECONCILED
                                ? transaction.getReconciled(account).toString() : null;
                    case 6:
                        if (transaction instanceof InvestmentTransaction) {
                            return ((InvestmentTransaction) transaction).getQuantity();
                        }
                        return null;
                    case 7:
                        if (transaction instanceof InvestmentTransaction) {
                            return ((InvestmentTransaction) transaction).getPrice();
                        }
                        return null;
                    case 8:
                        if (transaction instanceof InvestmentTransaction) {
                            return ((InvestmentTransaction) transaction).getNetCashValue();
                        }
                        return transaction.getAmount(account);
                    default:
                        return null;
                }
            }
        }
    }

    private static class TransactionBeforeDatePredicate implements Predicate<Row<Transaction>> {

        private final LocalDate localDate;

        TransactionBeforeDatePredicate(final LocalDate localDate) {
            this.localDate = localDate;
        }

        @Override
        public boolean test(final Row<Transaction> transactionRow) {
            return DateUtils.before(transactionRow.getValue().getLocalDate(), localDate);
        }
    }

    private static class TransactionAfterDatePredicate implements Predicate<Row<Transaction>> {

        private final LocalDate localDate;

        TransactionAfterDatePredicate(final LocalDate localDate) {
            this.localDate = localDate;
        }

        @Override
        public boolean test(final Row<Transaction> transactionRow) {
            return DateUtils.after(transactionRow.getValue().getLocalDate(), localDate);
        }
    }

    private static class MemoPredicate implements Predicate<Row<Transaction>> {

        private final String filter;

        MemoPredicate(final String memo) {
            if (memo != null && !memo.isEmpty()) {
                filter = memo.toLowerCase(Locale.getDefault());
            } else {
                filter = memo;
            }
        }

        @Override
        public boolean test(final Row<Transaction> transactionRow) {
            return transactionRow.getValue().getMemo().toLowerCase(Locale.getDefault()).contains(filter);
        }
    }

    private static class PayeePredicate implements Predicate<Row<Transaction>> {

        private final String filter;

        PayeePredicate(final String payee) {
            if (payee != null && !payee.isEmpty()) {
                filter = payee.toLowerCase(Locale.getDefault());
            } else {
                filter = payee;
            }
        }

        @Override
        public boolean test(final Row<Transaction> transactionRow) {
            return transactionRow.getValue().getPayee().toLowerCase(Locale.getDefault()).contains(filter);
        }
    }

}
