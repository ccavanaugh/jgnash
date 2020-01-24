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
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit test for Monthly Reminders.
 *
 * @author Craig Cavanaugh
 */
class MonthlyReminderTest {

    @Test
    void iteratorDateTestOne() {
        final MonthlyReminder reminder = new MonthlyReminder();

        final LocalDate startDate = LocalDate.of(2015, Month.JULY, 4);

        reminder.setIncrement(1);
        reminder.setStartDate(startDate);
        reminder.setEndDate(null);
        reminder.setType(MonthlyReminder.DATE);

        assertEquals(reminder.getReminderType(), ReminderType.MONTHLY);

        final RecurringIterator iterator = reminder.getIterator();

        assertEquals(startDate, iterator.next());
        assertEquals(LocalDate.of(2015, Month.AUGUST, 4), iterator.next());
        assertEquals(LocalDate.of(2015, Month.SEPTEMBER, 4), iterator.next());
        assertEquals(LocalDate.of(2015, Month.OCTOBER, 4), iterator.next());
        assertEquals(LocalDate.of(2015, Month.NOVEMBER, 4), iterator.next());
        assertEquals(LocalDate.of(2015, Month.DECEMBER, 4), iterator.next());
        assertEquals(LocalDate.of(2016, Month.JANUARY, 4), iterator.next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.AUGUST, 4), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.SEPTEMBER, 4), reminder.getIterator().next());
    }

    @Test
    void iteratorDateTestTwo() {
        final MonthlyReminder reminder = new MonthlyReminder();

        final LocalDate startDate = LocalDate.of(2015, Month.JULY, 4);

        reminder.setIncrement(2);
        reminder.setStartDate(startDate);
        reminder.setEndDate(null);
        reminder.setType(MonthlyReminder.DATE);

        assertEquals(reminder.getReminderType(), ReminderType.MONTHLY);

        final RecurringIterator iterator = reminder.getIterator();

        assertEquals(startDate, iterator.next());
        assertEquals(LocalDate.of(2015, Month.SEPTEMBER, 4), iterator.next());
        assertEquals(LocalDate.of(2015, Month.NOVEMBER, 4), iterator.next());
        assertEquals(LocalDate.of(2016, Month.JANUARY, 4), iterator.next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.SEPTEMBER, 4), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.NOVEMBER, 4), reminder.getIterator().next());
    }

    @Test
    void iteratorDateTestThree() {
        final MonthlyReminder reminder = new MonthlyReminder();

        final LocalDate startDate = LocalDate.of(2015, Month.JULY, 4);

        reminder.setIncrement(3);
        reminder.setStartDate(startDate);
        reminder.setEndDate(null);
        reminder.setType(MonthlyReminder.DATE);

        assertEquals(reminder.getReminderType(), ReminderType.MONTHLY);

        final RecurringIterator iterator = reminder.getIterator();

        assertEquals(startDate, iterator.next());
        assertEquals(LocalDate.of(2015, Month.OCTOBER, 4), iterator.next());
        assertEquals(LocalDate.of(2016, Month.JANUARY, 4), iterator.next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.OCTOBER, 4), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2016,Month.JANUARY, 4), reminder.getIterator().next());
    }

    @Test
    void iteratorDayTestOne() {
        Locale.setDefault(Locale.US);      // how weeks are counted depends on the locale
        
        final MonthlyReminder reminder = new MonthlyReminder();

        final LocalDate startDate = LocalDate.of(2015, Month.JULY, 4);

        reminder.setIncrement(1);
        reminder.setStartDate(startDate);
        reminder.setEndDate(null);
        reminder.setType(MonthlyReminder.DAY);

        assertEquals(reminder.getReminderType(), ReminderType.MONTHLY);

        final RecurringIterator iterator = reminder.getIterator();

        assertEquals(startDate, iterator.next());
        assertEquals(LocalDate.of(2015, Month.AUGUST, 1), iterator.next());
        assertEquals(LocalDate.of(2015, Month.SEPTEMBER, 5), iterator.next());
        assertEquals(LocalDate.of(2015, Month.OCTOBER, 3), iterator.next());
        assertEquals(LocalDate.of(2015, Month.NOVEMBER, 7), iterator.next());
        assertEquals(LocalDate.of(2015, Month.DECEMBER, 5), iterator.next());
        assertEquals(LocalDate.of(2016, Month.JANUARY, 2), iterator.next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.AUGUST, 1), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.SEPTEMBER, 5), reminder.getIterator().next());
    }

    @Test
    void iteratorDayTestTwo() {
        Locale.setDefault(Locale.US);      // how weeks are counted depends on the locale
        
        final MonthlyReminder reminder = new MonthlyReminder();

        final LocalDate startDate = LocalDate.of(2015, Month.JULY, 4);

        reminder.setIncrement(2);
        reminder.setStartDate(startDate);
        reminder.setEndDate(null);
        reminder.setType(MonthlyReminder.DAY);

        assertEquals(reminder.getReminderType(), ReminderType.MONTHLY);

        final RecurringIterator iterator = reminder.getIterator();

        assertEquals(startDate, iterator.next());

        assertEquals(LocalDate.of(2015, Month.SEPTEMBER, 5), iterator.next());
        assertEquals(LocalDate.of(2015, Month.NOVEMBER, 7), iterator.next());
        assertEquals(LocalDate.of(2016, Month.JANUARY, 2), iterator.next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.SEPTEMBER, 5), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.NOVEMBER, 7), reminder.getIterator().next());
    }

    @Test
    void iteratorDayTestThree() {
        final MonthlyReminder reminder = new MonthlyReminder();

        final LocalDate startDate = LocalDate.of(2015, Month.JULY, 4);

        reminder.setIncrement(3);
        reminder.setStartDate(startDate);
        reminder.setEndDate(null);
        reminder.setType(MonthlyReminder.DAY);

        assertEquals(reminder.getReminderType(), ReminderType.MONTHLY);

        final RecurringIterator iterator = reminder.getIterator();

        assertEquals(startDate, iterator.next());
        assertEquals(LocalDate.of(2015, Month.OCTOBER, 3), iterator.next());
        assertEquals(LocalDate.of(2016, Month.JANUARY, 2), iterator.next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.OCTOBER, 3), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2016, Month.JANUARY, 2), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2016, Month.APRIL, 2), reminder.getIterator().next());
    }

    @Test
    void iteratorDayTestFour() {
        final MonthlyReminder reminder = new MonthlyReminder();

        final LocalDate startDate = LocalDate.of(2015, Month.JULY, 4);

        reminder.setIncrement(3);
        reminder.setStartDate(startDate);
        reminder.setEndDate(LocalDate.of(2016, Month.APRIL, 3));
        reminder.setType(MonthlyReminder.DAY);

        assertEquals(reminder.getReminderType(), ReminderType.MONTHLY);

        final RecurringIterator iterator = reminder.getIterator();

        assertEquals(startDate, iterator.next());
        assertEquals(LocalDate.of(2015, Month.OCTOBER, 3), iterator.next());
        assertEquals(LocalDate.of(2016, Month.JANUARY, 2), iterator.next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.OCTOBER, 3), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2016, Month.JANUARY, 2), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2016, Month.APRIL, 2), reminder.getIterator().next());

        reminder.setLastDate();
        assertNull(reminder.getIterator().next());
    }
}
