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
import java.util.logging.Level;

import jgnash.engine.DataStoreType;
import jgnash.util.FileUtils;
import jgnash.util.NotNull;

/**
 * JPA specific code for HSQLDB data storage and creating an engine.
 *
 * @author Craig Cavanaugh
 */
public class JpaHsqlDataStore extends AbstractJpaDataStore {

    public static final String FILE_EXT = "script";

    public static final String LOCK_EXT = ".lck";

    @NotNull
    @Override
    public String getFileExt() {
        return FILE_EXT;
    }

    @Override
    public DataStoreType getType() {
        return DataStoreType.HSQL_DATABASE;
    }

    @Override
    public void deleteDatabase(final File file) {
        deleteDatabase(file.getAbsolutePath());
    }

    @Override
    public String getLockFileExtension() {
        return LOCK_EXT;
    }

    /**
     * Deletes a Hsqldb database and associated files.
     *
     * @param fileName one of the database files
     */
    private static void deleteDatabase(final String fileName) {
        final String[] extensions = new String[]{".log", ".properties", ".script", ".data", ".backup", ".tmp",
                ".lobs", ".lck"};

        final String base = FileUtils.stripFileExtension(fileName);

        for (String extension : extensions) {
            try {
                Files.deleteIfExists(Paths.get(base + extension));
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }
}
