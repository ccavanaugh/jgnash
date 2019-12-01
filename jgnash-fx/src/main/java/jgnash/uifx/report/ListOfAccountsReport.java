package jgnash.uifx.report;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jgnash.engine.Account;
import jgnash.engine.Comparators;
import jgnash.engine.CurrencyNode;
import jgnash.report.pdf.Report;
import jgnash.report.table.AbstractReportTableModel;
import jgnash.report.table.ColumnStyle;
import jgnash.resource.util.ResourceUtils;
import jgnash.time.DateUtils;
import jgnash.util.NotNull;

/**
 * Account List/Tree Report Model
 *
 * @author Craig Cavanaugh
 */
public class ListOfAccountsReport extends Report {

    private static final String SPACE = " ";

    private static final int INDENT = 2;

    public static class AccountListModel extends AbstractReportTableModel {

        final CurrencyNode currencyNode;

        final List<Account> accountList;

        final String[] columnNames = new String[] {ResourceUtils.getString("Column.Account"), ResourceUtils.getString("Column.Code"),
                ResourceUtils.getString("Column.Entries"), ResourceUtils.getString("Column.Balance"),
                ResourceUtils.getString("Column.ReconciledBalance"), ResourceUtils.getString("Column.Currency"),
                ResourceUtils.getString("Column.Type")};

        private final ColumnStyle[] columnStyles = new ColumnStyle[]{ColumnStyle.STRING, ColumnStyle.STRING,
                ColumnStyle.STRING, ColumnStyle.BALANCE, ColumnStyle.BALANCE, ColumnStyle.STRING, ColumnStyle.STRING};

        public AccountListModel(@NotNull final List<Account> accountList, @NotNull final CurrencyNode currencyNode) {
            this.accountList = new ArrayList<>(accountList);
            this.currencyNode = currencyNode;

            this.accountList.sort(Comparators.getAccountByTreePosition(Comparators.getAccountByCode()));
        }

        @Override
        public CurrencyNode getCurrencyNode() {
            return currencyNode;
        }

        /**
         * Returns the formatting style for the values in the column.
         *
         * @param columnIndex the index of the column
         * @return the common {@code ColumnStyle} of the object values for the column
         */
        @Override
        public ColumnStyle getColumnStyle(int columnIndex) {
            return columnStyles[columnIndex];
        }

        /**
         * Returns the column class for the values in the column.
         *
         * @param columnIndex the index of the column
         * @return the common  class of the object values in the model.
         */
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 3 || columnIndex == 4) { // group column
                return BigDecimal.class;
            }

            return String.class;
        }

        /**
         * Returns the column count in the model.
         *
         * @return the number of columns in the model
         * @see #getRowCount
         */
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * Returns column name at {@code columnIndex}.
         *
         * @param columnIndex the index of the column
         * @return the name of the column
         */
        @Override
        public String getColumnName(int columnIndex) {
            return columnNames[columnIndex];
        }

        /**
         * Returns the row count in the model.
         *
         * @return the number of rows in the model
         * @see #getColumnCount
         */
        @Override
        public int getRowCount() {
            return accountList.size();
        }

        /**
         * Returns the value at {@code columnIndex} and {@code rowIndex}.
         *
         * @param rowIndex    the row whose value is to be queried
         * @param columnIndex the column whose value is to be queried
         * @return the value Object at the specified cell
         */
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {

            final Account account= accountList.get(rowIndex);

            switch (columnIndex) {
                case 0:
                    return SPACE.repeat((account.getDepth() - 1) * INDENT) + account.getName();
                case 1:
                    return String.valueOf(account.getAccountCode());
                case 2:
                    return String.valueOf(account.getTransactionCount());
                case 3:
                    return account.getTreeBalance(LocalDate.now(), currencyNode);
                case 4:
                    return account.getReconciledTreeBalance();
                case 5:
                    return account.getCurrencyNode().getSymbol();
                case 6:
                    return account.getAccountType().toString();
                default:
                    return "";
            }
        }

        @Override
        public boolean isColumnFixedWidth(final int columnIndex) {
            return columnIndex != 0;
        }

        /**
         * Returns the title for the Report
         *
         * @return the report title
         */
        @Override
        public String getTitle() {
            return ResourceUtils.getString("Title.ListOfAccounts");
        }

        /**
         * Returns the subtitle for the Report
         *
         * @return the report title
         */
        @Override
        public String getSubTitle() {
            final MessageFormat format = new MessageFormat(rb.getString("Pattern.Date"));
            return format.format(new Object[]{DateUtils.asDate(LocalDate.now())});
        }
    }
}
