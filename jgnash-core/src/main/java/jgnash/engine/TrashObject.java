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
package jgnash.engine;

import java.util.Date;

/**
 * Wraps objects that have been removed from active use in the engine.
 *
 * @author Craig Cavanaugh
 *
 */
public class TrashObject extends StoredObject {

    private static final long serialVersionUID = -5923174140959126059L;

    /**
     * Date object was added
     */
    private final Date date = new Date();

    /**
     * The stored object
     */
    private StoredObject object;

    /**
     * Public no-argument constructor for reflection purposes
     *
     * @deprecated
     */
    @Deprecated
    public TrashObject() {
    }

    protected TrashObject(StoredObject object) {
        this.object = object;
        object.setMarkedForRemoval(true);
        setMarkedForRemoval(true);
    }

    public StoredObject getObject() {
        return object;
    }

    public Date getDate() {
        return date;
    }
}
