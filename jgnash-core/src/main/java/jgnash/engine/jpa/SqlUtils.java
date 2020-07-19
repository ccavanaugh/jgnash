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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.DataStoreType;
import jgnash.engine.EngineFactory;
import jgnash.util.FileUtils;

/**
 * Utility class to perform low level SQL related stuff with the database.
 *
 * @author Craig Cavanaugh
 */
public class SqlUtils {

    private static final Logger logger = Logger.getLogger(SqlUtils.class.getName());

    /**
     * Maximum amount of time to wait for the lock file to release after closure.  Typical time should be about 2
     * seconds, but unit tests or large databases can sometimes take longer
     */
    private static final long MAX_LOCK_RELEASE_TIME = 30L * 1000L;

    private static final int TABLE_NAME = 3;

    private static final int COLUMN_NAME = 4;

    private static final String SHUTDOWN = "SHUTDOWN";

    private SqlUtils() {
    }

    public static boolean changePassword(final String fileName, final char[] password, final char[] newPassword) {
        boolean result = false;

        try {
            if (!FileUtils.isFileLocked(fileName)) {
                final DataStoreType dataStoreType = EngineFactory.getDataStoreByType(fileName);

                Objects.requireNonNull(dataStoreType);

                final Properties properties = JpaConfiguration.getLocalProperties(dataStoreType, fileName, password, false);
                final String url = properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL);

                try (final Connection connection = DriverManager.getConnection(url)) {

                    try (final PreparedStatement statement = connection.prepareStatement("SET PASSWORD ?")) {
                        statement.setString(1, new String(newPassword));
                        statement.execute();
                        result = true;
                    }
                } catch (final SQLException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        } catch (final IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return result;
    }

    /**
     * Opens the database in readonly mode and reads the version of the file format.
     *
     * @param fileName name of file to open
     * @param password connection password
     * @return file version. Zero is returned if not found
     */
    public static float getFileVersion(final String fileName, final char[] password) {
        float fileVersion = 0f;

        try {
            if (!FileUtils.isFileLocked(fileName)) {
                final DataStoreType dataStoreType = EngineFactory.getDataStoreByType(fileName);
                final Properties properties = JpaConfiguration.getLocalProperties(dataStoreType, fileName, password,
                        false);
                final String url = properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL);

                try (final Connection connection = DriverManager.getConnection(url)) {
                    try (final Statement statement = connection.createStatement()) {

                        boolean configTableExists = false;

                        // check for the existence of the table first
                        final DatabaseMetaData metaData = connection.getMetaData();

                        try (final ResultSet resultSet = metaData.getTables(null, null, "CONFIG", null)) {
                            while (resultSet.next()) {
                                if (resultSet.getString(TABLE_NAME).toUpperCase(Locale.ROOT).equals("CONFIG")) {
                                    configTableExists = true;
                                }
                            }
                        }

                        // check for the value if the table exists
                        if (configTableExists) {
                            try (final ResultSet resultSet = statement.executeQuery("SELECT FILEFORMAT FROM CONFIG")) {
                                resultSet.next();
                                fileVersion = Float.parseFloat(resultSet.getString("fileformat"));
                            }
                        }
                    }

                    // must issue a shutdown for correct file closure
                    try (final Statement statement = connection.createStatement()) {
                        statement.execute(SHUTDOWN);
                    }
                } catch (final SQLException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            } else {
                logger.severe("File was locked");
            }
        } catch (final IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return fileVersion;
    }

    /**
     * Diagnostic utility method to dump a list of table names and columns to the console.
     * Assumes the file is not password protected.
     *
     * @param fileName name of file to open
     * @return a {@code Set} of strings with the table names and columns, comma separated
     */
    public static Set<String> getTableAndColumnNames(final String fileName) {

        final Set<String> tableNames = new TreeSet<>();

        try {
            if (!FileUtils.isFileLocked(fileName)) {
                final DataStoreType dataStoreType = EngineFactory.getDataStoreByType(fileName);
                final Properties properties = JpaConfiguration.getLocalProperties(dataStoreType, fileName,
                        EngineFactory.EMPTY_PASSWORD, true);
                final String url = properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL);

                try (final Connection connection = DriverManager.getConnection(url)) {
                    final DatabaseMetaData metaData = connection.getMetaData();

                    try (final ResultSet resultSet = metaData.getColumns(null, null, "%", "%")) {
                        while (resultSet.next()) {
                            tableNames.add(resultSet.getString(TABLE_NAME).toUpperCase(Locale.ROOT) + ","
                                    + resultSet.getString(COLUMN_NAME).toUpperCase(Locale.ROOT));
                        }
                    }

                    // must issue a shutdown for correct file closure
                    try (final Statement statement = connection.createStatement()) {
                        statement.execute(SHUTDOWN);
                    }
                } catch (final SQLException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            } else {
                logger.severe("File was locked");
            }
        } catch (final IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return tableNames;
    }

    @SuppressWarnings("SameParameterValue")
    static void dropColumn(final String fileName, final char[] password, final String table, final String... columns) {
        try {
            if (!FileUtils.isFileLocked(fileName)) {
                final DataStoreType dataStoreType = EngineFactory.getDataStoreByType(fileName);
                final Properties properties = JpaConfiguration.getLocalProperties(dataStoreType, fileName, password,
                        false);
                final String url = properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL);

                try (final Connection connection = DriverManager.getConnection(url)) {

                    final DatabaseMetaData metaData = connection.getMetaData();

                    final ResultSet resultSet = metaData.getTables(null, null, table,
                            new String[] {"TABLE"});

                    while (resultSet.next()) {
                        final String tableName = resultSet.getString("TABLE_NAME");

                        if (tableName !=null && tableName.equalsIgnoreCase(table)) {
                            for (final String column : columns) {
                                try (final Statement statement = connection.createStatement()) {
                                    statement.execute("ALTER TABLE " + table + " DROP " + column);
                                }
                            }
                        }
                    }

                    // must issue a shutdown for correct file closure
                    try (final Statement statement = connection.createStatement()) {
                        statement.execute(SHUTDOWN);
                    }
                } catch (final SQLException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            } else {
                logger.severe("File was locked");
            }
        } catch (final IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Returns true if the url is valid, throws an exception otherwise.
     *
     * @param url url to validate
     * @return {@code true} if valid
     */
    static boolean isConnectionValid(final String url) {
        boolean result = false;

        try (final Connection ignored = DriverManager.getConnection(url)) {
            logger.fine("Connection is valid");
            result = true;
        } catch (final SQLException e) {
            if (e.toString().contains("Unknown database")) {
                logger.severe("Unknown database");
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }

        return result;
    }

    /**
     * Forces a database closed and waits for the lock file to disappear indicating the database server is closed.
     *
     * @param dataStoreType     DataStoreType to connect to
     * @param fileName          path of the file to close
     * @param lockFileExtension lock file extension
     * @param password          password for the database
     */
    static void waitForLockFileRelease(final DataStoreType dataStoreType, final String fileName,
                                       final String lockFileExtension, final char[] password) {

        final String lockFile = FileUtils.stripFileExtension(fileName) + lockFileExtension;
        logger.log(Level.INFO, "Searching for lock file: {0}", lockFile);

        // Don't try if the lock file does not exist
        if (Files.exists(Paths.get(lockFile))) {
            final Properties properties = JpaConfiguration.getLocalProperties(dataStoreType, fileName, password, false);
            final String url = properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL);

            // Send shutdown to close the database
            try (final Connection connection = DriverManager.getConnection(url, JpaConfiguration.DEFAULT_USER,
                    new String(password))) {
                // must issue a shutdown for correct file closure
                try (final Statement statement = connection.createStatement()) {
                    statement.execute(SHUTDOWN);
                }
            } catch (final SQLException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }

            logger.info("SQL SHUTDOWN was issued");

            if (!FileUtils.waitForFileRemoval(lockFile, MAX_LOCK_RELEASE_TIME)) {
                logger.warning("Exceeded the maximum wait time for the file lock release");
            }
        }
    }
}
