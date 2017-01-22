/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.prefs.Preferences;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.Comparators;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.ui.report.jasper.AbstractReportTableModel;
import jgnash.ui.report.jasper.ColumnHeaderStyle;
import jgnash.ui.report.jasper.ColumnStyle;
import jgnash.uifx.control.DatePickerEx;
import jgnash.time.DateUtils;

import net.sf.jasperreports.engine.JasperPrint;

/**
 * Abstract Report that groups and sums by {@code AccountGroup}, has a line for a global sum, and cross tabulates
 * all rows.
 *
 * @author Craig Cavanaugh
 * @author Michael Mueller
 * @author David Robertson
 * @author Aleksey Trufanov
 * @author Vincent Frison
 * @author Klemen Zagar
 */
public abstract class AbstractCrosstabReport extends DynamicJasperReport {

    // ----- Resolution constants
    private final String RES_YEAR = rb.getString("Word.Yearly");

    private final String RES_QUARTER = rb.getString("Word.Quarterly");

    private final String RES_MONTH = rb.getString("Word.Monthly");

    // ----- Sort order constants

    private final String SORT_ORDER_NAME = rb.getString("SortOrder.AccountName");

    private final String SORT_ORDER_BALANCE_DESC = rb.getString("SortOrder.AccountBalanceDesc");

    private final String SORT_ORDER_BALANCE_DESC_WITH_PERCENTILE = rb.getString("SortOrder.AccountBalanceDescWithPercentile");

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    private CheckBox hideZeroBalanceAccounts;

    @FXML
    private ComboBox<String> resolutionComboBox;

    @FXML
    private ComboBox<String> sortOrderComboBox;

    @FXML
    private CheckBox showLongNamesCheckBox;

    private final Map<Account, Double> percentileMap = new HashMap<>();

    /*
     * Report Data
     */
    private final ArrayList<LocalDate> startDates = new ArrayList<>();

    private final ArrayList<LocalDate> endDates = new ArrayList<>();

    private final ArrayList<String> dateLabels = new ArrayList<>();

    private static final String HIDE_ZERO_BALANCE = "hideZeroBalance";

    private static final String MONTHS = "months";

    private static final String USE_LONG_NAMES = "useLongNames";

    @FXML
    private void initialize() {
        final Preferences preferences = getPreferences();

        startDatePicker.setValue(LocalDate.now().minusYears(1));

        startDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> handleRefresh());

        endDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> handleRefresh());

        hideZeroBalanceAccounts.setSelected(preferences.getBoolean(HIDE_ZERO_BALANCE, true));
        showLongNamesCheckBox.setSelected(preferences.getBoolean(USE_LONG_NAMES, false));

        resolutionComboBox.getItems().addAll(RES_YEAR, RES_QUARTER, RES_MONTH);
        resolutionComboBox.setValue(RES_QUARTER);

        sortOrderComboBox.getItems().addAll(SORT_ORDER_NAME, SORT_ORDER_BALANCE_DESC,
                SORT_ORDER_BALANCE_DESC_WITH_PERCENTILE);

        sortOrderComboBox.setValue(SORT_ORDER_NAME);
    }

    /**
     * Returns the subtitle for the report.
     *
     * @return subtitle
     */
    @Override
    public String getSubTitle() {
        final MessageFormat format = new MessageFormat(rb.getString("Pattern.DateRange"));

        return format.format(new Object[]{DateUtils.asDate(startDates.get(0)),
                DateUtils.asDate(endDates.get(endDates.size() - 1))});
    }

    @FXML
    private void handleRefresh() {
        final Preferences preferences = getPreferences();

        preferences.putBoolean(HIDE_ZERO_BALANCE, hideZeroBalanceAccounts.isSelected());
        preferences.putBoolean(USE_LONG_NAMES, showLongNamesCheckBox.isSelected());
        preferences.putInt(MONTHS, DateUtils.getLastDayOfTheMonths(startDatePicker.getValue(), endDatePicker.getValue()).size());

        if (refreshCallBackProperty().get() != null) {
            refreshCallBackProperty().get().run();
        }
    }

    protected abstract List<AccountGroup> getAccountGroups();

    private void updateResolution() {
        startDates.clear();
        endDates.clear();
        dateLabels.clear();

        final String currentResolution = resolutionComboBox.getValue();

        final LocalDate globalStart = startDatePicker.getValue();
        final LocalDate globalEnd = endDatePicker.getValue();

        LocalDate start = globalStart;
        LocalDate end = globalStart;

        if (RES_YEAR.equals(currentResolution)) {
            while (end.isBefore(globalEnd)) {
                startDates.add(start);
                end = end.with(TemporalAdjusters.lastDayOfYear());
                endDates.add(end);
                dateLabels.add("    " + start.getYear());

                start = end.plusDays(1);
            }
        } else if (RES_QUARTER.equals(currentResolution)) {
            int i = DateUtils.getQuarterNumber(start) - 1;
            while (end.isBefore(globalEnd)) {
                startDates.add(start);
                end = DateUtils.getLastDayOfTheQuarter(start);
                endDates.add(end);
                dateLabels.add(" " + start.getYear() + "-Q" + (1 + i++ % 4));
                start = end.plusDays(1);
            }
        } else if (RES_MONTH.equals(currentResolution)) {
            while (end.isBefore(globalEnd)) {
                startDates.add(start);
                end = DateUtils.getLastDayOfTheMonth(start);
                endDates.add(end);
                int month = start.getMonthValue();
                dateLabels.add(" " + start.getYear() + (month < 10 ? "/0" + month : "/" + month));
                start = end.plusDays(1);
            }
        }

        assert startDates.size() == endDates.size() && startDates.size() == dateLabels.size();

        // adjust label for global end date
        if (endDates.get(startDates.size() - 1).compareTo(globalEnd) > 0) {
            endDates.set(endDates.size() - 1, globalEnd);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private ReportModel createTableModel() {
        logger.info(rb.getString("Message.CollectingReportData"));

        final CurrencyNode baseCurrency = EngineFactory.getEngine(EngineFactory.DEFAULT).getDefaultCurrency();

        final List<Account> accounts = new ArrayList<>();

        final String sortOrder = sortOrderComboBox.getValue();
        final boolean needPercentiles = SORT_ORDER_BALANCE_DESC_WITH_PERCENTILE.equals(sortOrder);

        for (final AccountGroup group : getAccountGroups()) {
            List<Account> list = getAccountList(AccountType.getAccountTypes(group));

            boolean ascendingSortOrder = true;
            if (!list.isEmpty()) {
                if (list.get(0).getAccountType() == AccountType.EXPENSE) {
                    ascendingSortOrder = false;
                }
            }

            if (SORT_ORDER_NAME.equals(sortOrder)) {
                if (!showLongNamesCheckBox.isSelected()) {
                    list.sort(Comparators.getAccountByName());
                } else {
                    list.sort(Comparators.getAccountByPathName());
                }
            } else if (SORT_ORDER_BALANCE_DESC.equals(sortOrder) || SORT_ORDER_BALANCE_DESC_WITH_PERCENTILE.equals(sortOrder)) {
                list.sort(Comparators.getAccountByBalance(startDatePicker.getValue(),
                        endDatePicker.getValue(), baseCurrency, ascendingSortOrder));
            }

            if (needPercentiles) {
                BigDecimal groupTotal = BigDecimal.ZERO;
                for (final Account a : list) {
                    groupTotal = groupTotal.add(a.getBalance(startDatePicker.getValue(), endDatePicker.getValue(), baseCurrency));
                }
                BigDecimal sumSoFar = BigDecimal.ZERO;
                for (final Account a : list) {
                    sumSoFar = sumSoFar.add(a.getBalance(startDatePicker.getValue(), endDatePicker.getValue(), baseCurrency));
                    percentileMap.put(a, sumSoFar.doubleValue() / groupTotal.doubleValue());
                }
            }

            accounts.addAll(list);
        }

        updateResolution();

        // remove any account that will report a zero balance for all periods
        if (hideZeroBalanceAccounts.isSelected()) {
            Iterator<Account> i = accounts.iterator();
            while (i.hasNext()) {
                Account account = i.next();
                boolean remove = true;

                for (int j = 0; j < endDates.size(); j++) {
                    if (account.getBalance(startDates.get(j), endDates.get(j)).compareTo(BigDecimal.ZERO) != 0) {
                        remove = false;
                        break;
                    }
                }

                if (remove) {
                    i.remove();
                }
            }
        }

        // configure columns
        List<ColumnInfo> columnsList = new LinkedList<>();

        // accounts column
        ColumnInfo ci = new AccountNameColumnInfo(accounts);
        ci.columnName = rb.getString("Column.Account");
        ci.headerStyle = ColumnHeaderStyle.LEFT;
        ci.columnClass = String.class;
        ci.columnStyle = ColumnStyle.STRING;
        ci.isFixedWidth = false;
        columnsList.add(ci);

        for (int i = 0; i < dateLabels.size(); ++i) {
            ci = new DateRangeBalanceColumnInfo(accounts, startDates.get(i), endDates.get(i), baseCurrency);
            ci.columnName = dateLabels.get(i);
            ci.headerStyle = ColumnHeaderStyle.RIGHT;
            ci.columnClass = BigDecimal.class;
            ci.columnStyle = ColumnStyle.BALANCE_WITH_SUM_AND_GLOBAL;
            ci.isFixedWidth = true;
            columnsList.add(ci);
        }

        // cross-tab total column
        ci = new CrossTabAmountColumnInfo(accounts, baseCurrency);
        ci.columnName = "";
        ci.headerStyle = ColumnHeaderStyle.RIGHT;
        ci.columnClass = BigDecimal.class;
        ci.columnStyle = ColumnStyle.CROSSTAB_TOTAL;
        ci.isFixedWidth = true;
        columnsList.add(ci);

        if (needPercentiles) {
            ci = new PercentileColumnInfo(accounts);
            ci.columnName = "Percentile";
            ci.headerStyle = ColumnHeaderStyle.RIGHT;
            ci.columnClass = String.class;
            ci.columnStyle = ColumnStyle.CROSSTAB_TOTAL;
            ci.isFixedWidth = true;
            columnsList.add(ci);
        }

        // grouping column (last column)
        ci = new GroupColumnInfo(accounts);
        ci.columnName = "Type";
        ci.headerStyle = ColumnHeaderStyle.CENTER;
        ci.columnClass = String.class;
        ci.columnStyle = ColumnStyle.GROUP;
        ci.isFixedWidth = false;
        columnsList.add(ci);

        columns = columnsList.toArray(new ColumnInfo[columnsList.size()]);

        return new ReportModel(accounts, baseCurrency);
    }

    private static List<Account> getAccountList(final Set<AccountType> types) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        Objects.requireNonNull(engine);

        List<Account> accounts = engine.getAccountList();
        Iterator<Account> i = accounts.iterator();

        search:
        while (i.hasNext()) {
            Account a = i.next();

            if (a.getTransactionCount() == 0) {
                i.remove();
            } else {
                for (AccountType t : types) {
                    if (a.getAccountType() == t) {
                        continue search;
                    }
                }
                i.remove(); // made it here.. remove it
            }
        }
        return accounts;
    }

    /**
     * Creates a JasperPrint object.
     *
     * @return JasperPrint
     */
    @Override
    public JasperPrint createJasperPrint(final boolean formatForCSV) {
        return createJasperPrint(createTableModel(), formatForCSV);
    }

    private ColumnInfo[] columns;

    private class ReportModel extends AbstractReportTableModel {

        private final CurrencyNode baseCurrency;

        private List<Account> accountList = Collections.emptyList();

        ReportModel(final List<Account> accountList, final CurrencyNode currency) {
            this.accountList = accountList;
            this.baseCurrency = currency;
        }

        @Override
        public CurrencyNode getCurrency() {
            return baseCurrency;
        }

        @Override
        public int getRowCount() {
            return accountList.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return columns[columnIndex].columnName;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columns[columnIndex].columnClass;
        }

        @Override
        public ColumnStyle getColumnStyle(int columnIndex) {
            return columns[columnIndex].columnStyle;
        }

        @Override
        public ColumnHeaderStyle getColumnHeaderStyle(int columnIndex) {
            return columns[columnIndex].headerStyle;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return columns[columnIndex].getValue(rowIndex);
        }

        @Override
        public boolean isColumnFixedWidth(final int columnIndex) {
            return columns[columnIndex].isFixedWidth;
        }
    }

    private static abstract class ColumnInfo {

        public abstract Object getValue(int rowIndex);

        ColumnHeaderStyle headerStyle;

        ColumnStyle columnStyle;

        Class<?> columnClass;

        String columnName;

        boolean isFixedWidth;
    }

    private class AccountNameColumnInfo extends ColumnInfo {

        private final List<Account> accountList;

        AccountNameColumnInfo(List<Account> accountList) {
            this.accountList = accountList;
        }

        @Override
        public Object getValue(int rowIndex) {
            Account a = accountList.get(rowIndex);
            if (showLongNamesCheckBox.isSelected()) {
                return a.getPathName();
            }
            return a.getName();
        }
    }

    private class CrossTabAmountColumnInfo extends ColumnInfo {

        private final List<Account> accountList;

        private final CurrencyNode currency;

        CrossTabAmountColumnInfo(List<Account> accountList, CurrencyNode currency) {
            this.accountList = accountList;
            this.currency = currency;
        }

        @Override
        public Object getValue(int rowIndex) {
            final Account a = accountList.get(rowIndex);

            final LocalDate startDate = startDates.get(0);
            final LocalDate endDate = endDates.get(endDates.size() - 1);

            return a.getBalance(startDate, endDate, currency).negate();
        }
    }

    private static class GroupColumnInfo extends ColumnInfo {

        private final List<Account> accountList;

        GroupColumnInfo(List<Account> accountList) {
            this.accountList = accountList;
        }

        @Override
        public Object getValue(int rowIndex) {
            Account a = accountList.get(rowIndex);
            return a.getAccountType().getAccountGroup().toString();
        }
    }

    private static class DateRangeBalanceColumnInfo extends ColumnInfo {

        private final List<Account> accountList;

        private final LocalDate startDate;

        private final LocalDate endDate;

        private final CurrencyNode currency;

        DateRangeBalanceColumnInfo(List<Account> accountList, LocalDate startDate, LocalDate endDate, CurrencyNode currency) {
            this.accountList = accountList;
            this.startDate = startDate;
            this.endDate = endDate;
            this.currency = currency;
        }

        @Override
        public Object getValue(int rowIndex) {
            Account a = accountList.get(rowIndex);
            return a.getBalance(startDate, endDate, currency).negate();
        }
    }

    private class PercentileColumnInfo extends ColumnInfo {

        private final List<Account> accounts;

        PercentileColumnInfo(List<Account> accounts) {
            this.accounts = accounts;
        }

        @Override
        public Object getValue(int rowIndex) {
            Double percentile = percentileMap.get(accounts.get(rowIndex));
            return String.format("%.2f%%", percentile * 100.0);
        }
    }
}
