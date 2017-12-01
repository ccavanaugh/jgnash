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
package jgnash.uifx.report;

import java.math.BigDecimal;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.InvestmentPerformanceSummary;
import jgnash.engine.SecurityNode;
import jgnash.ui.report.jasper.AbstractReportTableModel;
import jgnash.ui.report.jasper.ColumnHeaderStyle;
import jgnash.ui.report.jasper.ColumnStyle;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.report.jasper.DynamicJasperReport;
import jgnash.util.Nullable;

import net.sf.jasperreports.engine.JasperPrint;

/**
 * Portfolio report controller.
 *
 * @author Craig Cavanaugh
 */
public class PortfolioReportController extends DynamicJasperReport {

    private static final String RECURSIVE = "recursive";

    private static final String VERBOSE = "verbose";

    @FXML
    private AccountComboBox accountComboBox;

    @FXML
    private CheckBox subAccountCheckBox;

    @FXML
    private CheckBox longNameCheckBox;

    @FXML
    private ResourceBundle resources;

    @FXML
    private void initialize() {
        // Only show visible investment accounts
        accountComboBox.setPredicate(account -> account.instanceOf(AccountType.INVEST) && account.isVisible());

        final Preferences preferences = getPreferences();

        subAccountCheckBox.setSelected(preferences.getBoolean(RECURSIVE, true));
        longNameCheckBox.setSelected(preferences.getBoolean(VERBOSE, false));
    }

    @Override
    @Nullable
    public JasperPrint createJasperPrint(final boolean formatForCSV) {
        final Account account = accountComboBox.getValue();

        if (account != null) {  // a null account is possible
            final PortfolioReportTableModel model = new PortfolioReportTableModel(account.getCurrencyNode());

            return createJasperPrint(model, formatForCSV);
        }

        return null;
    }

    @Override
    public String getReportName() {
        return accountComboBox.getValue().getName() + " - " + resources.getString("Title.PortfolioReport");
    }

    @Override
    protected String getGrandTotalLegend() {
        return "";
    }

    @Override
    protected String getGroupFooterLabel() {
        return null;
    }

    @FXML
    private void handleRefresh() {
        final Preferences preferences = getPreferences();

        preferences.putBoolean(RECURSIVE, subAccountCheckBox.isSelected());
        preferences.putBoolean(VERBOSE, longNameCheckBox.isSelected());

        if (refreshCallBackProperty().get() != null) {
            refreshCallBackProperty().get().run();
        }
    }

    private class PortfolioReportTableModel extends AbstractReportTableModel {

        private final CurrencyNode baseCurrency;

        private InvestmentPerformanceSummary performanceSummary;

        PortfolioReportTableModel(final CurrencyNode baseCurrency) {
            this.baseCurrency = baseCurrency;

            try {
                performanceSummary = new InvestmentPerformanceSummary(accountComboBox.getValue(), subAccountCheckBox.isSelected());
            } catch (Exception e) {
                Logger.getLogger(PortfolioReportTableModel.class.getName()).log(Level.SEVERE, null, e);
            }

            Logger.getLogger(PortfolioReportTableModel.class.getName()).info(performanceSummary.toString());
        }

        @Override
        public int getColumnCount() {
            return 13;
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
                    if (longNameCheckBox.isSelected()) {
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
                    double irr = pd.getInternalRateOfReturn();
                    return (Double.isNaN(irr)) ? null : BigDecimal.valueOf(irr);
                case 11:
                    return pd.getPercentPortfolio();
                case 12:
                    return "group";
                default:
                    return "ERR";
            }
        }

        @Override
        public String getColumnName(final int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return resources.getString("Column.Security");
                case 1:
                    return resources.getString("Column.Short.Quantity");
                case 2:
                    return resources.getString("Column.CostBasis");
                case 3:
                    return resources.getString("Column.TotalCostBasis");
                case 4:
                    return resources.getString("Column.Price");
                case 5:
                    return resources.getString("Column.MktValue");
                case 6:
                    return resources.getString("Column.Short.UnrealizedGain");
                case 7:
                    return resources.getString("Column.Short.RealizedGain");
                case 8:
                    return resources.getString("Column.Short.TotalGain");
                case 9:
                    return resources.getString("Column.Short.TotalGainPercentage");
                case 10:
                    return resources.getString("Column.Short.InternalRateOfReturn");
                case 11:
                    return resources.getString("Column.Short.PercentagePortfolio");
                case 12:
                    return "group";
                default:
                    return "ERR";
            }
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            if (columnIndex == 0 || columnIndex == 12) {
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
                case 11:
                    return ColumnStyle.PERCENTAGE;
                case 12:
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
