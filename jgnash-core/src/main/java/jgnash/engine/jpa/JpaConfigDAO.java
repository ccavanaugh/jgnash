/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
        Config defaultConfig = null;

        emLock.lock();

        try {
            Future<Config> future = executorService.submit(() -> {
                Config defaultConfig1;
                try {
                    CriteriaBuilder cb = em.getCriteriaBuilder();
                    CriteriaQuery<Config> cq = cb.createQuery(Config.class);
                    Root<Config> root = cq.from(Config.class);
                    cq.select(root);

                    TypedQuery<Config> q = em.createQuery(cq);

                    defaultConfig1 = q.getSingleResult();

                } catch (Exception e) {
                    defaultConfig1 = new Config();
                    em.persist(defaultConfig1);
                    logger.info("Generating new default config");
                }
                return defaultConfig1;
            });

            defaultConfig = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }

        return defaultConfig;
    }

    @Override
    public void update(final Config config) {

        merge(config);

        /*emLock.lock();

        try {
            Future<Void> future = executorService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    em.getTransaction().begin();
                    em.persist(config);
                    em.getTransaction().commit();
                    return null;
                }
            });

            future.get(); // block
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }*/
    }
}
