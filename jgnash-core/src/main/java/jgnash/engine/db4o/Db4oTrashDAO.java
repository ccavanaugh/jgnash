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
import java.util.List;
import java.util.logging.Logger;

import jgnash.engine.TrashObject;
import jgnash.engine.dao.TrashDAO;

/**
 * Db4o trash DAO
 *
 * @author Craig Cavanaugh
 */
class Db4oTrashDAO extends AbstractDb4oDAO implements TrashDAO {

    private static final Logger logger = Logger.getLogger(Db4oTrashDAO.class.getName());

    Db4oTrashDAO(final ObjectContainer container, final boolean isRemote) {
        super(container, isRemote);
    }

    @Override
    public List<TrashObject> getTrashObjects() {
        return new ArrayList<>(container.query(TrashObject.class));
    }

    @Override
    public void add(TrashObject trashObject) {
        container.set(trashObject);
        commit();
    }

    @Override
    public void remove(TrashObject trashObject) {
        container.delete(trashObject.getObject());
        container.ext().purge(trashObject.getObject());
        container.delete(trashObject);
        container.ext().purge(trashObject);

        commit();

        logger.info("Removed TrashObject");
    }
}
