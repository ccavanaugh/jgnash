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
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

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

            em.persist(budget);

            em.getTransaction().commit();

            return true;
        } finally {
            emLock.unlock();
        }
    }

    @Override
    public List<Budget> getBudgets() {
        try {
            emLock.lock();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Budget> cq = cb.createQuery(Budget.class);
            Root<Budget> b = cq.from(Budget.class);
            cq.select(b);

            TypedQuery<Budget> q = em.createQuery(cq);

            return stripMarkedForRemoval(new ArrayList<>(q.getResultList()));
        } finally {
            emLock.unlock();
        }
    }

    @Override
    public Budget getBudgetByUuid(final String uuid) {
        /*try {
            emLock.lock();

            Budget budget = null;

            try {
                budget = em.find(Budget.class, uuid);
            } catch (Exception e) {
                logger.info("Did not find Budget for uuid: " + uuid);
            }

            return budget;
        } finally {
            emLock.unlock();
        }*/
        return getObjectByUuid(Budget.class, uuid);
    }

    @Override
    public void refreshBudget(final Budget budget) {
        try {
            emLock.lock();

            em.getTransaction().begin();
            em.merge(budget);
            em.getTransaction().commit();
        } finally {
            emLock.unlock();
        }
    }
}
