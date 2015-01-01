/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.junit.Test;

/**
 * @author Craig Cavanaugh
 *
 */
public class DateTest {

    @Test
    public void getNameOfMonthTest() {
        Locale.setDefault(Locale.US);

        Date date = DateUtils.getDateOfTheYear(2011, 1);

        String month = DateUtils.getNameOfMonth(date);

        assertEquals("January", month);

        date = DateUtils.getDateOfTheYear(2011, 70);
        month = DateUtils.getNameOfMonth(date);

        assertEquals("March", month);
    }

    @Test
    public void trimTest() {

        // first one initializes the queue
        DateUtils.today();

        Date date = new Date();

        // micro benchmark
        long start = System.nanoTime();

        int TESTS = 1000;

        for (int i = 0; i < TESTS; i++) {
            DateUtils.trimDate(date);
        }

        System.out.println(" " + (System.nanoTime() - start) / TESTS + " nano seconds per trim");

        assertTrue(true);

    }

    @Test
    public void getFirstDayWeeklyTest() {
        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();

        // test days for year 2011
        Date[] days = DateUtils.getFirstDayWeekly(2011);

        //        for (Date day : days) {
        //            System.out.println(day.toString());
        //        }

        assertEquals(52, days.length);

        cal.setTime(days[0]);
        assertEquals(2, cal.get(Calendar.DAY_OF_MONTH));

        cal.setTime(days[51]);
        assertEquals(25, cal.get(Calendar.DAY_OF_MONTH));

        // test days for year 2004
        days = DateUtils.getFirstDayWeekly(2004);

        assertEquals(53, days.length);

        cal.setTime(days[0]);
        assertEquals(28, cal.get(Calendar.DAY_OF_MONTH));

        cal.setTime(days[51]);
        assertEquals(19, cal.get(Calendar.DAY_OF_MONTH));

        cal.setTime(days[52]);
        assertEquals(26, cal.get(Calendar.DAY_OF_MONTH));

        // test days for year 2015
        days = DateUtils.getFirstDayWeekly(2015);

        //        for (Date day: days) {
        //            System.out.println(day.toString());
        //        }

        assertEquals(53, days.length);

        cal.setTime(days[0]);
        assertEquals(28, cal.get(Calendar.DAY_OF_MONTH));

        cal.setTime(days[1]);
        assertEquals(4, cal.get(Calendar.DAY_OF_MONTH));

        cal.setTime(days[51]);
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH));

        cal.setTime(days[52]);
        assertEquals(27, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void getFirstDayBiWeeklyTest() {
        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();

        // test days for year 2011
        Date[] days = DateUtils.getFirstDayBiWeekly(2011);

        assertEquals(26, days.length);

        days = DateUtils.getFirstDayBiWeekly(2015);
        assertEquals(27, days.length);

        cal.setTime(days[0]);
        assertEquals(28, cal.get(Calendar.DAY_OF_MONTH));

        cal.setTime(days[1]);
        assertEquals(11, cal.get(Calendar.DAY_OF_MONTH));

        cal.setTime(days[26]);
        assertEquals(27, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void isLeapYearTest() {
        assertEquals(Boolean.FALSE, DateUtils.isLeapYear(2011));

        assertEquals(Boolean.TRUE, DateUtils.isLeapYear(2000));
    }

    @Test
    public void getDateOfTheYearTest() {

        Date today = DateUtils.today();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);

        int year = calendar.get(Calendar.YEAR);
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

        assertEquals(today, DateUtils.getDateOfTheYear(year, dayOfYear));
    }

    @Test
    public void getDaysInMonthTest() {
        assertEquals(31, DateUtils.getDaysInMonth(DateUtils.getDateOfTheYear(2011, 1)));
        assertEquals(31, DateUtils.getDaysInMonth(DateUtils.getDateOfTheYear(2011, 31)));

        assertEquals(28, DateUtils.getDaysInMonth(DateUtils.getDateOfTheYear(2011, 40)));

        // February leap year
        assertEquals(29, DateUtils.getDaysInMonth(DateUtils.getDateOfTheYear(2000, 40)));
    }

    @Test
    public void getDifferenceInDaysTest() {

        Date start = DateUtils.getDateOfTheYear(2011, 1);

        Date end = DateUtils.getDateOfTheYear(2011, 1);
        assertEquals(0, DateUtils.getDifferenceInDays(start, end));

        end = DateUtils.getDateOfTheYear(2011, 2);
        assertEquals(1, DateUtils.getDifferenceInDays(start, end));

        end = DateUtils.getDateOfTheYear(2011, 3);
        assertEquals(2, DateUtils.getDifferenceInDays(start, end));

        end = DateUtils.getDateOfTheYear(2012, 1);
        assertEquals(365, DateUtils.getDifferenceInDays(start, end));

        end = DateUtils.getDateOfTheYear(2012, 2);
        assertEquals(366, DateUtils.getDifferenceInDays(start, end));
    }

    @Test
    public void getDifferenceInMonthsTest() {

        Date start = DateUtils.getDateOfTheYear(2011, 1);

        Date end = DateUtils.getDateOfTheYear(2011, 1);
        assertEquals(0, DateUtils.getDifferenceInMonths(start, end), 0.01);

        end = DateUtils.getDateOfTheYear(2011, 31);
        assertEquals(1, DateUtils.getDifferenceInMonths(start, end), 0.02);

        end = DateUtils.getDateOfTheYear(2012, 1);
        assertEquals(12, DateUtils.getDifferenceInMonths(start, end), 0.01);
    }

    @Test
    public void getFirstDaysInMonthTest() {
        Date[] days = DateUtils.getFirstDayMonthly(2011);

        assertEquals(1, DateUtils.getDayOfTheYear(days[0]));
        assertEquals(1 + 31, DateUtils.getDayOfTheYear(days[1]));
        assertEquals(1 + 31 + 28, DateUtils.getDayOfTheYear(days[2]));
        assertEquals(1 + 31 + 28 + 31, DateUtils.getDayOfTheYear(days[3]));

        assertEquals(365 - 31 - 30 + 1, DateUtils.getDayOfTheYear(days[10]));
        assertEquals(365 - 31 + 1, DateUtils.getDayOfTheYear(days[11]));

    }

    @Test
    public void getAllDaysTest() {
        Date[] days = DateUtils.getAllDays(2011);
        assertEquals(365, days.length);

        assertEquals(DateUtils.getDateOfTheYear(2011, 1), days[0]);
        assertEquals(DateUtils.getDateOfTheYear(2011, 365), days[364]);

        assertEquals(366, DateUtils.getAllDays(2000).length);
    }
}
