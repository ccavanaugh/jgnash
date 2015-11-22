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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.DataStoreType;
import jgnash.engine.EngineFactory;
import jgnash.util.FileUtils;

/**
 * Utility class to perform low level SQL related stuff with the database
 *
 * @author Craig Cavanaugh
 */
public class SqlUtils {

    private static final Logger logger = Logger.getLogger(SqlUtils.class.getName());

    /**
     * Maximum amount of time to wait for the lock file to release after closure.  Typical time should be about 2 seconds,
     * but unit tests or large databases can sometimes take longer
     */
    private static final long MAX_LOCK_RELEASE_TIME = 30 * 1000;

    private static final int LOCK_WAIT_SLEEP = 750;

    private static final int TABLE_NAME = 3;

    private static final int COLUMN_NAME = 4;

    private SqlUtils() {
    }

    public static boolean changeUserAndPassword(final String fileName, final char[] password, final char[] newPassword) {
        boolean result = false;

        try {
            if (!FileUtils.isFileLocked(fileName)) {
                DataStoreType dataStoreType = EngineFactory.getDataStoreByType(fileName);
                Properties properties = JpaConfiguration.getLocalProperties(dataStoreType, fileName, password, false);
                String url = properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL);

                try (final Connection connection = DriverManager.getConnection(url)) {

                    try (final PreparedStatement statement = connection.prepareStatement("SET PASSWORD '?'")) {
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
     * @return file version
     */
    public static float getFileVersion(final String fileName, final char[] password) {
        float fileVersion = 0f;

        try {
            if (!FileUtils.isFileLocked(fileName)) {
                final DataStoreType dataStoreType = EngineFactory.getDataStoreByType(fileName);
                final Properties properties = JpaConfiguration.getLocalProperties(dataStoreType, fileName, password, false);
                final String url = properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL);

                try (final Connection connection = DriverManager.getConnection(url)) {
                    try (final Statement statement = connection.createStatement()) {
                        try (final ResultSet resultSet = statement.executeQuery("SELECT FILEVERSION FROM CONFIG")) {
                            resultSet.next();
                            fileVersion = resultSet.getFloat("fileversion");
                        }
                    }
                    // must issue a shutdown for correct file closure
                    try (final PreparedStatement statement =  connection.prepareStatement("SHUTDOWN")) {
                        statement.execute();
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

    public static boolean checkAndFixHibernate_HHH_9389(final String fileName, final char[] password) {
        boolean result = true;  // return false only if an error occurs

        try {
            if (!FileUtils.isFileLocked(fileName)) {
                final DataStoreType dataStoreType = EngineFactory.getDataStoreByType(fileName);
                final Properties properties = JpaConfiguration.getLocalProperties(dataStoreType, fileName, password, false);
                final String url = properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL);

                try (final Connection connection = DriverManager.getConnection(url)) {
                    final DatabaseMetaData metaData = connection.getMetaData();                   
                    
                    try (final ResultSet resultSet = metaData.getColumns(null, null, "%", "%")) {
                    	while (resultSet.next()) {
                            // table name is TRANSACT_TRANSACTIONENTRY
                            // need to rename the column TRANSACT_UUID to TRANSACTION_UUID
                            if (resultSet.getString(COLUMN_NAME).equals("TRANSACT_UUID") && resultSet.getString(TABLE_NAME).equals("TRANSACT_TRANSACTIONENTRY")) {
                                try (final PreparedStatement statement = connection.prepareStatement("ALTER TABLE TRANSACT_TRANSACTIONENTRY ALTER COLUMN TRANSACT_UUID RENAME TO TRANSACTION_UUID")) {
                                    statement.execute();
                                    logger.info("Correcting column name for Hibernate HHH-9389");
                                }
                            }
                        }
                    	
                    }
                
                    // must issue a shutdown for correct file closure
                    try (final PreparedStatement statement =  connection.prepareStatement("SHUTDOWN")) {
                        statement.execute();
                    }
                } catch (final SQLException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                    result = false;
                }
            } else {
                logger.severe("File was locked");
                result = false;
            }
        } catch (final IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            result = false;
        }

        return result;
    }

    /**
     * Utility function to dump a list of table names and columns to the console
     *
     * @param fileName name of file to open
     * @param password connection password
     * @return a {@code Set} of strings with the table names and columns, comma separated
     */
    public static Set<String> getTableAndColumnNames(final String fileName, final char[] password) {

        final Set<String> tableNames = new TreeSet<>();

        try {
            if (!FileUtils.isFileLocked(fileName)) {
                final DataStoreType dataStoreType = EngineFactory.getDataStoreByType(fileName);
                final Properties properties = JpaConfiguration.getLocalProperties(dataStoreType, fileName, password, false);
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
                    try (final PreparedStatement statement =  connection.prepareStatement("SHUTDOWN")) {
                        statement.execute();
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

    /**
     * Forces a database closed and waits for the lock file to disappear indicating the database server is closed
     *
     * @param dataStoreType     DataStoreType to connect to
     * @param fileName          path of the file to close
     * @param lockFileExtension lock file extension
     * @param password          password for the database
     */
    public static void waitForLockFileRelease(final DataStoreType dataStoreType, final String fileName, final String lockFileExtension, final char[] password) {

        final String lockFile = FileUtils.stripFileExtension(fileName) + lockFileExtension;
        logger.log(Level.INFO, "Searching for lock file: {0}", lockFile);

        // Don't try if the lock file does not exist
        if (Files.exists(Paths.get(lockFile))) {
            final Properties properties = JpaConfiguration.getLocalProperties(dataStoreType, fileName, password, false);
            final String url = properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL);

            // Send shutdown to close the database
            try (final Connection connection = DriverManager.getConnection(url, JpaConfiguration.DEFAULT_USER, new String(password))) {
                // must issue a shutdown for correct file closure
                try (final PreparedStatement statement =  connection.prepareStatement("SHUTDOWN")) {
                    statement.execute();
                }
            } catch (final SQLException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }

            logger.info("SQL SHUTDOWN was issued");

            // It may take awhile for the lock to be released.  Wait for removal so any later attempts to open the
            // file won't see the lock file and fail.
            final LocalDateTime start = LocalDateTime.now();

            while (Files.exists(Paths.get(lockFile))) {

                if (Duration.between(start, LocalDateTime.now()).toMillis() > MAX_LOCK_RELEASE_TIME){
                    logger.warning("Exceeded the maximum wait time for the file lock release");
                    break;
                }

                try {
                    Thread.sleep(LOCK_WAIT_SLEEP);
                } catch (final InterruptedException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
    }
}
