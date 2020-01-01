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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import jgnash.engine.Config;
import jgnash.engine.dao.ConfigDAO;

/**.
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
        Config defaultConfig = null;

        try {
            Future<Config> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    Config newConfig;
                    try {
                        final CriteriaBuilder cb = em.getCriteriaBuilder();
                        final CriteriaQuery<Config> cq = cb.createQuery(Config.class);
                        final Root<Config> root = cq.from(Config.class);
                        cq.select(root);

                        final TypedQuery<Config> q = em.createQuery(cq);

                        newConfig = q.getSingleResult();

                    } catch (final Exception e) {
                        newConfig = new Config();

                        em.getTransaction().begin();
                        em.persist(newConfig);
                        em.getTransaction().commit();

                        logger.info("Generating new default config");
                    }
                    return newConfig;
                } finally {
                    emLock.unlock();
                }
            });

            defaultConfig = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        return defaultConfig;
    }

    @Override
    public void update(final Config config) {
        persist(config);
    }
}
