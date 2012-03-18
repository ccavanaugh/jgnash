/*
 * jGnash, account personal finance application
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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.engine.budget;

import jgnash.util.Resource;

/**
 * Budget BudgetPeriod Enum
 * 
 * @author Craig Cavanaugh
 *
 */
public enum BudgetPeriod {

    DAILY(Resource.get().getString("Period.Daily")),
    WEEKLY(Resource.get().getString("Period.Weekly")),
    BI_WEEKLY(Resource.get().getString("Period.BiWeekly")),
    MONTHLY(Resource.get().getString("Period.Monthly")),
    QUARTERLY(Resource.get().getString("Period.Quarterly")),
    YEARLY(Resource.get().getString("Period.Yearly"));

    private final transient String description;

    private BudgetPeriod(final String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

}
