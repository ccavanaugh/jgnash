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
package jgnash.engine.db4o;

import com.db4o.ObjectContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import jgnash.engine.budget.Budget;
import jgnash.engine.dao.BudgetDAO;

/**
 * Db4o Budget DAO
 *
 * @author Craig Cavanaugh
 *
 */

public class Db4oBudgetDAO extends AbstractDb4oDAO implements BudgetDAO {

    private final static Logger logger = Logger.getLogger(Db4oBudgetDAO .class.getName());

    Db4oBudgetDAO(final ObjectContainer container, final boolean isRemote) {
        super(container, isRemote);
    }

    @Override
    public boolean add(final Budget budget) {
       boolean result = false;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.set(budget);
            commit();
            result = true;
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    @Override
    public boolean update(final Budget budget) {
        boolean result = false;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.set(budget);
            commit();
            result = true;
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    @Override
    public List<Budget> getBudgets() {
        List<Budget> list = Collections.emptyList();
        List<Budget> resultList = new ArrayList<>();

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            list = container.query(Budget.class);
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        /* Flush any that have been marked for removal
         * Returned list for query does not support iterator interface */
        for (Budget budget : list) {
            if (!budget.isMarkedForRemoval()) {
                resultList.add(budget);
            }
        }
        return resultList;
    }

    @Override
    public void refreshBudget(final Budget budget) {
        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.ext().refresh(budget, 4);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }
    }
}
