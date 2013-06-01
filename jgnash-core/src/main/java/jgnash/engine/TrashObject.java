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
package jgnash.engine;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Wraps objects that have been removed from active use in the engine.
 *
 * @author Craig Cavanaugh
 */
@Entity
public class TrashObject extends StoredObject implements Comparable<TrashObject> {

    private static final long serialVersionUID = -5923174140959126059L;

    /**
     * Date object was added
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date date = new Date();

    /**
     * The stored object
     */
    @OneToOne(orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private StoredObject object;

    /**
     * Public no-argument constructor for reflection purposes
     */
    @SuppressWarnings("unused")
    public TrashObject() {
    }

    TrashObject(final StoredObject object) {
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

    @Override
    public int compareTo(final TrashObject o) {
        return date.compareTo(o.date);
    }
}
