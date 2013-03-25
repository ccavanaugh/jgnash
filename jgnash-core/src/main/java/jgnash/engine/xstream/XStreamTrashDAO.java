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
package jgnash.engine.xstream;

import java.util.List;
import java.util.logging.Logger;

import jgnash.engine.StoredObject;
import jgnash.engine.TrashObject;
import jgnash.engine.dao.TrashDAO;

/**
 * XML trash DAO
 *
 * @author Craig Cavanaugh
 */
public class XStreamTrashDAO extends AbstractXStreamDAO implements TrashDAO {

    private static final Logger logger = Logger.getLogger(XStreamTrashDAO.class.getName());

    XStreamTrashDAO(final AbstractXStreamContainer container) {
        super(container);
    }

    @Override
    public List<TrashObject> getTrashObjects() {
        return container.query(TrashObject.class);
    }

    @Override
    public void add(final TrashObject trashObject) {
        container.set(trashObject);
        commit();
    }

    @Override
    public void remove(final TrashObject trashObject) {
        StoredObject storedObject = getObjectByUuid(trashObject.getUuid());

        container.delete(storedObject);
        container.delete(trashObject);

        commit();

        logger.info("Removed TrashObject");
    }
}
