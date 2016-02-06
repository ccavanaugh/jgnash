/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;

import jgnash.uifx.report.jasper.AbstractSumByTypeReport;

/**
 * Balance Sheet Report
 * 
 * @author Craig Cavanaugh
 */
public class BalanceSheetReportController extends AbstractSumByTypeReport {

    public BalanceSheetReportController() {
        runningTotal = false;
    }

    protected ReportModel createReportModel(final LocalDate startDate, final LocalDate endDate) {
        ReportModel model = super.createReportModel(startDate, endDate);

        // load retained profit and loss row        
        model.addRow(new RetainedEarningsRow());

        return model;
    }

    @Override
    protected List<AccountGroup> getAccountGroups() {
        List<AccountGroup> groups = new ArrayList<>();

        groups.add(AccountGroup.ASSET);
        groups.add(AccountGroup.INVEST);
        groups.add(AccountGroup.SIMPLEINVEST);
        groups.add(AccountGroup.LIABILITY);
        groups.add(AccountGroup.EQUITY);

        return groups;
    }

    /**
     * Returns the name of the report
     * 
     * @return report name
     */
    @Override
    public String getReportName() {
        return rb.getString("Title.BalanceSheet");
    }

    /**
     * Returns the legend for the grand total
     * 
     * @return report name
     */
    @Override
    public String getGrandTotalLegend() {
        return rb.getString("Word.Difference");
    }

    @Override
    public String getGroupFooterLabel() {
        return rb.getString("Word.Subtotal");
    }

    /**
     * Internal class to return a row the calculates the retained earnings for an account   
     */
    private class RetainedEarningsRow extends Row {

        /**
         * Returns values for retained earnings
         */
        @Override
        public Object getValueAt(final int columnIndex) {

            if (columnIndex == 0) { // account column
                return rb.getString("Title.RetainedEarnings");
            } else if (columnIndex == getColumnCount() - 1) { // group column              
                return AccountGroup.EQUITY.toString();
            } else if (columnIndex > 0 && columnIndex <= dates.size()) {
                final LocalDate startDate = dates.get(columnIndex - 1);
                final LocalDate endDate = dates.get(columnIndex).minusDays(1);

                return getRetainedProfitLoss(startDate, endDate);
            }
            return null;
        }

        private int getColumnCount() {
            if (runningTotal) {
                return dates.size() + 2;
            } else {
                return dates.size() - 1 + 2;
            }
        }

        /**
         * Returns the retained profit or loss for the given period
         * 
         * @param startDate Start date for the period
         * @param endDate End date for the period
         * @return the profit or loss for the period
         */
        private BigDecimal getRetainedProfitLoss(final LocalDate startDate, final LocalDate endDate) {
            BigDecimal profitLoss = BigDecimal.ZERO;

            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            final CurrencyNode baseCurrency = engine.getDefaultCurrency();

            for (final Account account : engine.getExpenseAccountList()) {
                profitLoss = profitLoss.add(account.getBalance(startDate, endDate, baseCurrency));
            }

            for (final Account account : engine.getIncomeAccountList()) {
                profitLoss = profitLoss.add(account.getBalance(startDate, endDate, baseCurrency));
            }

            return profitLoss.negate();
        }
    }
}