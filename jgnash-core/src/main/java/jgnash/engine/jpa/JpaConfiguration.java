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

import java.util.Properties;

/**
 * Utility class to help with JPA configuration
 *
 * @author Craig Cavanaugh
 */
public class JpaConfiguration {

    public static final String JAVAX_PERSISTENCE_JDBC_URL = "javax.persistence.jdbc.url";


    private static Properties getBaseProperties() {
        Properties properties = System.getProperties();

        properties.setProperty("javax.persistence.jdbc.driver", "org.h2.Driver");

        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "update");

        return properties;
    }

    protected static Properties getLocalProperties(final String fileName, final String user, final String password, boolean readOnly) {
        Properties properties = getBaseProperties();

        String url = "jdbc:h2:" + jgnash.util.FileUtils.stripFileExtension(fileName);

        if (readOnly) {
            url += ";ACCESS_MODE_DATA=r";
        }

        if (user != null && user.length() > 0) {
            url += (";USER=" + user);
        }

        if (password != null && password.length() > 0) {
            url += (";PASSWORD=" + password);
        }

        properties.setProperty(JAVAX_PERSISTENCE_JDBC_URL, url);
        properties.setProperty("javax.persistence.jdbc.user", user);
        properties.setProperty("javax.persistence.jdbc.password", password);

        return properties;
    }

    protected static Properties getClientProperties(final String fileName, final String host, int port, final String user, final String password) {
        Properties properties = getBaseProperties();

        properties.setProperty("javax.persistence.jdbc.user", user);
        properties.setProperty("javax.persistence.jdbc.password", password);

        StringBuilder builder = new StringBuilder("jdbc:h2:ssl://");
        builder.append(host).append(":").append(port);
        builder.append(fileName);

        builder.append(";USER=").append(user);
        builder.append(";PASSWORD=").append(password);

        properties.setProperty(JAVAX_PERSISTENCE_JDBC_URL, builder.toString());

        return properties;
    }
}
