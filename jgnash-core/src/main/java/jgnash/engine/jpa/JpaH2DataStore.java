/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

import jgnash.engine.DataStoreType;
import jgnash.util.FileUtils;
import jgnash.util.NotNull;

/**
 * JPA specific code for data storage and creating an engine
 *
 * @author Craig Cavanaugh
 */
public class JpaH2DataStore extends AbstractJpaDataStore {

    public static final String FILE_EXT = "h2.db";

    public static final String LOCK_EXT = ".lock.db";

    /**
     * Creates an empty database with the assumed default user name
     *
     * @param fileName file name to use
     * @return true if successful
     */
    @Override
    public boolean initEmptyDatabase(final String fileName) {
        // H2 starts cleanly without an initial file, don't call the super

        boolean result = false;

        final Properties properties = JpaConfiguration.getLocalProperties(getType(), fileName, new char[]{}, false);
        final String url = properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL);

        // Increase the maximum size of the log file.  Need for file save As
        try (final Connection connection = DriverManager.getConnection(url, JpaConfiguration.DEFAULT_USER, "")) {
            try (final PreparedStatement statement = connection.prepareStatement("MAX_LOG_SIZE 128")) {
                statement.execute();
                connection.commit();
            }

            // absolutely required for a correct shutdown
            try (final PreparedStatement statement = connection.prepareStatement("SHUTDOWN")) {
                statement.execute();
            }

            result = true;
        } catch (final SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        waitForLockFileRelease(fileName, new char[]{});

        logger.log(Level.INFO, "Initialized an empty database for {0}", FileUtils.stripFileExtension(fileName));

        return result;
    }

    @NotNull
    @Override
    public String getFileExt() {
        return FILE_EXT;
    }

    @Override
    public DataStoreType getType() {
        return DataStoreType.H2_DATABASE;
    }

    @Override
    public void deleteDatabase(final File file) {
        deleteDatabase(file.getAbsolutePath());
    }

    @Override
    public String getLockFileExtension() {
        return LOCK_EXT;
    }

    private static void deleteDatabase(final String fileName) {
        final String[] extensions = new String[]{".h2.db", LOCK_EXT};

        final String base = FileUtils.stripFileExtension(fileName);

        for (String extension : extensions) {
            try {
                logger.log(Level.INFO, "Deleting {0}{1}", new Object[]{base, extension});
                Files.deleteIfExists(Paths.get(base + extension));
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }
}
