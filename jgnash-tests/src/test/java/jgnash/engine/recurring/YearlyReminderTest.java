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
package jgnash.engine.recurring;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit test for Yearly Reminders.
 *
 * @author Craig Cavanaugh
 */
class YearlyReminderTest {

    @Test
    void yearEndTestOne() {
        final YearlyReminder reminder = new YearlyReminder();

        final LocalDate startDate = LocalDate.ofYearDay(2011, 365);

        reminder.setIncrement(1);
        reminder.setStartDate(startDate);
        reminder.setEndDate(null);

        assertEquals(reminder.getReminderType(), ReminderType.YEARLY);

        final RecurringIterator iterator = reminder.getIterator();

        assertEquals(startDate, iterator.next());
        assertEquals(LocalDate.ofYearDay(2012, 366), iterator.next());
        assertEquals(LocalDate.ofYearDay(2013, 365), iterator.next());
        assertEquals(LocalDate.ofYearDay(2014, 365), iterator.next());
        assertEquals(LocalDate.ofYearDay(2015, 365), iterator.next());
        assertEquals(LocalDate.ofYearDay(2016, 366), iterator.next());
        assertEquals(LocalDate.ofYearDay(2017, 365), iterator.next());

        reminder.setLastDate();
        assertEquals(LocalDate.ofYearDay(2012, 366), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.ofYearDay(2013, 365), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.ofYearDay(2014, 365), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.ofYearDay(2015, 365), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.ofYearDay(2016, 366), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.ofYearDay(2017, 365), reminder.getIterator().next());
    }

    @Test
    void yearEndTestTwo() {
        final YearlyReminder reminder = new YearlyReminder();

        final LocalDate startDate = LocalDate.ofYearDay(2012, 366);

        reminder.setIncrement(1);
        reminder.setStartDate(startDate);
        reminder.setEndDate(null);

        assertEquals(reminder.getReminderType(), ReminderType.YEARLY);

        final RecurringIterator iterator = reminder.getIterator();

        assertEquals(startDate, iterator.next());
        assertEquals(LocalDate.ofYearDay(2013, 365), iterator.next());
        assertEquals(LocalDate.ofYearDay(2014, 365), iterator.next());
        assertEquals(LocalDate.ofYearDay(2015, 365), iterator.next());
        assertEquals(LocalDate.ofYearDay(2016, 366), iterator.next());
        assertEquals(LocalDate.ofYearDay(2017, 365), iterator.next());

        reminder.setLastDate();
        assertEquals(LocalDate.ofYearDay(2013, 365), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.ofYearDay(2014, 365), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.ofYearDay(2015, 365), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.ofYearDay(2016, 366), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.ofYearDay(2017, 365), reminder.getIterator().next());
    }

    @Test
    void simpleTest() {
        final YearlyReminder reminder = new YearlyReminder();

        final LocalDate startDate = LocalDate.ofYearDay(2011, 364);

        reminder.setIncrement(1);
        reminder.setStartDate(startDate);
        reminder.setEndDate(LocalDate.ofYearDay(2017, 365));

        assertEquals(reminder.getReminderType(), ReminderType.YEARLY);

        final RecurringIterator iterator = reminder.getIterator();

        assertEquals(startDate, iterator.next());
        assertEquals(LocalDate.of(2012, Month.DECEMBER, 29), iterator.next());
        assertEquals(LocalDate.ofYearDay(2013, 364), iterator.next());
        assertEquals(LocalDate.ofYearDay(2014, 364), iterator.next());
        assertEquals(LocalDate.ofYearDay(2015, 364), iterator.next());
        assertEquals(LocalDate.of(2016, Month.DECEMBER, 29), iterator.next());
        assertEquals(LocalDate.ofYearDay(2017, 364), iterator.next());
        assertNull(iterator.next());

        // leap year
        reminder.setLastDate();
        assertEquals(LocalDate.of(2012, Month.DECEMBER, 29), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.ofYearDay(2013, 364), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.ofYearDay(2014, 364), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.ofYearDay(2015, 364), reminder.getIterator().next());

        // leap year
        reminder.setLastDate();
        assertEquals(LocalDate.of(2016, Month.DECEMBER, 29), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.ofYearDay(2017, 364), reminder.getIterator().next());

        reminder.setLastDate();
        assertNull(iterator.next());
    }
}
