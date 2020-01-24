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
package jgnash.resource.util;

import java.text.DateFormatSymbols;
import java.time.Month;

/**
 * Human friendly Decorator for {@code Month}
 *
 * @author Craig Cavanaugh
 */
public enum MonthName {

    JANUARY(Month.JANUARY, new DateFormatSymbols().getMonths()[0]),
    FEBRUARY(Month.FEBRUARY, new DateFormatSymbols().getMonths()[1]),
    MARCH(Month.MARCH, new DateFormatSymbols().getMonths()[2]),
    APRIL(Month.APRIL, new DateFormatSymbols().getMonths()[3]),
    MAY(Month.MAY, new DateFormatSymbols().getMonths()[4]),
    JUNE(Month.JUNE, new DateFormatSymbols().getMonths()[5]),
    JULY(Month.JULY, new DateFormatSymbols().getMonths()[6]),
    AUGUST(Month.AUGUST, new DateFormatSymbols().getMonths()[7]),
    SEPTEMBER(Month.SEPTEMBER, new DateFormatSymbols().getMonths()[8]),
    OCTOBER(Month.OCTOBER, new DateFormatSymbols().getMonths()[9]),
    NOVEMBER(Month.NOVEMBER, new DateFormatSymbols().getMonths()[10]),
    DECEMBER(Month.DECEMBER, new DateFormatSymbols().getMonths()[11]);

    private final Month month;
    private final String description;

    MonthName(final Month month, final String description) {
        this.month = month;
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

    public static MonthName valueOf(final Month month) {
        return values()[month.ordinal()];
    }

    public Month getMonth() {
        return month;
    }
}
