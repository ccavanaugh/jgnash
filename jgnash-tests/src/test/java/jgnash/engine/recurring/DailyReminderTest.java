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
 * Unit test for DailyReminders.
 *
 * @author Craig Cavanaugh
 */
class DailyReminderTest {

    @Test
    void simpleTest() {
        final DailyReminder reminder = new DailyReminder();

        final LocalDate startDate = LocalDate.of(2015, Month.AUGUST, 28);

        reminder.setIncrement(1);
        reminder.setStartDate(startDate);
        reminder.setEndDate(LocalDate.of(2015, Month.SEPTEMBER, 3));

        assertEquals(reminder.getReminderType(), ReminderType.DAILY);

        final RecurringIterator iterator = reminder.getIterator();

        assertEquals(startDate, iterator.next());
        assertEquals(LocalDate.of(2015, Month.AUGUST, 29), iterator.next());
        assertEquals(LocalDate.of(2015, Month.AUGUST, 30), iterator.next());
        assertEquals(LocalDate.of(2015, Month.AUGUST, 31), iterator.next());
        assertEquals(LocalDate.of(2015, Month.SEPTEMBER, 1), iterator.next());
        assertEquals(LocalDate.of(2015, Month.SEPTEMBER, 2), iterator.next());
        assertEquals(LocalDate.of(2015, Month.SEPTEMBER, 3), iterator.next());
        assertNull(iterator.next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.AUGUST, 29), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.AUGUST, 30), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.AUGUST, 31), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.SEPTEMBER, 1), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.SEPTEMBER, 2), reminder.getIterator().next());

        reminder.setLastDate();
        assertEquals(LocalDate.of(2015, Month.SEPTEMBER, 3), reminder.getIterator().next());

        reminder.setLastDate();
        assertNull(reminder.getIterator().next());
    }
}
