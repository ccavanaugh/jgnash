/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

import jgnash.engine.DataStoreType;
import jgnash.util.FileUtils;

/**
 * Utility class to help with JPA configuration.
 *
 * @author Craig Cavanaugh
 */
class JpaConfiguration {

    private JpaConfiguration() {
        // utility class
    }

    static final String UNIT_NAME = "jgnash";
    static final String OLD_UNIT_NAME = "jgnash-old";
    static final String DEFAULT_USER = "JGNASH";

    static final String JAVAX_PERSISTENCE_JDBC_URL = "javax.persistence.jdbc.url";

    private static final String JAVAX_PERSISTENCE_JDBC_DRIVER = "javax.persistence.jdbc.driver";
    private static final String JAVAX_PERSISTENCE_JDBC_USER = "javax.persistence.jdbc.user";
    private static final String JAVAX_PERSISTENCE_JDBC_PASSWORD = "javax.persistence.jdbc.password";
    private static final String HIBERNATE_DIALECT = "hibernate.dialect";
    private static final String HIBERNATE_HBM2DDL_AUTO = "hibernate.hbm2ddl.auto";

    private static final String UNKNOWN_DATABASE_TYPE = "Unknown database type";

    private static Properties getBaseProperties(final DataStoreType database) {
        Properties properties = System.getProperties();

        properties.setProperty(HIBERNATE_HBM2DDL_AUTO, "update");

        switch (database) {
            case H2_DATABASE:
            case H2MV_DATABASE:
                properties.setProperty(JAVAX_PERSISTENCE_JDBC_DRIVER, "org.h2.Driver");
                properties.setProperty(HIBERNATE_DIALECT, "org.hibernate.dialect.H2Dialect");
                break;
            case HSQL_DATABASE:
                properties.setProperty(JAVAX_PERSISTENCE_JDBC_DRIVER, "org.hsqldb.jdbcDriver");
                properties.setProperty(HIBERNATE_DIALECT, "org.hibernate.dialect.HSQLDialect");
                break;
            default:
                throw new RuntimeException(UNKNOWN_DATABASE_TYPE);
        }

        return properties;
    }

    static Properties getLocalProperties(final DataStoreType dataStoreType, final String fileName, final char[] password,
                                         final boolean readOnly) {
        Objects.requireNonNull(password);
        Objects.requireNonNull(dataStoreType);

        final StringBuilder urlBuilder = new StringBuilder();

        switch (dataStoreType) {
            case H2_DATABASE:
            case H2MV_DATABASE:
                urlBuilder.append("jdbc:h2:nio:");

                urlBuilder.append(FileUtils.stripFileExtension(fileName));

                urlBuilder.append(";USER=").append(DEFAULT_USER);

                if (password.length > 0) {
                    urlBuilder.append(";PASSWORD=").append(password);
                }

                // use the old 1.3 page storage format instead of the MVStore based on file extension.  This allows
                // for correct handling of old files without forcing an upgrade
                if (FileUtils.getFileExtension(fileName).contains("h2.db")) {
                    urlBuilder.append(";MV_STORE=FALSE;MVCC=FALSE");
                } else {
                    urlBuilder.append(";COMPRESS=TRUE;FILE_LOCK=FILE");   // do not use FS locking for
                }

                if (readOnly) {
                    urlBuilder.append(";ACCESS_MODE_DATA=r");
                }

                urlBuilder.append(";TRACE_LEVEL_SYSTEM_OUT=1"); // make sure errors are logged to the console
                break;
            case HSQL_DATABASE:
                urlBuilder.append("jdbc:hsqldb:file:");
                urlBuilder.append(FileUtils.stripFileExtension(fileName));

                urlBuilder.append(";user=").append(DEFAULT_USER);

                if (password.length > 0) {
                    urlBuilder.append(";password=").append(password);
                }

                if (readOnly) {
                    urlBuilder.append(";readonly=true");
                }
                break;
            default:
                throw new RuntimeException(UNKNOWN_DATABASE_TYPE);
        }

        final Properties properties = getBaseProperties(dataStoreType);

        properties.setProperty(JAVAX_PERSISTENCE_JDBC_URL, urlBuilder.toString());
        properties.setProperty(JAVAX_PERSISTENCE_JDBC_USER, DEFAULT_USER);
        properties.setProperty(JAVAX_PERSISTENCE_JDBC_PASSWORD, new String(password));

        return properties;
    }

    /**
     * Generates and a JPA properties to connect to a remote database.
     *
     * @param dataStoreType DataStoreType type
     * @param fileName remote file to connect to, ignored for HSQL_DATABASE connections
     * @param host remote host
     * @param port remote port
     * @param password database password
     * @return   JPA properties
     */
    static Properties getClientProperties(final DataStoreType dataStoreType, final String fileName, final String host,
                                          final int port, final char[] password) {
        Objects.requireNonNull(password);
        Objects.requireNonNull(dataStoreType);

        final StringBuilder urlBuilder = new StringBuilder();

        final Properties properties = getBaseProperties(dataStoreType);

        switch (dataStoreType) {
            case H2_DATABASE:
            case H2MV_DATABASE:
                urlBuilder.append("jdbc:h2");

                /*boolean useSSL = Boolean.parseBoolean(properties.getProperty(EncryptionManager.ENCRYPTION_FLAG));
                if (useSSL) {
                    urlBuilder.append(":ssl://");
                } else {
                    urlBuilder.append(":tcp://");
                }*/

                urlBuilder.append(":tcp://");

                urlBuilder.append(host).append(":").append(port).append("/");
                urlBuilder.append(FileUtils.stripFileExtension(fileName));

                urlBuilder.append(";USER=").append(DEFAULT_USER);
                urlBuilder.append(";PASSWORD=").append(password);
                break;
            case HSQL_DATABASE:
                urlBuilder.append("jdbc:hsqldb:hsql://");
                urlBuilder.append(host).append(":").append(port).append("/jgnash"); // needs a public alias

                urlBuilder.append(";user=").append(DEFAULT_USER);

                if (password.length > 0) {
                    urlBuilder.append(";password=").append(password);
                }

                Logger.getLogger(JpaConfiguration.class.getName()).info(urlBuilder.toString());
                break;
            default:
                throw new RuntimeException(UNKNOWN_DATABASE_TYPE);
        }

        properties.setProperty(JAVAX_PERSISTENCE_JDBC_USER, DEFAULT_USER);
        properties.setProperty(JAVAX_PERSISTENCE_JDBC_PASSWORD, new String(password));
        properties.setProperty(JAVAX_PERSISTENCE_JDBC_URL, urlBuilder.toString());

        return properties;
    }

}
