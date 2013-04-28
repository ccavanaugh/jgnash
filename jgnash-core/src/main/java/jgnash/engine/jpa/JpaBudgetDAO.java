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
package jgnash.engine.jpa;

import jgnash.engine.budget.Budget;
import jgnash.engine.dao.BudgetDAO;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 * Budget DAO
 *
 * @author Craig Cavanaugh
 */
public class JpaBudgetDAO extends AbstractJpaDAO implements BudgetDAO {

    // private final static Logger logger = Logger.getLogger(JpaBudgetDAO.class.getName());

    JpaBudgetDAO(final EntityManager entityManager, final boolean isRemote) {
        super(entityManager, isRemote);
    }

    @Override
    public boolean add(final Budget budget) {
        try {
            emLock.lock();

            em.getTransaction().begin();

            em.persist(budget);

            em.getTransaction().commit();

            return true;
        } finally {
            emLock.unlock();
        }
    }

    @Override
    public boolean update(final Budget budget) {

        try {
            emLock.lock();
            em.getTransaction().begin();

            em.merge(budget);

            em.getTransaction().commit();

            return true;
        } finally {
            emLock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Budget> getBudgets() {
        try {
            emLock.lock();

            Query q = em.createQuery("SELECT b FROM Budget b WHERE b.markedForRemoval = false");

            // result lists are readonly
            return new ArrayList<Budget>(q.getResultList());

        } finally {
            emLock.unlock();
        }
    }

    @Override
    public Budget getBudgetByUuid(final String uuid) {
        return getObjectByUuid(Budget.class, uuid);
    }

    @Override
    public void refreshBudget(final Budget budget) {
        try {
            emLock.lock();

            em.getTransaction().begin();
            em.refresh(budget);
            em.getTransaction().commit();
        } finally {
            emLock.unlock();
        }
    }
}
