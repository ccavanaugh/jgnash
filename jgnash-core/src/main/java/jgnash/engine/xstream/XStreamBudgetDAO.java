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
package jgnash.engine.xstream;

import jgnash.engine.budget.Budget;
import jgnash.engine.dao.BudgetDAO;

import java.util.List;
import java.util.UUID;

/**
 * XML Budget DAO.
 *
 * @author Craig Cavanaugh
 */
class XStreamBudgetDAO extends AbstractXStreamDAO implements BudgetDAO {

    XStreamBudgetDAO(final AbstractXStreamContainer container) {
        super(container);
    }

    @Override
    public boolean add(final Budget budget) {
        container.set(budget);
        commit();

        return true;
    }

    @Override
    public boolean update(final Budget budget) {
        container.set(budget);
        commit();

        return true;
    }

    @Override
    public List<Budget> getBudgets() {
        return stripMarkedForRemoval(container.query(Budget.class));
    }

    @Override
    public Budget getBudgetByUuid(final UUID uuid) {
        return getObjectByUuid(Budget.class, uuid);
    }
}
