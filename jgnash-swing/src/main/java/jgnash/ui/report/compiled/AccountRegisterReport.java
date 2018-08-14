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
package jgnash.ui.report.compiled;

import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import jgnash.engine.Account;
import jgnash.engine.CurrencyNode;
import jgnash.ui.components.AccountListComboBox;
import jgnash.ui.components.DatePanel;
import jgnash.ui.register.RegisterFactory;
import jgnash.ui.register.table.ClippingModel;
import jgnash.ui.register.table.FilterModel;
import jgnash.ui.register.table.FullCommodityStyle;
import jgnash.ui.register.table.QuantityStyle;
import jgnash.ui.register.table.RegisterModel;
import jgnash.ui.register.table.ShortCommodityStyle;
import jgnash.report.ui.jasper.AbstractReportTableModel;
import jgnash.report.ui.jasper.ColumnHeaderStyle;
import jgnash.report.ui.jasper.ColumnStyle;
import jgnash.ui.report.jasper.DynamicJasperReport;
import jgnash.ui.util.IconUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import net.sf.jasperreports.engine.JasperPrint;

/**
 * Account Register Report
 *
 * @author Craig Cavanaugh
 */
public class AccountRegisterReport extends DynamicJasperReport {

    private final AccountListComboBox accountCombo;

    private final JButton refreshButton;

    private final DatePanel startDateField;

    private final DatePanel endDateField;

    private final JCheckBox detailSplitsCheckBox;

    private static final String SHOW_DETAILS = "showDetails";

    private final JCheckBox filterCheckBox;

    private static final String FILTER_TAG = "filter:";

    private TextField txtFilter;

    private int startIndex;

    private int endIndex;

    public AccountRegisterReport(final Account account) {

        accountCombo = new AccountListComboBox();

        if (account != null) {
            accountCombo.setSelectedAccount(account);
        }

        Account a = accountCombo.getSelectedAccount();

        refreshButton = new JButton(rb.getString("Button.Refresh"));
        refreshButton.setIcon(IconUtils.getIcon("/jgnash/resource/view-refresh.png"));

        startDateField = new DatePanel();
        if (a.getTransactionCount() > 0) {
            startDateField.setDate(a.getTransactionAt(0).getLocalDate());
        }

        endDateField = new DatePanel();

        filterCheckBox = new JCheckBox(rb.getString("Button.Filter"));
        filterCheckBox.setSelected(getPreferences().getBoolean(FILTER_TAG, false));
        filterCheckBox.addActionListener(new AbstractAction() {

            @Override
            public void actionPerformed(final ActionEvent ae) {
                startDateField.setEnabled(!filterCheckBox.isSelected());
                endDateField.setEnabled(!filterCheckBox.isSelected());
                txtFilter.setEnabled(filterCheckBox.isSelected());
            }
        });

        txtFilter = new TextField(40);
        txtFilter.setEnabled(false);

        detailSplitsCheckBox = new JCheckBox(rb.getString("Button.DetailSplits"));
        detailSplitsCheckBox.setSelected(getPreferences().getBoolean(SHOW_DETAILS, false));
        detailSplitsCheckBox.addActionListener(new AbstractAction() {

            @Override
            public void actionPerformed(final ActionEvent ae) {
                getPreferences().putBoolean(SHOW_DETAILS, detailSplitsCheckBox.isSelected());
            }
        });

        refreshButton.addActionListener(new AbstractAction() {

            @Override
            public void actionPerformed(final ActionEvent ae) {
                refreshReport();
            }
        });
    }

