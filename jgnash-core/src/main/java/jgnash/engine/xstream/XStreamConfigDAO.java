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
package jgnash.engine.xstream;

import java.util.List;
import java.util.logging.Logger;

import jgnash.engine.Config;
import jgnash.engine.dao.ConfigDAO;

/**
 * Config object DAO.
 *
 * @author Craig Cavanaugh
 */
class XStreamConfigDAO extends AbstractXStreamDAO implements ConfigDAO {

    private static final Logger logger = Logger.getLogger(XStreamConfigDAO.class.getName());

    XStreamConfigDAO(final AbstractXStreamContainer container) {
        super(container);
    }

    @Override
    public Config getDefaultConfig() {
        Config defaultConfig = null;

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

        return defaultConfig;
    }

    @Override
    public void update(final Config config) {
        container.set(config);
        commit();
    }
}
