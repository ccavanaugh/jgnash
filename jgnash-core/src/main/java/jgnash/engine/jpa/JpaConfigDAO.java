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

import jgnash.engine.Config;
import jgnash.engine.dao.ConfigDAO;

import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Config DAO
 *
 * @author Craig Cavanaugh
 */
class JpaConfigDAO extends AbstractJpaDAO implements ConfigDAO {

    private static final Logger logger = Logger.getLogger(JpaConfigDAO.class.getName());

    JpaConfigDAO(final EntityManager entityManager, final boolean isRemote) {
        super(entityManager, isRemote);
    }

    /*
     * @see jgnash.engine.ConfigDAOInterface#getDefaultConfig()
     */
    @Override
    public synchronized Config getDefaultConfig() {
        try {
            emLock.lock();
            Config defaultConfig;

            try {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Config> cq = cb.createQuery(Config.class);
                Root<Config> root = cq.from(Config.class);
                cq.select(root);

                TypedQuery<Config> q = em.createQuery(cq);

                defaultConfig = q.getSingleResult();

            } catch (Exception e) {
                defaultConfig = new Config();
                em.persist(defaultConfig);
                logger.info("Generating new default config");
            }

            return defaultConfig;
        } finally {
            emLock.unlock();
        }
    }

    @Override
    public void update(final Config config) {
        try {
            emLock.lock();
            em.getTransaction().begin();

            em.merge(config);

            em.getTransaction().commit();
        } finally {
            emLock.unlock();
        }
    }
}
