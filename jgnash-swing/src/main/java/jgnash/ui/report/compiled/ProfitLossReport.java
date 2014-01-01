/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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

import java.util.ArrayList;
import java.util.List;

import jgnash.engine.AccountGroup;

/**
 * Profit and Loss Report
 *
 * @author Craig Cavanaugh
 */
public class ProfitLossReport extends AbstractCrosstabReport {

    public ProfitLossReport() {
        super();
    }

    @Override
    protected List<AccountGroup> getAccountGroups() {
        List<AccountGroup> groups = new ArrayList<>();

        groups.add(AccountGroup.INCOME);
        groups.add(AccountGroup.EXPENSE);

        return groups;
    }

    /**
     * Returns the name of the report
     *
     * @return report name
     */
    @Override
    public String getReportName() {
        return rb.getString("Title.ProfitLoss");
    }

    /**
     * Returns the legend for the grand total
     *
     * @return report name
     */
    @Override
    public String getGrandTotalLegend() {
        return rb.getString("Word.NetIncome");
    }

    @Override
    public String getGroupFooterLabel() {
        return rb.getString("Word.Subtotal");
    }
}