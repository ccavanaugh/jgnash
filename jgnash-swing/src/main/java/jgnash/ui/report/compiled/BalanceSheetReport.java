/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;

/**
 * Balance Sheet Report
 *
 * @author Craig Cavanaugh
 */
public class BalanceSheetReport extends AbstractSumByTypeReport {
    
    public BalanceSheetReport() {
        runningTotal = false;
    }
    
    /**
     * Returns the retained profit or loss for the given period
     * 
     * @param date Start date for the period
     * @return the profit or loss for the period
     */
    BigDecimal getRetainedProfitLoss(final Date date) {
        BigDecimal profitLoss = BigDecimal.ZERO;
        
        CurrencyNode baseCurrency = EngineFactory.getEngine(EngineFactory.DEFAULT).getDefaultCurrency();
                
        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        
        for (Account account : engine.getExpenseAccountList()) {
            profitLoss = profitLoss.add(account.getBalance(date, baseCurrency));
        }
        
        for (Account account : engine.getIncomeAccountList()) {
            profitLoss = profitLoss.add(account.getBalance(date, baseCurrency));
        }  
        
        return profitLoss;
    }

    @Override
    protected List<AccountGroup> getAccountGroups() {
        List<AccountGroup> groups = new ArrayList<>();

        groups.add(AccountGroup.ASSET);
        groups.add(AccountGroup.INVEST);
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
}