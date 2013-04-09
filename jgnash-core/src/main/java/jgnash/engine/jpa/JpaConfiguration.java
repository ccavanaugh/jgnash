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
    public static final String DEFAULT_USER = "JGNASH";

    /**
     * The preferred file locking method is to use the OS
     */
    public static final String FILE_LOCK_FS = ";FILE_LOCK=FS";

    private static Properties getBaseProperties(final Database database) {
        Properties properties = System.getProperties();

        properties.setProperty(HIBERNATE_HBM2DDL_AUTO, "update");

        switch (database) {
            case H2:
                properties.setProperty(JAVAX_PERSISTENCE_JDBC_DRIVER, "org.h2.Driver");
                properties.setProperty(HIBERNATE_DIALECT, "org.hibernate.dialect.H2Dialect");
                break;
            case HSQLDB:
                properties.setProperty(JAVAX_PERSISTENCE_JDBC_DRIVER, "org.hsqldb.jdbcDriver");
                properties.setProperty(HIBERNATE_DIALECT, "org.hibernate.dialect.HSQLDialect");
        }

        return properties;
    }

    public static Properties getLocalProperties(final Database database, final String fileName, final char[] password, final boolean readOnly) {
        StringBuilder urlBuilder = new StringBuilder();

        switch (database) {
            case H2:
                urlBuilder.append("jdbc:h2:");

                urlBuilder.append(FileUtils.stripFileExtension(fileName));

                urlBuilder.append(";USER=").append(DEFAULT_USER);

                if (password != null && password.length > 0) {
                    urlBuilder.append(";PASSWORD=").append(password);
                }

                urlBuilder.append(FILE_LOCK_FS);

                if (readOnly) {
                    urlBuilder.append(";ACCESS_MODE_DATA=r");
                }
                break;
            case HSQLDB:
                urlBuilder.append("jdbc:hsqldb:file:");
                urlBuilder.append(FileUtils.stripFileExtension(fileName));

                urlBuilder.append(";user=").append(DEFAULT_USER);
                if (password != null && password.length > 0) {
                    urlBuilder.append(";password=").append(password);
                }

                urlBuilder.append(";create=true");

                if (readOnly) {
                    urlBuilder.append(";readonly=true");
                }
        }

        Properties properties = getBaseProperties(database);

        properties.setProperty(JAVAX_PERSISTENCE_JDBC_URL, urlBuilder.toString());
        properties.setProperty(JAVAX_PERSISTENCE_JDBC_USER, DEFAULT_USER);
        properties.setProperty(JAVAX_PERSISTENCE_JDBC_PASSWORD, new String(password));


        return properties;
    }

    protected static Properties getClientProperties(final Database database, final String fileName, final String host, final int port, final char[] password) {

        StringBuilder urlBuilder = new StringBuilder();

        Properties properties = getBaseProperties(database);

        boolean useSSL = Boolean.parseBoolean(properties.getProperty("ssl"));

        switch (database) {
            case H2:
                urlBuilder.append("jdbc:h2");

                if (useSSL) {
                    urlBuilder.append(":ssl://");
                } else {
                    urlBuilder.append(":tcp://");
                }

                urlBuilder.append(host).append(":").append(port).append("/");
                urlBuilder.append(fileName);

                urlBuilder.append(";USER=").append(DEFAULT_USER);
                urlBuilder.append(";PASSWORD=").append(password);

                //urlBuilder.append(";DB_CLOSE_DELAY=20");
                break;
            case HSQLDB:
                urlBuilder.append("jdbc:hsqldb:hsql://");
                urlBuilder.append(host).append(":").append(port).append("/jgnash"); // needs a public alias

                urlBuilder.append(";user=").append(DEFAULT_USER);
                if (password != null && password.length > 0) {
                    urlBuilder.append(";password=").append(password);
                }
        }


        properties.setProperty(JAVAX_PERSISTENCE_JDBC_USER, DEFAULT_USER);
        properties.setProperty(JAVAX_PERSISTENCE_JDBC_PASSWORD, new String(password));

        properties.setProperty(JAVAX_PERSISTENCE_JDBC_URL, urlBuilder.toString());

        return properties;
    }
}
