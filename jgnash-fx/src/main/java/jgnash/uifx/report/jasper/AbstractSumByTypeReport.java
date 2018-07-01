/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.time.DateUtils;
import jgnash.ui.report.Row;
import jgnash.ui.report.jasper.AbstractReportTableModel;
import jgnash.ui.report.jasper.ColumnHeaderStyle;
import jgnash.ui.report.jasper.ColumnStyle;
import jgnash.uifx.control.DatePickerEx;
import jgnash.util.ResourceUtils;
import net.sf.jasperreports.engine.JasperPrint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Abstract Report that groups and sums by {@code AccountGroup} and has a
 * line for a global sum.
 * 
 * @author Craig Cavanaugh
 */
public abstract class AbstractSumByTypeReport extends DynamicJasperReport {

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    private CheckBox hideZeroBalanceAccounts;

    private static final String HIDE_ZERO_BALANCE = "hideZeroBalance";

    private static final String MONTHS = "months";

    protected boolean runningTotal = true;

    protected List<LocalDate> dates = Collections.emptyList();

    @FXML
    private void initialize() {
        final Preferences preferences = getPreferences();

        hideZeroBalanceAccounts.setSelected(preferences.getBoolean(HIDE_ZERO_BALANCE, true));

        startDatePicker.setValue(LocalDate.now().minusMonths(preferences.getInt(MONTHS, 4)));

        startDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> handleRefresh());

        endDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> handleRefresh());
    }

    @FXML
    private void handleRefresh() {
        final Preferences preferences = getPreferences();

        preferences.putBoolean(HIDE_ZERO_BALANCE, hideZeroBalanceAccounts.isSelected());
        preferences.putInt(MONTHS, DateUtils.getLastDayOfTheMonths(startDatePicker.getValue(),
                endDatePicker.getValue()).size());

        if (refreshCallBackProperty().get() != null) {
            refreshCallBackProperty().get().run();
        }
    }

    protected abstract List<AccountGroup> getAccountGroups();

    protected ReportModel createReportModel(final LocalDate startDate, final LocalDate endDate) {

        logger.info(rb.getString("Message.CollectingReportData"));

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        // generate the date information
        if (runningTotal) {
            dates = DateUtils.getLastDayOfTheMonths(startDate, endDate);
        } else {
            dates = DateUtils.getFirstDayOfTheMonths(startDate, endDate);
            dates.set(0, startDate);

            if (DateUtils.after(endDate, dates.get(dates.size() - 1))) {
                dates.add(endDate);
            }
        }

        final CurrencyNode baseCurrency = engine.getDefaultCurrency();

        List<Account> accounts = new ArrayList<>();

        for (AccountGroup group : getAccountGroups()) {
            accounts.addAll(getAccountList(AccountType.getAccountTypes(group)));
        }

        // remove any account that will report a zero balance for all periods
        if (hideZeroBalanceAccounts.isSelected()) {            
            Iterator<Account> i = accounts.iterator();
            
            while (i.hasNext()) {
                Account account = i.next();
                boolean remove = true;

                if (runningTotal) {                    
                    for (LocalDate date : dates) {
                        if (account.getBalance(date).compareTo(BigDecimal.ZERO) != 0) {
                            remove = false;
                            break;
                        }
                    }
                } else {
                    for (int j = 0; j < dates.size() - 1; j++) {
                        final LocalDate sDate = dates.get(j);
                        final LocalDate eDate = dates.get(j+1).minusDays(1);
                        
                        if (account.getBalance(sDate, eDate).compareTo(BigDecimal.ZERO) != 0) {
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

        ReportModel model = new ReportModel(baseCurrency);
        model.addAccounts(accounts);

        return model;
    }

    private static List<Account> getAccountList(final Set<AccountType> types) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        return engine.getAccountList().stream().
                filter(a -> types.contains(a.getAccountType())).distinct().sorted().collect(Collectors.toList());
    }

    /**
     * Creates a JasperPrint object.
     * 
     * @return JasperPrint
     */
    @Override
    public JasperPrint createJasperPrint(final boolean formatForCSV) {
        return createJasperPrint(createReportModel(startDatePicker.getValue(), endDatePicker.getValue()), formatForCSV);
    }


    protected class ReportModel extends AbstractReportTableModel {

        private final List<Row<?>> rowList = new ArrayList<>();

        private final CurrencyNode baseCurrency;

        private final DateTimeFormatter dateFormat = DateUtils.getShortDateFormatter();

        private final ResourceBundle rb = ResourceUtils.getBundle();

        ReportModel(final CurrencyNode currency) {
            this.baseCurrency = currency;
        }

        void addAccounts(final Collection<Account> accounts) {
            accounts.forEach(this::addAccount);
        }

        public void addRow(final Row<?> row) {
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
        public int getColumnCount() {
            if (runningTotal) {
                return dates.size() + 2;
            }
            
			return dates.size() - 1 + 2;
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            if (!rowList.isEmpty()) {
                return rowList.get(rowIndex).getValueAt(columnIndex);
            }

            return null;
        }

        @Override
        public CurrencyNode getCurrency() {
            return baseCurrency;
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            if (columnIndex == 0) { // accounts column
                return String.class;
            } else if (columnIndex == getColumnCount() - 1) { // group column
                return String.class;
            }
            return BigDecimal.class;
        }

        @Override
        public String getColumnName(final int columnIndex) {
            if (columnIndex == 0) {
                return rb.getString("Column.Account");
            } else if (columnIndex == getColumnCount() - 1) {
                return "Type";
            }

            if (runningTotal) {
                return dateFormat.format(dates.get(columnIndex - 1));
            }
            
			LocalDate startDate = dates.get(columnIndex - 1);
			LocalDate endDate = dates.get(columnIndex).minusDays(1);

			return dateFormat.format(startDate) + " - " + dateFormat.format(endDate);
        }

        @Override
        public ColumnStyle getColumnStyle(final int columnIndex) {
            if (columnIndex == 0) { // accounts column
                return ColumnStyle.STRING;
            } else if (columnIndex == getColumnCount() - 1) { // group column
                return ColumnStyle.GROUP;
            }
            return ColumnStyle.BALANCE_WITH_SUM_AND_GLOBAL;
        }

        @Override
        public ColumnHeaderStyle getColumnHeaderStyle(final int columnIndex) {
            if (columnIndex == 0) { // accounts column
                return ColumnHeaderStyle.LEFT;
            } else if (columnIndex == getColumnCount() - 1) { // group column
                return ColumnHeaderStyle.CENTER;
            }
            return ColumnHeaderStyle.RIGHT;
        }

        private class AccountRow extends Row<Account> {

            AccountRow(final Account account) {
                super(account);
            }

            @Override
            public Object getValueAt(final int columnIndex) {

                if (columnIndex == 0) { // account column
                    return getValue().getName();
                } else if (columnIndex == getColumnCount() - 1) { // group column
                    return getValue().getAccountType().getAccountGroup().toString();
                } else if (columnIndex > 0 && columnIndex <= dates.size()) {
                    if (runningTotal) {
                        return getValue().getBalance(dates.get(columnIndex - 1), getCurrency());
                    }
                    
					final LocalDate startDate = dates.get(columnIndex - 1);
					final LocalDate endDate = dates.get(columnIndex).minusDays(1);

					return getValue().getBalance(startDate, endDate, getCurrency()).negate();
                }
                
                return null;
            }
        }
    }
}