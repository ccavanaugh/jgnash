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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;
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

    private static final Logger logger = Logger.getLogger(JpaConfiguration.class.getName());

    /**
     * Maximum amount of time to wait for the lock file to release after closure.  Typical time should be about 2 seconds,
     * but unit tests or large databases can sometimes take longer
     */
    private static final long MAX_LOCK_RELEASE_TIME = 30 * 1000;

    private static final int LOCK_WAIT_SLEEP = 750;

    private SqlUtils() {
    }

    public static boolean changeUserAndPassword(final String fileName, final char[] password, final char[] newPassword) {
        boolean result = false;

        try {
            if (!FileUtils.isFileLocked(fileName)) {

                DataStoreType dataStoreType = EngineFactory.getDataStoreByType(fileName);

                Properties properties = JpaConfiguration.getLocalProperties(dataStoreType, fileName, password, false);

                String url = properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL);

                try (Connection connection = DriverManager.getConnection(url)) {
                    Statement statement = connection.createStatement();

                    statement.execute(String.format("SET PASSWORD '%s'", new String(newPassword)));

                    result = true;

                    statement.close();
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
     * @param fileName <code>File</code> to open
     * @return file version
     */
    public static float getFileVersion(final String fileName, final char[] password) {
        float fileVersion = 0f;

        try {
            if (!FileUtils.isFileLocked(fileName)) {

                final DataStoreType dataStoreType = EngineFactory.getDataStoreByType(fileName);

                final Properties properties = JpaConfiguration.getLocalProperties(dataStoreType, fileName, password, true);

                final String url = properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL);

                try (Connection connection = DriverManager.getConnection(url)) {
                    Statement statement = connection.createStatement();

                    ResultSet resultSet = statement.executeQuery("SELECT FILEVERSION FROM CONFIG");
                    resultSet.next();

                    fileVersion = resultSet.getFloat("fileversion");

                    connection.prepareStatement("SHUTDOWN").execute(); // absolutely required for correct file closure

                    resultSet.close();
                    statement.close();
                } catch (final SQLException e) {
                    Logger.getLogger(JpaConfiguration.class.getName()).log(Level.SEVERE, e.getMessage(), e);
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
     * Forces a database closed and waits for the lock file to disappear indicating the database server is closed
     *
     * @param dataStoreType     DataStoreType to connect to
     * @param fileName          path of the file to close
     * @param lockFileExtension lock file extension
     * @param password          password for the database
     */
    public static void waitForLockFileRelease(final DataStoreType dataStoreType, final String fileName, final String lockFileExtension, final char[] password) {

        final String lockFile = FileUtils.stripFileExtension(fileName) + lockFileExtension;
        logger.info("Searching for lock file: " + lockFile);

        // Don't try if the lock file does not exist
        if (Files.exists(Paths.get(lockFile))) {

            Properties properties = JpaConfiguration.getLocalProperties(dataStoreType, fileName, password, false);
            String url = properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL);

            // Send shutdown to close the database
            try {
                final Connection connection = DriverManager.getConnection(url, JpaConfiguration.DEFAULT_USER, new String(password));

                connection.prepareStatement("SHUTDOWN").execute(); // absolutely required for correct file closure
                connection.close();
            } catch (final SQLException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }

            logger.info("SQL SHUTDOWN was issued");

            // It may take awhile for the lock to be released.  Wait for removal so any later attempts to open the file won't see the lock file and fail.
            final long then = new Date().getTime();

            while (Files.exists(Paths.get(lockFile))) {
                long now = new Date().getTime();

                if ((now - then) > MAX_LOCK_RELEASE_TIME) {
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
