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

import java.time.LocalDateTime;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;

import jgnash.util.NotNull;

/**
 * Wraps objects that have been removed from active use in the engine.
 *
 * @author Craig Cavanaugh
 */
@Entity
public class TrashObject extends StoredObject implements Comparable<TrashObject> {

    /**
     * Date object was added.
     */
    private final LocalDateTime date = LocalDateTime.now();

    /**
     * The stored object.
     */
    @OneToOne(orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private StoredObject object;

    /**
     * Public no-argument constructor for reflection purposes.
     */
    @SuppressWarnings("unused")
    public TrashObject() {
    }

    TrashObject(final StoredObject object) {
        this.object = object;
        object.setMarkedForRemoval();
        setMarkedForRemoval();
    }

    public StoredObject getObject() {
        return object;
    }

    public LocalDateTime getDate() {
        return date;
    }

    @Override
    public int compareTo(@NotNull final TrashObject o) {
        return date.compareTo(o.date);
    }
}
