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
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.InvestmentPerformanceSummary;
import jgnash.engine.SecurityNode;
import jgnash.report.pdf.Report;
import jgnash.report.table.AbstractReportTableModel;
import jgnash.report.table.ColumnStyle;
import jgnash.time.DateUtils;
import jgnash.util.NotNull;

/**
 * Portfolio Report
 *
 * @author Craig Cavanaugh
 */
public class PortfolioReport extends Report {

    private static final int COLUMN_COUNT = 12;

    PortfolioReport() {
        super();

        setForceGroupPagination(false);
    }

    static int getColumnCount() {
        return COLUMN_COUNT;
    }

    static String getColumnName(final int columnIndex) {
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
                return rb.getString("Column.MktValue");
            case 6:
                return rb.getString("Column.Short.UnrealizedGain");
            case 7:
                return rb.getString("Column.Short.RealizedGain");
            case 8:
                return rb.getString("Column.Short.TotalGain");
            case 9:
                return rb.getString("Column.Short.TotalGainPercentage");
            case 10:
                return rb.getString("Column.Short.InternalRateOfReturn");
            case 11:
                return rb.getString("Column.Short.PercentagePortfolio");
            default:
                return "ERR";
        }
    }

    static AbstractReportTableModel createReportModel(final Account account, final LocalDate startDate,
                                                      final LocalDate endDate, final boolean recursive,
                                                      final boolean longNames,
                                                      final Function<String, Boolean> columnVisibilityFunction) {

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        return new PortfolioReportTableModel(account, engine.getDefaultCurrency(), startDate, endDate, recursive,
                longNames, columnVisibilityFunction);
    }

    private static class PortfolioReportTableModel extends AbstractReportTableModel {

        private final Account account;

        private final CurrencyNode baseCurrency;

        private InvestmentPerformanceSummary performanceSummary;

        private final boolean longNames;

        private final String subTitle;

        private final Function<String, Boolean> columnVisibilityFunction;

        PortfolioReportTableModel(@NotNull final Account account, @NotNull final CurrencyNode baseCurrency,
                                  final LocalDate startDate, final LocalDate endDate, final boolean recursive,
                                  final boolean longNames, final Function<String, Boolean> columnVisibilityFunction) {
            Objects.requireNonNull(account);
            Objects.requireNonNull(baseCurrency);

            this.account = account;
            this.baseCurrency = baseCurrency;
            this.longNames = longNames;

            this.columnVisibilityFunction = Objects.requireNonNullElseGet(columnVisibilityFunction, () -> s -> true);

            // update the subtitle
            final MessageFormat format = new MessageFormat(rb.getString("Pattern.DateRange"));
            subTitle = format.format(new Object[]{DateUtils.asDate(startDate), DateUtils.asDate(endDate)});

            try {
                performanceSummary = new InvestmentPerformanceSummary(account, startDate, endDate, recursive);

                performanceSummary.runCalculations();

                Logger.getLogger(PortfolioReport.class.getName()).info(performanceSummary.toString());
            } catch (final Exception e) {
                Logger.getLogger(PortfolioReport.class.getName()).log(Level.SEVERE, null, e);
            }
        }

        @Override
        public boolean isColumnVisible(final int column) {

            // super can override
            boolean result  = super.isColumnVisible(column);

            if (column != 0 && result) {    // security name is not an option
                result = columnVisibilityFunction.apply(getColumnName(column));
            }

            return result;
        }

        @Override
        public String getTitle() {
            return account.getName() + " - " + rb.getString("Title.PortfolioReport");
        }

        @Override
        public String getSubTitle() {
            return subTitle;
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
                    if (longNames) {
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
                    return AbstractReportTableModel.DEFAULT_GROUP;
                default:
                    return "ERR";
            }
        }

        @Override
        public String getColumnName(final int columnIndex) {
            if (columnIndex >= getColumnCount()) {
                return "group";
            }

            return PortfolioReport.getColumnName(columnIndex);
        }
        @Override
        public int getColumnCount() {
            return PortfolioReport.getColumnCount() + 1;    // add group column
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            if (columnIndex == 0 || columnIndex == 12) {
                return String.class;
            }

            return BigDecimal.class;
        }

        @Override
        public CurrencyNode getCurrencyNode() {
            return baseCurrency;
        }

        @Override
        public ColumnStyle getColumnStyle(final int columnIndex) {
            switch (columnIndex) {
                case 1:
                    return ColumnStyle.QUANTITY;
                case 2:
                case 4:
                    return ColumnStyle.BALANCE;
                case 3:
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
                case 0:
                default:
                    return ColumnStyle.STRING;
            }
        }

        @Override
        public boolean isColumnFixedWidth(final int columnIndex) {
            return columnIndex > 0;
        }
    }
}
