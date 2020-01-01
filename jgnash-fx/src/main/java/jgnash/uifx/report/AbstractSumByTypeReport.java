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
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.Comparators;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.MathConstants;
import jgnash.report.pdf.Report;
import jgnash.report.table.AbstractReportTableModel;
import jgnash.report.table.ColumnStyle;
import jgnash.report.table.Row;
import jgnash.report.table.SortOrder;
import jgnash.resource.util.ResourceUtils;
import jgnash.time.DateUtils;
import jgnash.time.Period;
import jgnash.util.NotNull;

/**
 * Abstract Report that groups and sums by {@code AccountGroup} and has a line for a global sum. and cross tabulates
 * all rows.
 *
 * @author Craig Cavanaugh
 * @author Michael Mueller
 * @author David Robertson
 * @author Aleksey Trufanov
 * @author Vincent Frison
 * @author Klemen Zagar
 */
public abstract class AbstractSumByTypeReport extends Report {

    private boolean runningTotal = true;

    final ArrayList<LocalDate> startDates = new ArrayList<>();

    final ArrayList<LocalDate> endDates = new ArrayList<>();

    private final ArrayList<String> dateLabels = new ArrayList<>();

    private final Map<Account, BigDecimal> percentileMap = new HashMap<>();

    private boolean addCrossTabColumn = false;

    private boolean addPercentileColumn = false;

    private String subTitle = "";

    private String title = "";

    private boolean showFullAccountPath = false;

    private SortOrder sortOrder = SortOrder.BY_NAME;

    private Period reportPeriod = Period.MONTHLY;

    /**
     * Returns a list of AccountGroup that will be reported on
     *
     * @return List of AccountGroup
     */
    @NotNull
    protected abstract List<AccountGroup> getAccountGroups();

    /**
     * Returns the reporting period
     *
     * @return returns a Monthly period unless overridden
     */
    private Period getReportPeriod() {
        return reportPeriod;
    }

    void setReportPeriod(final Period reportPeriod) {
        this.reportPeriod = reportPeriod;
    }

