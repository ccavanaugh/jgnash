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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.time;

import jgnash.resource.util.ResourceUtils;

/**
 * Period Enum.
 * 
 * @author Craig Cavanaugh
 */
public enum Period {

    DAILY(ResourceUtils.getString("Period.Daily"), 1/30f),      // approximation
    WEEKLY(ResourceUtils.getString("Period.Weekly"), .25f),     // approximation
    BI_WEEKLY(ResourceUtils.getString("Period.BiWeekly"), .5f), // approximation
    MONTHLY(ResourceUtils.getString("Period.Monthly"), 1),
    QUARTERLY(ResourceUtils.getString("Period.Quarterly"), 3),
    YEARLY(ResourceUtils.getString("Period.Yearly" ), 12);

    private final transient String description;

    /**
     * The number of months in a period.  The value may be an approximation
     */
    private final transient float months;

    Period(final String description, final float months) {
        this.description = description;
        this.months = months;
    }

    @Override
    public String toString() {
        return description;
    }

    public float getMonths() {
        return months;
    }

}
