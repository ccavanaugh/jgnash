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
package jgnash.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import jgnash.time.DateUtils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for date utilities.
 *
 * @author Craig Cavanaugh
 */
class DateTest {

    @Test
    void formatTestOne() throws ParseException {
        final SimpleDateFormat format = new SimpleDateFormat("yyMMdd");
        final Date date = format.parse("121206");

        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyMMdd");
        final LocalDate localDate = LocalDate.from(dateTimeFormatter.parse("121206"));

        assertEquals(localDate, DateUtils.asLocalDate(date));
    }

    @Test
    void epochTest() {
        final LocalDate now = LocalDate.now();

        // round trip
        assertEquals(now, DateUtils.asLocalDate(DateUtils.asEpochMilli(now)));
    }

    @Test
    void getFirstDayWeeklyTest() {
        // test days for year 2011
        LocalDate[] days = DateUtils.getFirstDayWeekly(2011);

        /*for (final LocalDate day : days) {
            System.out.println(day.toString());
        }*/

        assertEquals(52, days.length);

        assertEquals(3, days[0].getDayOfMonth());

        assertEquals(26, days[51].getDayOfMonth());

        // test days for year 2004
        days = DateUtils.getFirstDayWeekly(2004);


        /*for (final LocalDate day : days) {
            System.out.println(day.toString());
        }*/

        assertEquals(53, days.length);

        assertEquals(29, days[0].getDayOfMonth());

        assertEquals(20, days[51].getDayOfMonth());

        assertEquals(27, days[52].getDayOfMonth());

        // test days for year 2015
        days = DateUtils.getFirstDayWeekly(2015);

        /*for (final LocalDate day : days) {
            System.out.println(day.toString());
        }*/

        assertEquals(53, days.length);

        assertEquals(29, days[0].getDayOfMonth());

        assertEquals(5, days[1].getDayOfMonth());

        assertEquals(21, days[51].getDayOfMonth());

        assertEquals(28, days[52].getDayOfMonth());
    }

    @Test
    void getFirstDayBiWeeklyTest() {
        // test days for year 2011
        LocalDate[] days = DateUtils.getFirstDayBiWeekly(2011);

        assertEquals(26, days.length);

        days = DateUtils.getFirstDayBiWeekly(2015);
        assertEquals(27, days.length);

        assertEquals(29, days[0].getDayOfMonth());

        assertEquals(12, days[1].getDayOfMonth());

        assertEquals(28, days[26].getDayOfMonth());
    }

    @Test
    void getFirstDaysInMonthTest() {
        LocalDate[] days = DateUtils.getFirstDayMonthly(2011);

        assertEquals(1, days[0].getDayOfYear());
        assertEquals(1 + 31, days[1].getDayOfYear());
        assertEquals(1 + 31 + 28, days[2].getDayOfYear());
        assertEquals(1 + 31 + 28 + 31, days[3].getDayOfYear());

        assertEquals(365 - 31 - 30 + 1, days[10].getDayOfYear());
        assertEquals(365 - 31 + 1, days[11].getDayOfYear());
    }

    @Test
    void getFirstDaysInMonthTest2() {
        LocalDate[] days = DateUtils.getFirstDayMonthly(Month.JANUARY, 2011, 12);

        assertEquals(12, days.length);
        assertEquals(1, days[0].getDayOfYear());
        assertEquals(1 + 31, days[1].getDayOfYear());
        assertEquals(1 + 31 + 28, days[2].getDayOfYear());
        assertEquals(1 + 31 + 28 + 31, days[3].getDayOfYear());

        assertEquals(365 - 31 - 30 + 1, days[10].getDayOfYear());
        assertEquals(365 - 31 + 1, days[11].getDayOfYear());

        // roll past 2011 test
        days = DateUtils.getFirstDayMonthly(Month.FEBRUARY, 2011, 12);

        assertEquals(12, days.length);
        assertEquals(2011, days[0].getYear());
        assertEquals(2012, days[11].getYear());
        assertEquals(DayOfWeek.SUNDAY, days[11].getDayOfWeek());
    }

    @Test
    void quaterTest() {
        LocalDate[] days = DateUtils.getFirstDayQuarterly(2019);

        assertEquals(days[0], LocalDate.of(2019, 1, 1));
        assertEquals(days[1], LocalDate.of(2019, 4, 1));
        assertEquals(days[2], LocalDate.of(2019, 7, 1));
        assertEquals(days[3], LocalDate.of(2019, 10, 1));


        days = DateUtils.getFirstDayQuarterly(Month.APRIL, 2019, 4);

        assertEquals(days[0], LocalDate.of(2019, 4, 1));
        assertEquals(days[1], LocalDate.of(2019, 7, 1));
        assertEquals(days[2], LocalDate.of(2019, 10, 1));
        assertEquals(days[3], LocalDate.of(2020, 1, 1));
    }

    @Test
    void getAllDaysTest() {
        LocalDate[] days = DateUtils.getAllDays(2011);
        assertEquals(365, days.length);

        assertEquals(LocalDate.ofYearDay(2011, 1), days[0]);
        assertEquals(LocalDate.ofYearDay(2011, 365), days[364]);

        assertEquals(366, DateUtils.getAllDays(2000).length);
    }

    @Test
    void weekOfYear() {
        assertEquals(53, DateUtils.getWeekOfTheYear(LocalDate.ofYearDay(2016, 3)));
        assertEquals(1, DateUtils.getWeekOfTheYear(LocalDate.ofYearDay(2016, 4)));
    }

    @Test
    void weeksPerYear() {
        assertEquals(53, DateUtils.getNumberOfWeeksInYear(2009));
        assertEquals(52, DateUtils.getNumberOfWeeksInYear(2014));
        assertEquals(53, DateUtils.getNumberOfWeeksInYear(2015));
        assertEquals(52, DateUtils.getNumberOfWeeksInYear(2016));
        assertEquals(53, DateUtils.getNumberOfWeeksInYear(2020));
        assertEquals(52, DateUtils.getNumberOfWeeksInYear(2025));
        assertEquals(53, DateUtils.getNumberOfWeeksInYear(2026));
        assertEquals(53, DateUtils.getNumberOfWeeksInYear(2032));
    }
}