    void setSortOrder(@NotNull final SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    ReportModel createReportModel(final LocalDate startDate, final LocalDate endDate,
                                  final boolean hideZeroBalanceAccounts) {

        percentileMap.clear();

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        // update the subtitle
        final MessageFormat format = new MessageFormat(rb.getString("Pattern.DateRange"));
        subTitle = format.format(new Object[]{DateUtils.asDate(startDate), DateUtils.asDate(endDate)});

        // generate the required date and label arrays
        updateResolution(startDate, endDate);

        final CurrencyNode baseCurrency = engine.getDefaultCurrency();

        List<Account> accounts = new ArrayList<>();

        for (final AccountGroup group : getAccountGroups()) {
            accounts.addAll(getAccountList(AccountType.getAccountTypes(group)));
        }

        // remove any account that will report a zero balance for all periods
        if (hideZeroBalanceAccounts) {
            final Iterator<Account> i = accounts.iterator();

            while (i.hasNext()) {
                final Account account = i.next();
                boolean remove = true;

                if (runningTotal) {
                    for (final LocalDate date : startDates) {
                        if (account.getBalance(date).compareTo(BigDecimal.ZERO) != 0) {
                            remove = false;
                            break;
                        }
                    }

                    for (final LocalDate date : endDates) {
                        if (account.getBalance(date).compareTo(BigDecimal.ZERO) != 0) {
                            remove = false;
                            break;
                        }
                    }


                } else {
                    for (int j = 0; j < startDates.size(); j++) {
                        if (account.getBalance(startDates.get(j), endDates.get(j)).compareTo(BigDecimal.ZERO) != 0) {
                            remove = false;
                            break;
                        }
                    }
                }
                if (remove) {
                    i.remove();
                }
            }
        }

        switch (sortOrder) {    // sort the accounts
            case BY_NAME:
                accounts.sort(showFullAccountPath ? Comparators.getAccountByPathName() : Comparators.getAccountByName());
                break;
            case BY_BALANCE:
                accounts.sort(Comparators.getAccountByBalance(startDate, endDate, baseCurrency, true));
                break;
            default:
                accounts.sort(Comparators.getAccountByName());
        }

        // cross tabulate account percentages by group
        if (addPercentileColumn) {
            for (final AccountGroup group : getAccountGroups()) {

                // sum the group
                BigDecimal groupTotal = BigDecimal.ZERO;
                for (final Account a : accounts) {
                    if (a.getAccountType().getAccountGroup() == group) {
                        groupTotal = groupTotal.add(a.getBalance(startDate, endDate, baseCurrency));
                    }
                }

                // calculate the percentage
                for (final Account a : accounts) {
                    if (a.getAccountType().getAccountGroup() == group) {

                        BigDecimal sum = a.getBalance(startDate, endDate, baseCurrency);
                        percentileMap.put(a, sum.divide(groupTotal, MathConstants.mathContext));
                    }
                }
            }
        }

        final ReportModel model = new ReportModel(baseCurrency);
        model.addAccounts(accounts);

        return model;
    }

    private void updateResolution(final LocalDate startDate, final LocalDate endDate) {

        final DateTimeFormatter dateFormat = DateUtils.getShortDateFormatter();


        startDates.clear();
        endDates.clear();
        dateLabels.clear();

        LocalDate start = startDate;
        LocalDate end = startDate;

        switch (getReportPeriod()) {
            case YEARLY:
                while (start.isBefore(endDate)) {
                    startDates.add(start);
                    end = DateUtils.getLastDayOfTheYear(start);
                    endDates.add(end);
                    dateLabels.add(String.valueOf(start.getYear()));
                    start = end.plusDays(1);
                }
                break;
            case QUARTERLY:
                int i = DateUtils.getQuarterNumber(start) - 1;
                while (end.isBefore(endDate)) {
                    startDates.add(start);
                    end = DateUtils.getLastDayOfTheQuarter(start);
                    endDates.add(end);
                    dateLabels.add(start.getYear() + "-Q" + (1 + i++ % 4));
                    start = end.plusDays(1);
                }
                break;
            case MONTHLY:   // default is monthly
            default:
                endDates.addAll(DateUtils.getLastDayOfTheMonths(startDate, endDate));
                startDates.addAll(DateUtils.getFirstDayOfTheMonths(startDate, endDate));

                startDates.set(0, startDate);   // force the start date

                if (runningTotal) {
                    for (final LocalDate date : endDates) {
                        dateLabels.add(dateFormat.format(date));
                    }
                } else {
                    for (int j = 0; j < startDates.size(); j++) {
                        dateLabels.add(dateFormat.format(startDates.get(j)) + " - " + dateFormat.format(endDates.get(j)));
                    }
                }

                break;
        }

        assert startDates.size() == endDates.size() && startDates.size() == dateLabels.size();

        // adjust label for global end date
        if (endDates.get(startDates.size() - 1).compareTo(endDate) > 0) {
            endDates.set(endDates.size() - 1, endDate);
        }
    }

    private static List<Account> getAccountList(final Set<AccountType> types) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        return engine.getAccountList().stream().
                filter(a -> types.contains(a.getAccountType())).distinct().sorted().collect(Collectors.toList());
    }

    void setRunningTotal(final boolean runningTotal) {
        this.runningTotal = runningTotal;
    }

    void setAddCrossTabColumn(final boolean addCrossTabColumn) {
        this.addCrossTabColumn = addCrossTabColumn;
    }

    void setAddPercentileColumn(final boolean addPercentileColumn) {
        this.addPercentileColumn = addPercentileColumn;
    }

    private boolean isShowFullAccountPath() {
        return showFullAccountPath;
    }

    void setShowFullAccountPath(boolean showFullAccountPath) {
        this.showFullAccountPath = showFullAccountPath;
    }

    protected class ReportModel extends AbstractReportTableModel {

        private final List<Row<?>> rowList = new ArrayList<>();

        private final CurrencyNode baseCurrency;

        private final ResourceBundle rb = ResourceUtils.getBundle();

        ReportModel(final CurrencyNode currency) {
            this.baseCurrency = currency;
        }

        @Override
        public String getTitle() {
            return AbstractSumByTypeReport.this.title;
        }

        @Override
        public String getSubTitle() {
            return AbstractSumByTypeReport.this.subTitle;
        }

        /**
         * Returns the legend for the grand total
         *
         * @return report name
         */
        @Override
        public String getGrandTotalLegend() {
            return AbstractSumByTypeReport.this.getGrandTotalLegend();
        }

