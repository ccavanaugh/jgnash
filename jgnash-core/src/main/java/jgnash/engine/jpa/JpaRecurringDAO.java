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
package jgnash.engine.jpa;


import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;

import jgnash.engine.dao.RecurringDAO;
import jgnash.engine.recurring.Reminder;

/**
 * JPA Reminder DAO.
 *
 * @author Craig Cavanaugh
 */
class JpaRecurringDAO extends AbstractJpaDAO implements RecurringDAO {

    JpaRecurringDAO(final EntityManager entityManager, final boolean isRemote) {
        super(entityManager, isRemote);
    }

    /*
     * @see jgnash.engine.ReminderDAOInterface#getReminderList()
     */
    @Override
    public List<Reminder> getReminderList() {
        return query(Reminder.class);
    }

    /*
     * @see jgnash.engine.ReminderDAOInterface#addReminder(jgnash.engine.recurring.Reminder)
     */
    @Override
    public boolean addReminder(final Reminder reminder) {
        return persist(reminder);
    }

    @Override
    public Reminder getReminderByUuid(final UUID uuid) {
        return getObjectByUuid(Reminder.class, uuid);
    }


    @Override
    public boolean updateReminder(final Reminder reminder) {
        return addReminder(reminder);   // call add, same code
    }
}
