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

import jgnash.engine.StoredObject;
import jgnash.engine.dao.AbstractDAO;

import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

/**
 * Abstract DAO
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractJpaDAO extends AbstractDAO {

    static final ReentrantLock emLock = new ReentrantLock();

    EntityManager em;

    boolean isRemote = false;

    AbstractJpaDAO(final EntityManager entityManager, final boolean isRemote) {
        assert entityManager != null;

        this.isRemote = isRemote;
        em = entityManager;
    }

    public <T> T getObjectByUuid(final Class<T> tClass, final String uuid) {
        T object = null;

        try {
            emLock.lock();

            object = em.find(tClass, uuid);
        } catch (NoResultException e) {
            Logger.getLogger(AbstractJpaDAO.class.getName()).info("Did not find " + tClass.getName() + " for uuid: " + uuid);
        } finally {
            emLock.unlock();
        }

        return object;
    }

    public StoredObject getObjectByUuid(final String uuid) {
        return getObjectByUuid(StoredObject.class, uuid);
    }
}
