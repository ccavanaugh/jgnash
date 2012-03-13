/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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

import java.io.File;
import java.util.Collection;

/**
 * Interface for data storage backends
 *
 * @author Craig Cavanaugh
 * @version $Id: DataStore.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public interface DataStore {

    /**
     * Close the engine instance if open
     */
    void closeEngine();

    /**
     * Create an engine instance connected to a remote server
     * 
     * @param host host name or IP address
     * @param port connection port
     * @param user user name
     * @param password user password
     * @param engineName unique name to give the engine instance
     * @return Engine instance if a successful connection is made
     */
    Engine getClientEngine(final String host, final int port, final String user, final String password, final String engineName);

    /**
     * Create an engine instance that uses a file
     * 
     * @param fileName full path to the file
     * @param engineName unique name to give the engine instance
     * @return Engine instance.  A new file will be created if it does not exist
     */
    Engine getLocalEngine(final String fileName, final String engineName);

    /**
     * Returns the default file extension for this DataStore
     * 
     * @return file extension
     */
    String getFileExt();

    /**
     * Returns the full path to the file the DataStore is using.
     * 
     * @return  full path to the file, null if this is a remotely connected DataStore
     */
    String getFileName();

    /**
     * Remote connection indicator
     * 
     * @return true if connected to a remote server
     */
    boolean isRemote();

    /**
     * Saves a Collection of StoredObejcts to a file other than what is currently open.
     * <p> 
     * The currently open file will not be closed
     * 
     * @param file full path to the file to save the database to
     * @param objects Collection of StoredObjects to save
     */
    void saveAs(File file, Collection<StoredObject> objects);
}
