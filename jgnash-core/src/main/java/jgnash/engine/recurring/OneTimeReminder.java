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
package jgnash.engine.recurring;

import java.util.Date;

import javax.persistence.Entity;

/**
 * A one time only reminder
 *
 * @author Craig Cavanaugh
 */
@Entity
public class OneTimeReminder extends Reminder {

    public OneTimeReminder() {
    }

    /**
     * @see jgnash.engine.recurring.Reminder#getIterator()
     */
    @Override
    public RecurringIterator getIterator() {
        return new OneTimeIterator();
    }

    /**
     * @see jgnash.engine.recurring.Reminder#getReminderType()
     */
    @Override
    public ReminderType getReminderType() {
        return ReminderType.ONETIME;
    }

    private class OneTimeIterator implements RecurringIterator {

        private boolean end = false; // one time only trigger

        /**
         * @see jgnash.engine.recurring.RecurringIterator#next()
         */
        @Override
        public Date next() {
            if (isEnabled()) {
                if (getLastDate() == null && !end) {
                    end = true;
                    return getStartDate();
                }
            }
            return null;
        }
    }

}