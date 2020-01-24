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
package jgnash.engine.dao;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import jgnash.engine.StoredObject;

/**
 * Basic DAO.
 *
 * @author Craig Cavanaugh
 */
public abstract class AbstractDAO implements DAO {

    protected final AtomicBoolean dirtyFlag = new AtomicBoolean(false);

    protected static <T extends StoredObject> List<T> stripMarkedForRemoval(final List<T> list) {
        list.removeIf(StoredObject::isMarkedForRemoval);
        return list;
    }

    @Override
    public boolean isDirty() {
        return dirtyFlag.get();
    }
}
