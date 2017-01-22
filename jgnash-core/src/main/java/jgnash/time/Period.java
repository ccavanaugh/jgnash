/*
 * jGnash, account personal finance application
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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.time;

import jgnash.util.ResourceUtils;

/**
 * Period Enum.
 * 
 * @author Craig Cavanaugh
 */
public enum Period {

    DAILY(ResourceUtils.getString("Period.Daily")),
    WEEKLY(ResourceUtils.getString("Period.Weekly")),
    BI_WEEKLY(ResourceUtils.getString("Period.BiWeekly")),
    MONTHLY(ResourceUtils.getString("Period.Monthly")),
    QUARTERLY(ResourceUtils.getString("Period.Quarterly")),
    YEARLY(ResourceUtils.getString("Period.Yearly"));

    private final transient String description;

    Period(final String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

}
