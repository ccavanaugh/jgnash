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
 * A monthly reminder / iterator. Dates get a little weird when iterating by DAY
 * and the day is early or late in the month. Months may be skipped or multiple
 * days in the same month. Iterating by DATE will be much more consistent.
 * 
 * @author Craig Cavanaugh
 */
public class MonthlyReminder extends Reminder {

    private static final long serialVersionUID = 11516039436649194L;

    /**
     * Defines if increment is by day of the week or day of the month
     */
    private int type = DATE;

    private static final int DATE = 0;

    private static final int DAY = 1;

    public MonthlyReminder() {
    }

    /**
     * @see jgnash.engine.recurring.Reminder#getIterator()
     */
    @Override
    public RecurringIterator getIterator() {
        return new MonthlyIterator();
    }

    /**
     * @return Returns the type.
     */
    public int getType() {
        return type;
    }

    /**
     * @return Return reminder type
     * @see jgnash.engine.recurring.Reminder#getReminderType()
     */
    @Override
    public ReminderType getReminderType() {
        return ReminderType.MONTHLY;
    }

    /**
     * @param type The type to set.
     */
    public void setType(int type) {
        if (type == DATE || type == DAY) {
            this.type = type;
        }
    }

    private class MonthlyIterator implements RecurringIterator {
        private final Calendar calendar = Calendar.getInstance();

        public MonthlyIterator() {
            if (getLastDate() != null) {
                calendar.setTime(getLastDate()); // set the last execute date

                // adjust for actual target date, it could have been modified since the last date                
                calendar.set(Calendar.DAY_OF_MONTH,  DateUtils.getDayOfTheMonth(getStartDate()));
            } else {
                if (type == DATE) {
                    calendar.setTime(getStartDate());
                    calendar.add(Calendar.MONTH, getIncrement() * -1);
                } else if (type == DAY) {
                    calendar.setTime(getStartDate());
                    int week = calendar.get(Calendar.WEEK_OF_MONTH);
                    int day = calendar.get(Calendar.DAY_OF_WEEK);

                    calendar.add(Calendar.MONTH, getIncrement() * -1);
                    calendar.set(Calendar.WEEK_OF_MONTH, week);
                    calendar.set(Calendar.DAY_OF_WEEK, day);
                }
            }
        }

        /**
         * @see jgnash.engine.recurring.RecurringIterator#next()
         */
        @Override
        public Date next() {

            if (type == DATE) {
                calendar.add(Calendar.MONTH, getIncrement());
            } else {
                int week = calendar.get(Calendar.WEEK_OF_MONTH);
                int day = calendar.get(Calendar.DAY_OF_WEEK);

                calendar.add(Calendar.MONTH, getIncrement());
                calendar.set(Calendar.WEEK_OF_MONTH, week);
                calendar.set(Calendar.DAY_OF_WEEK, day);
            }
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