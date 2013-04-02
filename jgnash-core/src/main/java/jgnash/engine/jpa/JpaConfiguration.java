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

import jgnash.util.FileUtils;

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


        StringBuilder urlBuilder = new StringBuilder("jdbc:h2:");

        urlBuilder.append(FileUtils.stripFileExtension(fileName));

        if (user != null && user.length() > 0) {
            urlBuilder.append(USER).append(user);
        }

        if (password != null && password.length > 0) {
            urlBuilder.append(PASSWORD).append(password);
        }

        if (readOnly) {
            urlBuilder.append(";ACCESS_MODE_DATA=r");
        }

        properties.setProperty(JAVAX_PERSISTENCE_JDBC_URL, urlBuilder.toString());
        properties.setProperty(JAVAX_PERSISTENCE_JDBC_USER, user);
        properties.setProperty(JAVAX_PERSISTENCE_JDBC_PASSWORD, new String(password));

        return properties;
    }

    protected static Properties getClientProperties(final String fileName, final String host, final int port, final String user, final char[]  password) {
        Properties properties = getBaseProperties();

        boolean useSSL = Boolean.parseBoolean(properties.getProperty("ssl"));

        properties.setProperty(JAVAX_PERSISTENCE_JDBC_USER, user);
        properties.setProperty(JAVAX_PERSISTENCE_JDBC_PASSWORD, new String(password));

        StringBuilder urlBuilder = new StringBuilder("jdbc:h2");

        if (useSSL) {
            urlBuilder.append(":ssl://");
        } else {
            urlBuilder.append(":tcp://");
        }

        urlBuilder.append(host).append(":").append(port).append("/");
        urlBuilder.append(fileName);

        urlBuilder.append(USER).append(user);
        urlBuilder.append(PASSWORD).append(password);

        properties.setProperty(JAVAX_PERSISTENCE_JDBC_URL, urlBuilder.toString());

        return properties;
    }
}