    public AccountRegisterReport(final Account account, final int startIndex, final int endIndex) {
        this(account);
        this.startIndex = startIndex;
        this.endIndex = endIndex;

        try {
            startDateField.setDate(account.getTransactionAt(startIndex).getLocalDate());
            endDateField.setDate(account.getTransactionAt(endIndex).getLocalDate());
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Creates a JasperPrint object.
     *
     * @return JasperPrint object
     */
    @Override
    public JasperPrint createJasperPrint(final boolean formatForCSV) {

        logger.info(rb.getString("Message.CollectingReportData"));

        if (filterCheckBox.isSelected()) {
            FilterModel model = RegisterFactory.getFilterTableModel(accountCombo.getSelectedAccount(), detailSplitsCheckBox.isSelected());
            model.setFilter(txtFilter.getText());

            // last column is the balance.  It does not make sense if filtered, so hide it from the report.
            final String hideColName = model.getColumnName(model.getColumnCount() - 1);
            model.setColumnVisible(hideColName, false);

            return createJasperPrint(new ReportTableModel(model, true), formatForCSV);
        }

        ClippingModel model = RegisterFactory.getClippingTableModel(accountCombo.getSelectedAccount(), detailSplitsCheckBox.isSelected());

        if (endIndex > 0) { // start and end index specified... reset after model is created
            model.setStartIndex(startIndex);
            model.setEndIndex(endIndex);
            endIndex = 0;
            startIndex = 0;
        } else {
            model.setStartDate(startDateField.getLocalDate());
            model.setEndDate(endDateField.getLocalDate());
        }

        return createJasperPrint(new ReportTableModel(model, false), formatForCSV);
    }

    /**
     * Creates a report control panel.  May return null if a panel is not used
     * The ReportController is responsible for dynamic report options with the exception
     * of page format options
     *
     * @return control panel
     */
    @Override
    public JPanel getReportController() {
        FormLayout layout = new FormLayout("p, $lcgap, p:g, 8dlu, p, 8dlu, p", "f:d, $lgap, f:d");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.border(Borders.DIALOG);
        builder.append(rb.getString("Label.Account"), accountCombo);
        builder.append(refreshButton, 3);
        builder.nextLine();
        builder.nextLine();
        builder.append(createDatePanel(), 5);
        builder.append(detailSplitsCheckBox);
        builder.nextLine();
        builder.append(createFilterPanel(), 3);
        layout.addGroupedRow(1);
        layout.addGroupedRow(3);
        layout.addGroupedRow(5);

        return builder.getPanel();
    }

    private JPanel createDatePanel() {
        FormLayout layout = new FormLayout("p, $lcgap, max(48dlu;min), 8dlu, p, $lcgap, max(48dlu;min)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(rb.getString("Label.StartDate"), startDateField);
        builder.append(rb.getString("Label.EndDate"), endDateField);

        return builder.getPanel();
    }

    private JPanel createFilterPanel() {
        FormLayout layout = new FormLayout("p, $lcgap, p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(filterCheckBox, txtFilter);

        return builder.getPanel();
    }

    /**
     * Returns the name of the report
     *
     * @return report name
     */
    @Override
    public String getReportName() {
        return accountCombo.getSelectedAccount().getName();
    }

    /**
     * Returns the legend for the grand total
     *
     * @return report name
     */
    @Override
    public String getGrandTotalLegend() {
        return null;
    }

    /**
     * Returns the general label for the group footer
     *
     * @return footer label
     */
    @Override
    public String getGroupFooterLabel() {
        return rb.getString("Word.Totals");
    }

    private class ReportTableModel extends AbstractReportTableModel {

        private final RegisterModel model;

        /* if sumAmounts is true, any column with a ShortCommodityStyle class will be summed
         * at the end of the report
        */
        private final boolean sumAmounts;

        ReportTableModel(final RegisterModel model, final boolean sumAmounts) {
            this.model = model;
            this.sumAmounts = sumAmounts;
        }

        @Override
        public CurrencyNode getCurrency() {
            return accountCombo.getSelectedAccount().getCurrencyNode();
        }

        @Override
        public boolean isColumnFixedWidth(final int columnIndex) {
            return model.getPreferredColumnWeights()[columnIndex] == 0;
        }

        @Override
        public ColumnStyle getColumnStyle(final int columnIndex) {
            if (sumAmounts && columnIndex == model.getColumnCount()) {
                return ColumnStyle.GROUP_NO_HEADER;
            }

            if (model.getColumnClass(columnIndex).isAssignableFrom(LocalDate.class)) {
                return ColumnStyle.SHORT_DATE;
            } else if (model.getColumnClass(columnIndex).isAssignableFrom(FullCommodityStyle.class)) {
                return ColumnStyle.BALANCE;
            } else if (model.getColumnClass(columnIndex).isAssignableFrom(ShortCommodityStyle.class)) {
                if (sumAmounts) {
                    return ColumnStyle.AMOUNT_SUM;
                }
                return ColumnStyle.SHORT_AMOUNT;
            } else if (model.getColumnClass(columnIndex).isAssignableFrom(QuantityStyle.class)) {
                return ColumnStyle.QUANTITY;
            } else if (model.getColumnClass(columnIndex).isAssignableFrom(String.class)) {
                return ColumnStyle.STRING;
            } else if (model.getColumnClass(columnIndex).isAssignableFrom(BigDecimal.class)) {
                return ColumnStyle.SHORT_AMOUNT;
            }

            return ColumnStyle.STRING;
        }

        @Override
        public ColumnHeaderStyle getColumnHeaderStyle(final int columnIndex) {
            if (sumAmounts && columnIndex == model.getColumnCount()) {
                return ColumnHeaderStyle.LEFT;
            }

            if (model.getColumnClass(columnIndex).isAssignableFrom(LocalDate.class)) {
                return ColumnHeaderStyle.LEFT;
            } else if (model.getColumnClass(columnIndex).isAssignableFrom(ShortCommodityStyle.class)) {
                return ColumnHeaderStyle.RIGHT;
            } else if (model.getColumnClass(columnIndex).isAssignableFrom(FullCommodityStyle.class)) {
                return ColumnHeaderStyle.RIGHT;
            } else if (model.getColumnClass(columnIndex).isAssignableFrom(QuantityStyle.class)) {
                return ColumnHeaderStyle.RIGHT;
            } else if (model.getColumnClass(columnIndex).isAssignableFrom(String.class)) {
                return ColumnHeaderStyle.LEFT;
            } else if (model.getColumnClass(columnIndex).isAssignableFrom(BigDecimal.class)) {
                return ColumnHeaderStyle.RIGHT;
            }

            return ColumnHeaderStyle.LEFT;
        }

        @Override
        public String getColumnName(final int columnIndex) {
            if (sumAmounts && columnIndex == model.getColumnCount()) {
                return "group";
            }
            return model.getColumnName(columnIndex);
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {

            if (sumAmounts && columnIndex == model.getColumnCount()) {
                return String.class;
            }

            if (model.getColumnClass(columnIndex).isAssignableFrom(ShortCommodityStyle.class)) {
                return BigDecimal.class;
            }

            if (model.getColumnClass(columnIndex).isAssignableFrom(FullCommodityStyle.class)) {
                return BigDecimal.class;
            }

            if (model.getColumnClass(columnIndex).isAssignableFrom(QuantityStyle.class)) {
                return BigDecimal.class;
            }

            return model.getColumnClass(columnIndex);
        }

        @Override
        public int getRowCount() {
            return model.getRowCount();
        }

        @Override
        public int getColumnCount() {
            if (sumAmounts) {
                return model.getColumnCount() + 1;
            }
            return model.getColumnCount();
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            if (sumAmounts && columnIndex == model.getColumnCount()) {
                return "group";
            }
            return model.getValueAt(rowIndex, columnIndex);
        }
    }
}
