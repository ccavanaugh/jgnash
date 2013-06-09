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
package jgnash.engine.jpa;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * A Trash entity for generic cleanup of typed entities that need to be cleanup up
 * at a later date because of how JPA operates, but the object is not a StoredObject
 *
 * @author Craig Cavanaugh
 */
@Entity
public class JpaTrashEntity {

    @SuppressWarnings("unused")
    @Id @GeneratedValue(strategy= GenerationType.TABLE)
    private long id;

    private long entityId;

    /**
     * Date object was added
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date date = new Date();

    private String className;

    /**
     * No argument constructor for reflection purposes.
     * <b>Do not use to create a new instance</b>
     */
    @SuppressWarnings("unused")
    private JpaTrashEntity() {

    }

    public JpaTrashEntity(final Object entity, final long entityId) {
        className = entity.getClass().getName();
        this.entityId = entityId;
    }

    public Date getDate() {
        return date;
    }

    public String getClassName() {
        return className;
    }

    public long getEntityId() {
        return entityId;
    }
}
