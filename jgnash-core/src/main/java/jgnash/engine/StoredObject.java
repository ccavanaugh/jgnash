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
package jgnash.engine;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Version;

import static jgnash.util.LogUtil.logSevere;

/**
 * Abstract class for anything stored in the database that requires a unique id.
 *
 * @author Craig Cavanaugh
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class StoredObject implements Cloneable, Serializable {

    /**
     * Version field for persistence purposes.
     */
    @Version
    @SuppressWarnings("unused")
    private int version;

    /**
     * Indicates object is marked for removal.
     */
    @Basic
    private boolean markedForRemoval = false;

    /**
     * String based Unique ID for every object.
     */
    @Id
    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid = UUID.randomUUID();

    /** Cache the hash code for the StoredObject */
    private transient int hash = 0;

    /**
     * Getter for the uuid.
     *
     * Note: method should not be final (HHH000305)
     *
     * @return uuid of the object
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Setter for the uuid.  Used for reflection purposes only
     *
     * @param uuid uuid to assign the object
     */
    private void setUuid(final UUID uuid) {
        this.uuid = uuid;
    }

    void setMarkedForRemoval() {
        this.markedForRemoval = true;
    }

    public boolean isMarkedForRemoval() {
        return markedForRemoval;
    }

    /**
     * Override hashCode to use UUID hash code.
     *
     * The hash is cached since UUID does not cache
     *
     * @see java.util.UUID#hashCode()
     */
    @Override
    public int hashCode() {
        int h = hash;

        if (h == 0) {
            hash = h = getUuid().hashCode();
        }

        return h;
    }

    /**
     * Default equals override.
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
            o.setUuid(UUID.randomUUID());   // force a new UUID
            o.hash = 0;
            o.markedForRemoval = false;
        } catch (final CloneNotSupportedException e) {
            logSevere(StoredObject.class, e);
        }

        return o;
    }
}
