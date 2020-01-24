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

import java.time.DayOfWeek;
import java.time.LocalDate;

import javax.persistence.Entity;

import jgnash.time.DateUtils;

/**
 * A weekly reminder.
 *
 * @author Craig Cavanaugh
 */
@Entity
public class WeeklyReminder extends Reminder {

    public WeeklyReminder() {

    }

    @Override
    public ReminderType getReminderType() {
        return ReminderType.WEEKLY;
    }

    @Override
    public RecurringIterator getIterator() {
        return new WeeklyIterator();
    }

    private class WeeklyIterator implements RecurringIterator {
        private LocalDate base;

        WeeklyIterator() {
            if (getLastDate() != null) {
                final DayOfWeek dayOfWeek = DayOfWeek.from(getStartDate());

                base = getLastDate().plusDays(1);   // plus one day to force next iteration

                // adjust for actual target date, it could have been modified since the last date
                base = (LocalDate) dayOfWeek.adjustInto(base);
            } else {
                base = getStartDate().minusWeeks(getIncrement());
            }
        }

        @Override
        public LocalDate next() {
            if (isEnabled()) {
                final DayOfWeek dayOfWeek = DayOfWeek.from(getStartDate());

                base = base.plusWeeks(getIncrement());
                base = (LocalDate) dayOfWeek.adjustInto(base);

                if (getEndDate() == null || DateUtils.before(base, getEndDate())) {
                    return base;
                }
            }
            return null;
        }
    }

}