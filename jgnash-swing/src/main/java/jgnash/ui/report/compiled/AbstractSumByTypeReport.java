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
package jgnash.ui.report.compiled;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import net.sf.jasperreports.engine.JasperPrint;

import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.ui.components.DatePanel;
import jgnash.ui.report.AbstractReportTableModel;
import jgnash.ui.report.ColumnHeaderStyle;
import jgnash.ui.report.ColumnStyle;
import jgnash.ui.report.jasper.DynamicJasperReport;
import jgnash.util.DateUtils;
import jgnash.util.Resource;

/**
 * Abstract Report that groups and sums by {@code AccountGroup} and has a
 * line for a global sum
 * 
 * @author Craig Cavanaugh
 */
abstract class AbstractSumByTypeReport extends DynamicJasperReport {

    private final DatePanel startDateField;

    private final DatePanel endDateField;

    private final JButton refreshButton;

    private final JCheckBox hideZeroBalanceAccounts;

    private static final String HIDE_ZERO_BALANCE = "hideZeroBalance";

    private static final String MONTHS = "months";

    boolean runningTotal = true;

    List<Date> dates = Collections.emptyList();

    AbstractSumByTypeReport() {

        Preferences p = getPreferences();

        int months = p.getInt(MONTHS, 5);

        Date startDate = new Date();

        for (int i = 0; i < months - 1; i++) {
            startDate = DateUtils.subtractMonth(startDate);
        }

        startDateField = new DatePanel();
        endDateField = new DatePanel();

        hideZeroBalanceAccounts = new JCheckBox(Resource.get().getString("Button.HideZeroBalance"));
        hideZeroBalanceAccounts.setSelected(p.getBoolean(HIDE_ZERO_BALANCE, true));

        startDateField.setDate(startDate);

        refreshButton = new JButton(rb.getString("Button.Refresh"),
                Resource.getIcon("/jgnash/resource/view-refresh.png"));

        refreshButton.addActionListener(new AbstractAction() {

            @Override
            public void actionPerformed(final ActionEvent ae) {
                refreshReport();
            }
        });
    }

    @Override
    protected void refreshReport() {
        Preferences p = getPreferences();

        p.putBoolean(HIDE_ZERO_BALANCE, hideZeroBalanceAccounts.isSelected());
        p.putInt(MONTHS, DateUtils.getLastDayOfTheMonths(startDateField.getDate(), endDateField.getDate()).size());

        super.refreshReport();
    }

    protected abstract List<AccountGroup> getAccountGroups();

    ReportModel createReportModel(final Date startDate, final Date endDate) {

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
                    for (Date date : dates) {
                        if (account.getBalance(date).compareTo(BigDecimal.ZERO) != 0) {
                            remove = false;
                            break;
                        }
                    }
                } else {
                    for (int j = 0; j < dates.size() - 1; j++) {
                        Date sDate = dates.get(j);
                        Date eDate = DateUtils.subtractDay(dates.get(j+1));
                        
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

        Set<Account> accountSet = new TreeSet<>();

        for (Account a : engine.getAccountList()) {
            if (types.contains(a.getAccountType())) {
                accountSet.add(a);
            }
        }

        return new ArrayList<>(accountSet);
    }

    /**
     * Creates a JasperPrint object
     * 
     * @return JasperPrint
     */
    @Override
    public JasperPrint createJasperPrint(final boolean formatForCSV) {
        Date endDate = endDateField.getDate();
        Date startDate = startDateField.getDate();

        ReportModel model = createReportModel(startDate, endDate);

        return createJasperPrint(model, formatForCSV);
    }

    /**
     * Creates a report control panel. May return null if a panel is not used
     * 
     * @return control panel
     */
    @Override
    public JPanel getReportController() {
        FormLayout layout = new FormLayout("p, $lcgap, max(p;55dlu), 8dlu, p, $lcgap, max(p;55dlu), 8dlu, p", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.border(Borders.DIALOG);
        builder.append(rb.getString("Label.StartDate"), startDateField);
        builder.append(rb.getString("Label.EndDate"), endDateField);

        builder.append(refreshButton);

        builder.nextLine();
        builder.append(hideZeroBalanceAccounts, 9);

        return builder.getPanel();
    }

    /**
     * Wraps a row of table data into one object
     */
    static abstract class Row {
        /**
         * Returns the value given a column index
         * 
         * @param columnIndex
         *            column index
         * @return column value
         */
        public abstract Object getValueAt(final int columnIndex);
    }

    protected class ReportModel extends AbstractReportTableModel {

        private final List<Row> rowList = new ArrayList<>();

        private final CurrencyNode baseCurrency;

        private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);

        private final Resource rb = Resource.get();

        public ReportModel(final CurrencyNode currency) {
            this.baseCurrency = currency;
        }

        public void addAccounts(final Collection<Account> accounts) {
            for (Account account : accounts) {
                addAccount(account);
            }
        }

        public void addRow(final Row row) {
            rowList.add(row);
        }

        public void addAccount(final Account account) {
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
            } else {
                return dates.size() - 1 + 2;
            }
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

        /**
         * @see javax.swing.table.TableModel#getColumnClass(int)
         */
        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            if (columnIndex == 0) { // accounts column
                return String.class;
            } else if (columnIndex == getColumnCount() - 1) { // group column
                return String.class;
            }
            return BigDecimal.class;
        }

        /**
         * @see javax.swing.table.TableModel#getColumnName(int)
         */
        @Override
        public String getColumnName(final int columnIndex) {
            if (columnIndex == 0) {
                return rb.getString("Column.Account");
            } else if (columnIndex == getColumnCount() - 1) {
                return "Type";
            }

            if (runningTotal) {
                return dateFormat.format(dates.get(columnIndex - 1));
            } else {
                Date startDate = dates.get(columnIndex - 1);
                Date endDate = DateUtils.subtractDay(dates.get(columnIndex));

                return dateFormat.format(startDate) + " - " + dateFormat.format(endDate);
            }
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

        private class AccountRow extends Row {

            final Account account;

            public AccountRow(final Account account) {
                this.account = account;
            }

            @Override
            public Object getValueAt(final int columnIndex) {

                if (columnIndex == 0) { // account column
                    return account.getName();
                } else if (columnIndex == getColumnCount() - 1) { // group column
                    return account.getAccountType().getAccountGroup().toString();
                } else if (columnIndex > 0 && columnIndex <= dates.size()) {
                    if (runningTotal) {
                        return account.getBalance(dates.get(columnIndex - 1), getCurrency());
                    } else {
                        Date startDate = dates.get(columnIndex - 1);
                        Date endDate = DateUtils.subtractDay(dates.get(columnIndex));

                        return account.getBalance(startDate, endDate, getCurrency()).negate();
                    }
                }
                return null;
            }
        }
    }
}