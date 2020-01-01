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
package jgnash.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.function.DoubleConsumer;

import jgnash.util.NotNull;

/**
 * Interface for data storage backends.
 *
 * @author Craig Cavanaugh
 */
public interface DataStore {

    /**
     * Close the engine instance if open.
     */
    void closeEngine();

    /**
     * Create an engine instance connected to a remote server.
     * 
     * @param host host name or IP address
     * @param port connection port
     * @param password user password
     * @param engineName unique name to give the engine instance
     * @return Engine instance if a successful connection is made
     */
    Engine getClientEngine(final String host, final int port, final char[] password, final String engineName);

    /**
     * Create an engine instance that uses a file.
     * 
     * @param fileName full path to the file
     * @param engineName unique name to give the engine instance
     * @param password user password
     * @return Engine instance.  A new file will be created if it does not exist
     */
    Engine getLocalEngine(final String fileName, final String engineName, final char[] password);

    /**
     * Returns the default file extension for this DataStore.
     * 
     * @return file extension
     */
    @NotNull
    String getFileExt();

    /**
     * Returns the full path to the file the DataStore is using.
     * 
     * @return  full path to the file, null if this is a remotely connected DataStore
     */
    String getFileName();

    /**
     * Returns this DataStores type.
     *
     * @return type of data store
     */
    DataStoreType getType();

    /**
     * Local / Remote connection indicator.
     * 
     * @return false if connected to a remote server
     */
    boolean isLocal();

    /**
     * Saves a Collection of StoredObjects to a file other than what is currently open.
     * <p> 
     * The currently open file will not be closed.
     *  @param path full path to the file to save the database to
     * @param objects Collection of StoredObjects to save
     * @param percentComplete callback to report the percent complete
     */
    void saveAs(Path path, Collection<StoredObject> objects, DoubleConsumer percentComplete);

    /**
     * Renames a datastore.
     *
     * @param fileName name of the datastore to rename
     * @param newFileName the new filename
     * @throws java.io.IOException if an I/O error occurs
     */
    default void rename(final String fileName, final String newFileName) throws IOException {
        final Path path = Paths.get(fileName);

        if (Files.exists(path)) {
            Files.move(path, Paths.get(newFileName));
        }
    }
}
