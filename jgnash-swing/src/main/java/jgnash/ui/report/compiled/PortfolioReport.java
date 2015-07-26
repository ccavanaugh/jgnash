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

import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.InvestmentPerformanceSummary;
import jgnash.engine.SecurityNode;
import jgnash.ui.components.AccountListComboBox;
import jgnash.ui.report.AbstractReportTableModel;
import jgnash.ui.report.ColumnHeaderStyle;
import jgnash.ui.report.ColumnStyle;
import jgnash.ui.report.jasper.DynamicJasperReport;
import jgnash.ui.util.IconUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import net.sf.jasperreports.engine.JasperPrint;

/**
 * This is a portfolio report for Investment Accounts
 *
 * @author Vijil E C
 * @author Craig Cavanaugh
 * @author Juan Jose Garcia Ripoll
 */
public class PortfolioReport extends DynamicJasperReport {

    private final JCheckBox subAccountCheckBox;

    private final JCheckBox verboseCheckBox;

    private final AccountListComboBox accountCombo;

    private final JButton refreshButton;

    private static final String USE_LONG_NAMES = "useLongNames";

    private static final String RECURSIVE = "recursive";

    public PortfolioReport() {

        Preferences p = getPreferences();

        subAccountCheckBox = new JCheckBox(rb.getString("Button.IncludeSubAccounts"));
        subAccountCheckBox.setSelected(p.getBoolean(RECURSIVE, false));

        verboseCheckBox = new JCheckBox(rb.getString("Button.UseLongNames"));
        verboseCheckBox.setSelected(p.getBoolean(USE_LONG_NAMES, false));

        accountCombo = AccountListComboBox.getInstanceByType(AccountType.getAccountTypes(AccountGroup.INVEST));

        refreshButton = new JButton(rb.getString("Button.Refresh"), IconUtils.getIcon("/jgnash/resource/view-refresh.png"));

        refreshButton.addActionListener(new AbstractAction() {

            @Override
            public void actionPerformed(final ActionEvent ae) {
                refreshReport();
            }
        });
    }

    public PortfolioReport(final Account account) {
        this();
        accountCombo.setSelectedAccount(account);
    }

    @Override
    protected void refreshReport() {
        Preferences p = getPreferences();

        p.putBoolean(RECURSIVE, subAccountCheckBox.isSelected());
        p.putBoolean(USE_LONG_NAMES, verboseCheckBox.isSelected());

        super.refreshReport();
    }

    /**
     * Creates a JasperPrint object.
     *
     * @return JasperPrint object
     */
    @Override
    public JasperPrint createJasperPrint(final boolean formatForCSV) {
        logger.info(rb.getString("Message.CollectingReportData"));

        Account account = accountCombo.getSelectedAccount();
     
        PortfolioReportTableModel model = new PortfolioReportTableModel(account.getCurrencyNode());
        model.verbose = verboseCheckBox.isSelected();

        return createJasperPrint(model, formatForCSV);
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
        FormLayout layout = new FormLayout("p, $lcgap, p:g, 8dlu, p", "f:d, $lgap, f:d");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.border(Borders.DIALOG);
        builder.append(rb.getString("Label.Account"), accountCombo);
        builder.append(refreshButton);
        builder.nextLine();
        builder.nextLine();
        builder.append(buildOptionPanel(), 5);

        layout.addGroupedRow(1);
        layout.addGroupedRow(3);

        return builder.getPanel();
    }