        /**
         * Returns the general label for the group footer
         *
         * @return footer label
         */
        public String getGroupFooterLabel() {
            return AbstractSumByTypeReport.this.getGroupFooterLabel();
        }

        void addAccounts(final Collection<Account> accounts) {
            accounts.forEach(this::addAccount);
        }

        /**
         * Supports manual addition of a report row
         *
         * @param row the Row to add
         */
        void addRow(final Row<?> row) {
            rowList.add(row);
        }

        void addAccount(final Account account) {
            rowList.add(new AccountRow(account));
        }

        @Override
        public int getRowCount() {
            return rowList.size();
        }

        @Override
        public boolean isColumnFixedWidth(final int columnIndex) {
            return columnIndex != 0;    // fixed width if not the account column
        }

        /**
         * Returns the number of additional columns added by report options
         *
         * @return extra column count
         */
        private int getExtraColumnCount() {
            return (addPercentileColumn ? 1 : 0) + (addCrossTabColumn ? 1 : 0);
        }

        @Override
        public int getColumnCount() {
            return startDates.size() + 2 + getExtraColumnCount();
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            if (!rowList.isEmpty()) {
                return rowList.get(rowIndex).getValueAt(columnIndex);
            }

            return null;
        }

        @Override
        public CurrencyNode getCurrencyNode() {
            return baseCurrency;
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            if (columnIndex == 0 || columnIndex == getColumnCount() - 1) { // accounts and group column
                return String.class;
            }

            return BigDecimal.class;
        }

        @Override
        public String getColumnName(final int columnIndex) {

            if (isCrossTabColumn(columnIndex)) {
                return "";
            }

            if (isPercentileColumn(columnIndex)) {
                return rb.getString("Column.Percentile");
            }

            if (columnIndex == 0) {
                return rb.getString("Column.Account");
            } else if (columnIndex == getColumnCount() - 1) {   // type / group
                return "Type";
            }

            return dateLabels.get(columnIndex - 1);
        }

        @Override
        public ColumnStyle getColumnStyle(final int columnIndex) {

            if (isPercentileColumn(columnIndex)) {
                return ColumnStyle.PERCENTAGE;
            }

            if (columnIndex == 0) { // accounts column
                return ColumnStyle.STRING;
            } else if (columnIndex == getColumnCount() - 1) { // group column
                return ColumnStyle.GROUP;
            }
            return ColumnStyle.BALANCE_WITH_SUM_AND_GLOBAL;
        }

        private boolean isPercentileColumn(final int columnIndex) {
            if (addPercentileColumn) {
                return columnIndex == getColumnCount() - 2; // last column
            }
            return false;
        }

        private boolean isCrossTabColumn(final int columnIndex) {
            if (addCrossTabColumn && addPercentileColumn) {
                return columnIndex == getColumnCount() - 3; // 2nd to last column
            } else if (addCrossTabColumn) {
                return columnIndex == getColumnCount() - 2; // last column when percentages are not displayed
            }

            return false;
        }

        private class AccountRow extends Row<Account> {

            AccountRow(final Account account) {
                super(account);
            }

            @Override
            public Object getValueAt(final int columnIndex) {

                // check for cross tabulation column and do the math
                if (isCrossTabColumn(columnIndex)) {
                    BigDecimal sum = BigDecimal.ZERO;

                    for (int i = 1; i < getColumnCount() - 1 - getExtraColumnCount(); i++) {
                        sum = sum.add((BigDecimal) getValueAt(i));
                    }
                    return sum;
                }

                if (isPercentileColumn(columnIndex)) {
                   return percentileMap.get(getValue());
                }

                if (columnIndex == 0) { // account column
                    return isShowFullAccountPath() ? getValue().getPathName() : getValue().getName();
                } else if (columnIndex == getColumnCount() - 1) { // group column
                    return getValue().getAccountType().getAccountGroup().toString();
                } else if (columnIndex > 0 && columnIndex <= startDates.size()) {
                    if (runningTotal) {
                        return getValue().getBalance(endDates.get(columnIndex - 1), getCurrencyNode());
                    }

                    return getValue().getBalance(startDates.get(columnIndex - 1), endDates.get(columnIndex - 1),
                            getCurrencyNode()).negate();
                }

                return null;
            }
        }
    }
}
