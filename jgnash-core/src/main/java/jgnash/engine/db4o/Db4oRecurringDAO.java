/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
package jgnash.engine.db4o;

import com.db4o.ObjectContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import jgnash.engine.dao.RecurringDAO;
import jgnash.engine.recurring.Reminder;

/**
 * Reminder DAO
 *
 * @author Craig Cavanaugh
 */
class Db4oRecurringDAO extends AbstractDb4oDAO implements RecurringDAO {

    private static final String SEMAPHORE_NAME = "ReminderLock";

    private static final Logger logger = Logger.getLogger(Db4oRecurringDAO.class.getName());

    Db4oRecurringDAO(final ObjectContainer container, final boolean isRemote) {
        super(container, isRemote);
    }

    /*
     * @see jgnash.engine.ReminderDAOInterface#getReminderList()
     */
    @Override
    public List<Reminder> getReminderList() {

        if (container.ext().setSemaphore(SEMAPHORE_NAME, SEMAPHORE_WAIT_TIME)) {

            List<Reminder> list = container.query(Reminder.class);

            if (list != null) {
                list = new ArrayList<>(list);

                Iterator<Reminder> i = list.iterator();

                while (i.hasNext()) {
                    if (i.next().isMarkedForRemoval()) {
                        i.remove();
                    }
                }
            } else {
                list = Collections.emptyList();
            }

            container.ext().releaseSemaphore(SEMAPHORE_NAME);
            return list;
        }

        logger.severe(SEMAPHORE_WARNING);
        return Collections.emptyList();
    }

    /*
     * @see jgnash.engine.ReminderDAOInterface#addReminder(jgnash.engine.recurring.Reminder)
     */
    @Override
    public boolean addReminder(Reminder reminder) {
        boolean result = false;

        if (container.ext().setSemaphore(SEMAPHORE_NAME, SEMAPHORE_WAIT_TIME)) {
            container.set(reminder);
            commit();

            container.ext().releaseSemaphore(SEMAPHORE_NAME);
            result = true;
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    @Override
    public boolean updateReminder(Reminder reminder) {
        boolean result = false;

        if (container.ext().setSemaphore(SEMAPHORE_NAME, SEMAPHORE_WAIT_TIME)) {
            container.set(reminder);
            commit();

            container.ext().releaseSemaphore(SEMAPHORE_NAME);
            result = true;
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    @Override
    public void refreshReminder(Reminder reminder) {
        container.ext().refresh(reminder, 2);
    }
}