    private JPanel buildOptionPanel() {
        FormLayout layout = new FormLayout("p, 8dlu, p", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(subAccountCheckBox);
        builder.append(verboseCheckBox);

        return builder.getPanel();
    }

    /**
     * Returns the name of the report
     *
     * @return report name
     */
    @Override
    public String getReportName() {
        return accountCombo.getSelectedAccount().getName() + " - " + rb.getString("Title.PortfolioReport");
    }

    /**
     * Returns the legend for the grand total
     *
     * @return report name
     */
    @Override
    public String getGrandTotalLegend() {
        return "";
    }

    @Override
    public String getGroupFooterLabel() {
        return null;
    }

    public class PortfolioReportTableModel extends AbstractReportTableModel {

        boolean verbose;

        private final CurrencyNode baseCurrency;

        private InvestmentPerformanceSummary performanceSummary;

        PortfolioReportTableModel(final CurrencyNode baseCurrency) {

            this.baseCurrency = baseCurrency;

            verbose = verboseCheckBox.isSelected();

            try {
                performanceSummary = new InvestmentPerformanceSummary(accountCombo.getSelectedAccount(), subAccountCheckBox.isSelected());
            } catch (Exception e) {           
                Logger.getLogger(PortfolioReportTableModel.class.getName()).log(Level.SEVERE, null, e);
            }

             Logger.getLogger(PortfolioReportTableModel.class.getName()).info(performanceSummary.toString());
        }

        @Override
        public int getColumnCount() {
            return 12;
        }

        @Override
        public int getRowCount() {
            return performanceSummary.getSecurities().size();
        }

        @Override
        public Object getValueAt(final int row, final int col) {    
            SecurityNode cn = performanceSummary.getSecurities().get(row);

            InvestmentPerformanceSummary.SecurityPerformanceData pd = performanceSummary.getPerformanceData(cn);

            switch (col) {
                case 0:
                    if (verbose) {
                        return pd.getNode().getDescription();
                    }
                    return pd.getNode().getSymbol();
                case 1:
                    return pd.getSharesHeld();
                case 2:
                    return pd.getCostBasisPerShare();
                case 3:
                    return pd.getHeldCostBasis();
                case 4:
                    return pd.getPrice(baseCurrency);
                case 5:
                    return pd.getMarketValue(baseCurrency);
                case 6:
                    return pd.getUnrealizedGains();
                case 7:
                    return pd.getRealizedGains();
                case 8:
                    return pd.getTotalGains();
                case 9:
                    return pd.getTotalGainsPercentage();
                case 10:
                    return pd.getPercentPortfolio();
                case 11:
                    return "group";
                default:
                    return "ERR";
            }

        }

        @Override
        public String getColumnName(final int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return rb.getString("Column.Security");
                case 1:
                    return rb.getString("Column.Short.Quantity");
                case 2:
                    return rb.getString("Column.CostBasis");
                case 3:
                    return rb.getString("Column.TotalCostBasis");
                case 4:
                    return rb.getString("Column.Price");
                case 5:
                    return rb.getString("Column.Value");
                case 6:
                    return rb.getString("Column.Short.UnrealizedGain");
                case 7:
                    return rb.getString("Column.Short.RealizedGain");
                case 8:
                    return rb.getString("Column.Short.TotalGain");
                case 9:
                    return rb.getString("Column.Short.TotalGainPercentage");
                case 10:
                    return rb.getString("Column.Short.PercentagePortfolio");
                case 11:
                    return "group";
                default:
                    return "ERR";
            }
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            if (columnIndex == 0 || columnIndex == 11) {
                return String.class;
            }

            return BigDecimal.class;
        }

        @Override
        public CurrencyNode getCurrency() {
            return baseCurrency;
        }

        @Override
        public ColumnStyle getColumnStyle(final int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return ColumnStyle.STRING;
                case 1:
                    return ColumnStyle.QUANTITY;
                case 2:
                    return ColumnStyle.BALANCE;
                case 3:
                    return ColumnStyle.AMOUNT_SUM;
                case 4:
                    return ColumnStyle.BALANCE;
                case 5:
                case 6:
                case 7:
                case 8:
                    return ColumnStyle.AMOUNT_SUM;
                case 9:
                case 10:
                    return ColumnStyle.PERCENTAGE;
                case 11:
                    return ColumnStyle.GROUP_NO_HEADER;
                default:
                    return ColumnStyle.STRING;
            }
        }

        @Override
        public ColumnHeaderStyle getColumnHeaderStyle(final int columnIndex) {
            if (columnIndex == 0) { // security column
                return ColumnHeaderStyle.LEFT;
            }
            return ColumnHeaderStyle.RIGHT;
        }

        @Override
        public boolean isColumnFixedWidth(final int columnIndex) {
            return columnIndex > 0;
        }
    }

}
