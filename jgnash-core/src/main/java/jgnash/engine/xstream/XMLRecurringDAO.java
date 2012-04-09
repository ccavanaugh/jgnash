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
package jgnash.engine.xstream;

import java.util.List;

import jgnash.engine.dao.RecurringDAO;
import jgnash.engine.recurring.Reminder;

/**
 * Recurring XML DAO
 *
 * @author Craig Cavanaug
 */
public class XMLRecurringDAO extends AbstractXMLDAO implements RecurringDAO {

    XMLRecurringDAO(final AbstractXStreamContainer container) {
        super(container);
    }

    @Override
    public List<Reminder> getReminderList() {
        return stripMarkedForRemoval(container.query(Reminder.class));
    }

    @Override
    public boolean addReminder(final Reminder reminder) {
        container.set(reminder);
        commit();
        return true;
    }

    @Override
    public void refreshReminder(final Reminder reminder) {
        // do nothing for this DAO
    }

    @Override
    public boolean updateReminder(final Reminder reminder) {
        commit();
        return true;
    }
}
