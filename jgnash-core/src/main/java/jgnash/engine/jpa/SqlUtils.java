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

import jgnash.engine.DataStoreType;
import jgnash.engine.EngineFactory;
import jgnash.util.FileUtils;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to perform low level SQL related stuff with the database
 *
 * @author Craig Cavanaugh
 */
public class SqlUtils {

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
                } catch (SQLException e) {
                    Logger.getLogger(JpaConfiguration.class.getName()).log(Level.SEVERE, e.getMessage(), e);
                }
            }
        } catch (FileNotFoundException e) {
            Logger.getLogger(JpaConfiguration.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }

        return result;
    }
}
