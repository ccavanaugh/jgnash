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
 * JUnit test for weekly reminders.
 *
 * @author Craig Cavanaugh
 */
class WeeklyReminderTest {

    @Test
    void iteratorTestOne() {
        final WeeklyReminder reminder = new WeeklyReminder();

        final LocalDate startDate = LocalDate.of(2015, Month.JULY, 4);

        reminder.setIncrement(1);
        reminder.setStartDate(startDate);
        reminder.setEndDate(null);

        assertEquals(reminder.getReminderType(), ReminderType.WEEKLY);

        final RecurringIterator iterator = reminder.getIterator();

        assertEquals(startDate, iterator.next());
        assertEquals(LocalDate.of(2015, Month.JULY, 11), iterator.next());
        assertEquals(LocalDate.of(2015, Month.JULY, 18), iterator.next());
        assertEquals(LocalDate.of(2015, Month.JULY, 25), iterator.next());
        assertEquals(LocalDate.of(2015, Month.AUGUST, 1), iterator.next());
        assertEquals(LocalDate.of(2015, Month.AUGUST, 8), iterator.next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.JULY, 11), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.JULY, 18), reminder.getIterator().next());
    }

    @Test
    void iteratorTestTwo() {
        final WeeklyReminder reminder = new WeeklyReminder();

        final LocalDate startDate = LocalDate.of(2015, Month.JULY, 4);

        reminder.setIncrement(2);
        reminder.setStartDate(startDate);
        reminder.setEndDate(null);

        assertEquals(reminder.getReminderType(), ReminderType.WEEKLY);

        final RecurringIterator iterator = reminder.getIterator();

        assertEquals(startDate, iterator.next());
        assertEquals(LocalDate.of(2015, Month.JULY, 18), iterator.next());
        assertEquals(LocalDate.of(2015, Month.AUGUST, 1), iterator.next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.JULY, 18), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.AUGUST, 1), reminder.getIterator().next());
    }

    @Test
    void iteratorTestThree() {
        final WeeklyReminder reminder = new WeeklyReminder();

        final LocalDate startDate = LocalDate.of(2015, Month.JULY, 4);

        reminder.setIncrement(3);
        reminder.setStartDate(startDate);
        reminder.setEndDate(null);

        assertEquals(reminder.getReminderType(), ReminderType.WEEKLY);

        final RecurringIterator iterator = reminder.getIterator();

        assertEquals(startDate, iterator.next());
        assertEquals(LocalDate.of(2015, Month.JULY, 25), iterator.next());
        assertEquals(LocalDate.of(2015, Month.AUGUST, 15), iterator.next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.JULY, 25), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.AUGUST, 15), reminder.getIterator().next());
    }

    @Test
    void iteratorTestFour() {
        WeeklyReminder reminder = new WeeklyReminder();

        final LocalDate startDate = LocalDate.of(2015, Month.JULY, 4);

        reminder.setIncrement(3);
        reminder.setStartDate(startDate);
        reminder.setEndDate(LocalDate.of(2015, Month.AUGUST, 15));

        assertEquals(reminder.getReminderType(), ReminderType.WEEKLY);

        RecurringIterator iterator = reminder.getIterator();

        assertEquals(startDate, iterator.next());
        assertEquals(LocalDate.of(2015, Month.JULY, 25), iterator.next());
        assertEquals(LocalDate.of(2015, Month.AUGUST, 15), iterator.next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.JULY, 25), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.AUGUST, 15), reminder.getIterator().next());

        reminder.setLastDate();
        assertNull(reminder.getIterator().next());
    }

    // End date check
    @Test
    void iteratorTestFive() {
        WeeklyReminder reminder = new WeeklyReminder();

        final LocalDate startDate = LocalDate.of(2015, Month.JULY, 4);

        reminder.setIncrement(3);
        reminder.setStartDate(startDate);
        reminder.setEndDate(LocalDate.of(2015, Month.AUGUST, 14));

        assertEquals(reminder.getReminderType(), ReminderType.WEEKLY);

        RecurringIterator iterator = reminder.getIterator();

        assertEquals(startDate, iterator.next());
        assertEquals(LocalDate.of(2015, Month.JULY, 25), iterator.next());
        assertNull(iterator.next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.JULY, 25), reminder.getIterator().next());

        reminder.setLastDate();
        assertNull(reminder.getIterator().next());
    }
}
