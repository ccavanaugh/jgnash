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

import java.util.Arrays;
import java.util.Properties;

/**
 * Utility class to help with JPA configuration
 *
 * @author Craig Cavanaugh
 */
public class JpaConfiguration {

    public static final String JAVAX_PERSISTENCE_JDBC_URL = "javax.persistence.jdbc.url";
    public static final String JAVAX_PERSISTENCE_JDBC_DRIVER = "javax.persistence.jdbc.driver";
    public static final String JAVAX_PERSISTENCE_JDBC_USER = "javax.persistence.jdbc.user";
    public static final String JAVAX_PERSISTENCE_JDBC_PASSWORD = "javax.persistence.jdbc.password";
    public static final String HIBERNATE_DIALECT = "hibernate.dialect";
    public static final String HIBERNATE_HBM2DDL_AUTO = "hibernate.hbm2ddl.auto";
    public static final String USER = ";USER=";
    public static final String PASSWORD = ";PASSWORD=";


    private static Properties getBaseProperties() {
        Properties properties = System.getProperties();

        properties.setProperty(JAVAX_PERSISTENCE_JDBC_DRIVER, "org.h2.Driver");

        properties.setProperty(HIBERNATE_DIALECT, "org.hibernate.dialect.H2Dialect");
        properties.setProperty(HIBERNATE_HBM2DDL_AUTO, "update");

        return properties;
    }

    protected static Properties getLocalProperties(final String fileName, final String user, final char[] password, final boolean readOnly) {
        Properties properties = getBaseProperties();

        String url = "jdbc:h2:" + jgnash.util.FileUtils.stripFileExtension(fileName);

        if (readOnly) {
            url += ";ACCESS_MODE_DATA=r";
        }

        if (user != null && user.length() > 0) {
            url += (USER + user);
        }

        if (password != null && password.length > 0) {
            url += (PASSWORD + Arrays.toString(password));
        }

        properties.setProperty(JAVAX_PERSISTENCE_JDBC_URL, url);
        properties.setProperty(JAVAX_PERSISTENCE_JDBC_USER, user);
        properties.setProperty(JAVAX_PERSISTENCE_JDBC_PASSWORD, new String(password));

        return properties;
    }

    protected static Properties getClientProperties(final String fileName, final String host, final int port, final String user, final char[]  password) {
        Properties properties = getBaseProperties();

        boolean useSSL = Boolean.parseBoolean(properties.getProperty("ssl"));

        properties.setProperty(JAVAX_PERSISTENCE_JDBC_USER, user);
        properties.setProperty(JAVAX_PERSISTENCE_JDBC_PASSWORD, new String(password));

        StringBuilder builder = new StringBuilder("jdbc:h2");

        if (useSSL) {
            builder.append(":ssl://");
        } else {
            builder.append(":tcp://");
        }

        builder.append(host).append(":").append(port).append("/");
        builder.append(fileName);

        builder.append(USER).append(user);
        builder.append(PASSWORD).append(password);

        properties.setProperty(JAVAX_PERSISTENCE_JDBC_URL, builder.toString());

        return properties;
    }
}
