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
package jgnash.engine.jpa;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import jgnash.engine.Tag;
import jgnash.engine.dao.TagDAO;

/**
 * Tag DAO.
 *
 * @author Craig Cavanaugh
 */
class JpaTagDAO extends AbstractJpaDAO implements TagDAO {

    JpaTagDAO(final EntityManager entityManager, final boolean isRemote) {
        super(entityManager, isRemote);
    }

    @Override
    public boolean add(final Tag tag) {
        return persist(tag);
    }

    @Override
    public boolean update(final Tag tag) {
        return merge(tag) != null;
    }

    @Override
    public Set<Tag> getTags() {
        return new HashSet<>(query(Tag.class));
    }
}
