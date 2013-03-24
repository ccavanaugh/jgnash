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

import javax.persistence.*;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class for anything stored in the database that requires a unique id
 *
 * @author Craig Cavanaugh
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class StoredObject implements Cloneable, Serializable {

    private static final long serialVersionUID = -6989773226655555899L;

    /**
     * Indicates object is marked for removal
     */
    @Basic
    private boolean markedForRemoval = false;

    /**
     * Unique ID for every object
     */
    @Id()
    @Column(nullable = false)
    private String uuid = UUIDUtil.getUID();

    /**
     * Getter for the uuid.
     *
     * @return uuid of the object
     */
    public final String getUuid() {
        return uuid;
    }

    /**
     * Setter for the uuid.  Used for reflection purposes only
     *
     * @param uuid uuid to assign the object
     */
    private void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    void setMarkedForRemoval(final boolean markedForRemoval) {
        this.markedForRemoval = markedForRemoval;
    }

    public boolean isMarkedForRemoval() {
        return markedForRemoval;
    }

    /**
     * Override hashCode to use uuid
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getUuid().hashCode();
    }

    /**
     * Default equals override
     *
     * @param o object to compare
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object o) {
        return this == o || o instanceof StoredObject && getUuid().equals(((StoredObject) o).getUuid());

    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        StoredObject o = null;

        try {
            o = (StoredObject) super.clone();
            o.setUuid(UUIDUtil.getUID());   // force a unique id
        } catch (CloneNotSupportedException e) {
            Logger.getLogger(StoredObject.class.getName()).log(Level.SEVERE, e.toString(), e);
        }

        return o;
    }
}
