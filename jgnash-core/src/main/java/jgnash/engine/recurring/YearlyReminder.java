/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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

import java.util.Calendar;
import java.util.Date;

import jgnash.util.DateUtils;

/**
 * A yearly reminder
 * 
 * @author Craig Cavanaugh
 * 
 */
public class YearlyReminder extends Reminder {

    private static final long serialVersionUID = 8173755092984444499L;

    public YearlyReminder() {

    }

    /**
     * @see jgnash.engine.recurring.Reminder#getIterator()
     */
    @Override
    public RecurringIterator getIterator() {
        return new YearlyIterator();
    }

    /**
     * @return Return reminder type
     * @see jgnash.engine.recurring.Reminder#getReminderType()
     */
    @Override
    public ReminderType getReminderType() {
        return ReminderType.YEARLY;
    }

    private class YearlyIterator implements RecurringIterator {
        private final Calendar calendar = Calendar.getInstance();

        public YearlyIterator() {
            if (getLastDate() != null) {
                calendar.setTime(getLastDate());

                // adjust for actual target date, it could have been modified since the last date                
                calendar.set(Calendar.DAY_OF_YEAR, DateUtils.getDayOfTheYear(getStartDate()));
            } else {
                calendar.setTime(getStartDate());
                calendar.add(Calendar.YEAR, getIncrement() * -1);
            }
        }

        /**
         * @see jgnash.engine.recurring.RecurringIterator#next()
         */
        @Override
        public Date next() {
            calendar.add(Calendar.YEAR, getIncrement());
            Date date = calendar.getTime();

            if (getEndDate() == null) {
                return date;
            } else if (DateUtils.before(date, getEndDate())) {
                return date;
            }
            return null;
        }
    }

}