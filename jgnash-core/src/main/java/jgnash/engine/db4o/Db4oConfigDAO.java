/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
package jgnash.engine.db4o;

import com.db4o.ObjectContainer;
import jgnash.engine.Config;
import jgnash.engine.dao.ConfigDAO;

import java.util.List;
import java.util.logging.Logger;

/**
 * Hides all the db4o config code
 *
 * @author Craig Cavanaugh
 *
 */
class Db4oConfigDAO extends AbstractDb4oDAO implements ConfigDAO {

    private static final String CONFIG_SEMAPHORE = "ConfigLock";

    private final Logger logger = Logger.getLogger(Db4oConfigDAO.class.getName());

    Db4oConfigDAO(final ObjectContainer container, final boolean isRemote) {
        super(container, isRemote);
    }

    /*
     * @see jgnash.engine.ConfigDAOInterface#getDefaultConfig()
     */
    @Override
    public synchronized Config getDefaultConfig() {
        Config defaultConfig = null;

        if (container.ext().setSemaphore(CONFIG_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {

            List<Config> list = container.query(Config.class);

            if (!list.isEmpty()) {
                defaultConfig = list.get(0);
            }

            if (defaultConfig == null) {
                defaultConfig = new Config();
                container.set(defaultConfig);
                commit();
                logger.info("Generating new default config");
            }
            container.ext().releaseSemaphore(CONFIG_SEMAPHORE);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return defaultConfig;
    }

    @Override
    public void commit(Config config) {
        if (container.ext().setSemaphore(CONFIG_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.set(config);
            commit();
            container.ext().releaseSemaphore(CONFIG_SEMAPHORE);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }
    }
}
