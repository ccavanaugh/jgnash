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
package jgnash.ui.report.compiled;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;
import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.time.DateUtils;
import jgnash.ui.components.DatePanel;
import jgnash.ui.report.jasper.AbstractReportTableModel;
import jgnash.ui.report.jasper.ColumnHeaderStyle;
import jgnash.ui.report.jasper.ColumnStyle;
import jgnash.ui.report.jasper.DynamicJasperReport;
import jgnash.ui.util.IconUtils;
import jgnash.util.ResourceUtils;
import net.sf.jasperreports.engine.JasperPrint;

import javax.swing.*;
import java.awt.event.ActionEvent;
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

    List<LocalDate> dates = Collections.emptyList();

    AbstractSumByTypeReport() {

        Preferences p = getPreferences();

        startDateField = new DatePanel();
        endDateField = new DatePanel();

        hideZeroBalanceAccounts = new JCheckBox(ResourceUtils.getString("Button.HideZeroBalance"));
        hideZeroBalanceAccounts.setSelected(p.getBoolean(HIDE_ZERO_BALANCE, true));

        startDateField.setDate(LocalDate.now().minusMonths(p.getInt(MONTHS, 4)));

        refreshButton = new JButton(rb.getString("Button.Refresh"),
                IconUtils.getIcon("/jgnash/resource/view-refresh.png"));

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
        p.putInt(MONTHS, DateUtils.getLastDayOfTheMonths(startDateField.getLocalDate(), endDateField.getLocalDate()).size());

        super.refreshReport();
    }

    protected abstract List<AccountGroup> getAccountGroups();

    ReportModel createReportModel(final LocalDate startDate, final LocalDate endDate) {

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
     * Creates a JasperPrint object
     * 
     * @return JasperPrint
     */
    @Override
    public JasperPrint createJasperPrint(final boolean formatForCSV) {
        LocalDate endDate = endDateField.getLocalDate();
        LocalDate startDate = startDateField.getLocalDate();

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

    class ReportModel extends AbstractReportTableModel {

        private final List<Row> rowList = new ArrayList<>();

        private final CurrencyNode baseCurrency;

        private final DateTimeFormatter dateFormat = DateUtils.getShortDateFormatter();

        private final ResourceBundle rb = ResourceUtils.getBundle();

        ReportModel(final CurrencyNode currency) {
            this.baseCurrency = currency;
        }

        void addAccounts(final Collection<Account> accounts) {
            accounts.forEach(this::addAccount);
        }

        void addRow(final Row row) {
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
                LocalDate startDate = dates.get(columnIndex - 1);
                LocalDate endDate = dates.get(columnIndex).minusDays(1);

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

            AccountRow(final Account account) {
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
                        final LocalDate startDate = dates.get(columnIndex - 1);
                        final LocalDate endDate = dates.get(columnIndex).minusDays(1);

                        return account.getBalance(startDate, endDate, getCurrency()).negate();
                    }
                }
                return null;
            }
        }
    }
}