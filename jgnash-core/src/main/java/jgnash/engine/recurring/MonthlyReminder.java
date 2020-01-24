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
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Locale;

import javax.persistence.Entity;

import jgnash.time.DateUtils;

/**
 * A monthly reminder / iterator. Dates get a little weird when iterating by DAY
 * and the day is early or late in the month. Months may be skipped or multiple
 * days in the same month. Iterating by DATE will be much more consistent.
 * 
 * @author Craig Cavanaugh
 */
@Entity
public class MonthlyReminder extends Reminder {

    /**
     * Defines if increment is by day of the week or day of the month.
     */
    private int type = DATE;

    static final int DATE = 0;

    static final int DAY = 1;

    public MonthlyReminder() {
    }

    @Override
    public RecurringIterator getIterator() {
        return new MonthlyIterator();
    }

    /**
     * Returns the Monthly Reminder type.
     *
     * @return Returns the type.
     */
    public int getType() {
        return type;
    }

    @Override
    public ReminderType getReminderType() {
        return ReminderType.MONTHLY;
    }

    /**
     * Sets the Monthly Reminder type.
     *
     * @param type The type to set.
     */
    public void setType(int type) {
        if (type == DATE || type == DAY) {
            this.type = type;
        }
    }

    private class MonthlyIterator implements RecurringIterator {
        private LocalDate base;

        MonthlyIterator() {
            if (getLastDate() != null) {

                base = getLastDate();

                final TemporalField weekOfMonth = WeekFields.of(Locale.getDefault()).weekOfMonth();

                final int week = base.get(weekOfMonth);     // extract the current week
                final DayOfWeek day = base.getDayOfWeek();  // extract the current day of the week

                base = base.with(weekOfMonth, week);        // force the week of the month
                base = base.with(day);                      // force the day of the week
            } else {
                if (type == DATE) {
                    base = getStartDate().minusMonths(getIncrement());
                } else if (type == DAY) {
                    base = getStartDate();

                    final TemporalField weekOfMonth = WeekFields.of(Locale.getDefault()).weekOfMonth();

                    final int week = base.get(weekOfMonth);             // extract the current week
                    final DayOfWeek day = base.getDayOfWeek();          // extract the current day of the week

                    base = getStartDate().minusMonths(getIncrement());  // decrement the month
                    base = base.with(weekOfMonth, week);                // force the week of the month
                    base = base.with(day);                              // force the day of the week
                }
            }
        }

        @Override
        public LocalDate next() {
            if (isEnabled()) {
                if (type == DATE) {
                    base = base.plusMonths(getIncrement());
                } else {
                    final TemporalField weekOfMonth = WeekFields.of(Locale.getDefault()).weekOfMonth();

                    final int week = base.get(weekOfMonth);     // extract the current week
                    final DayOfWeek day = base.getDayOfWeek();  // extract the current day of the week

                    base = base.plusMonths(getIncrement());     // increment the month
                    base = base.with(weekOfMonth, week);        // force the week of the month
                    base = base.with(day);                      // force the day of the week

                    // if plusMonths resulted in an invalid date and adjusted to the prior week, bump it a week
                    if (base.get(weekOfMonth) > week) {
                        base = base.plusWeeks(1);
                        base = base.with(weekOfMonth, week);        // force the week of the month
                        base = base.with(day);                      // force the day of the week
                    }
                }

                if (getEndDate() == null || DateUtils.before(base, getEndDate())) {
                    return base;
                }
            }
            return null;
        }
    }
}