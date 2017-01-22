/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import jgnash.engine.dao.RecurringDAO;
import jgnash.engine.recurring.Reminder;

/**
 * JPA Reminder DAO.
 *
 * @author Craig Cavanaugh
 */
class JpaRecurringDAO extends AbstractJpaDAO implements RecurringDAO {

    private static final Logger logger = Logger.getLogger(JpaRecurringDAO.class.getName());

    JpaRecurringDAO(final EntityManager entityManager, final boolean isRemote) {
        super(entityManager, isRemote);
    }

    /*
     * @see jgnash.engine.ReminderDAOInterface#getReminderList()
     */
    @Override
    public List<Reminder> getReminderList() {

        List<Reminder> reminderList = Collections.emptyList();

        try {
            final Future<List<Reminder>> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    final CriteriaBuilder cb = em.getCriteriaBuilder();
                    final CriteriaQuery<Reminder> cq = cb.createQuery(Reminder.class);
                    final Root<Reminder> root = cq.from(Reminder.class);
                    cq.select(root);

                    final TypedQuery<Reminder> q = em.createQuery(cq);

                    return stripMarkedForRemoval(new ArrayList<>(q.getResultList()));
                } finally {
                    emLock.unlock();
                }
            });

            reminderList = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        return reminderList;
    }

    /*
     * @see jgnash.engine.ReminderDAOInterface#addReminder(jgnash.engine.recurring.Reminder)
     */
    @Override
    public boolean addReminder(final Reminder reminder) {
        return persist(reminder);
    }

    @Override
    public Reminder getReminderByUuid(final String uuid) {
        return getObjectByUuid(Reminder.class, uuid);
    }


    @Override
    public boolean updateReminder(final Reminder reminder) {
        return addReminder(reminder);   // call add, same code
    }
}
