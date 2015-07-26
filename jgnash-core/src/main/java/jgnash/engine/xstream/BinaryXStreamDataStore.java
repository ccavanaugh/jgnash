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
package jgnash.engine.xstream;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import jgnash.engine.Config;
import jgnash.engine.DataStore;
import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.StoredObject;
import jgnash.engine.attachment.LocalAttachmentManager;
import jgnash.engine.concurrent.LocalLockManager;
import jgnash.util.NotNull;
import jgnash.util.ResourceUtils;

/**
 * XML specific code for data storage and creating an engine
 *
 * @author Craig Cavanaugh
 */
public class BinaryXStreamDataStore implements DataStore {

    private static final Logger logger = Logger.getLogger(BinaryXStreamDataStore.class.getName());

    public static final String FILE_EXT = "bxds";

    private BinaryContainer container;

    /**
     * Close the open
     * {@code Engine}
     *
     * @see jgnash.engine.DataStore#closeEngine()
     */
    @Override
    public void closeEngine() {
        container.commit(); // force a commit
        container.close();

        container = null;
    }

    /**
     * Create an engine instance that uses a local XML file
     *
     * @see jgnash.engine.DataStore#getLocalEngine(String, String, char[])
     */
    @Override
    public Engine getLocalEngine(final String fileName, final String engineName, final char[] password) {

        File file = new File(fileName);

        container = new BinaryContainer(file);

        if (file.exists()) {
            container.readBinary();
        }

        Engine engine = new Engine(new XStreamEngineDAO(container), new LocalLockManager(), new LocalAttachmentManager(), engineName);

        logger.info("Created local Binary container and engine");

        return engine;
    }

    /**
     * {@code XMLDataStore} will always return false
     *
     * @see jgnash.engine.DataStore#isRemote()
     */
    @Override
    public boolean isRemote() {
        return false;
    }

    /**
     * Returns the default file extension for this
     * {@code DataStore}
     *
     * @see jgnash.engine.DataStore#getFileExt()
     * @see BinaryXStreamDataStore#FILE_EXT
     */
    @Override
    @NotNull
    public final String getFileExt() {
        return FILE_EXT;
    }

    /**
     * Returns the full path to the file the DataStore is using.
     *
     * @see jgnash.engine.DataStore#getFileName()
     */
    @Override
    public final String getFileName() {
        return container.getFileName();
    }

    @Override
    public DataStoreType getType() {
        return DataStoreType.BINARY_XSTREAM;
    }

    /**
     * XMLDataStore will throw an exception if called
     *
     * @see jgnash.engine.DataStore#getClientEngine(String, int, char[], String)
     * @throws UnsupportedOperationException
     */
    @Override
    public Engine getClientEngine(final String host, final int port, final char[] password, final String engineName) {
        throw new UnsupportedOperationException("Client / Server operation not supported for this type.");
    }

    /**
     * Returns the string representation of this
     * {@code DataStore}.
     *
     * @return string representation of this
     * {@code DataStore}.
     */
    @Override
    public String toString() {
        return ResourceUtils.getString("DataStoreType.Bxds");
    }

    /**
     * @see jgnash.engine.DataStore#saveAs(java.io.File, java.util.Collection)
     */
    @Override
    public void saveAs(final File file, final Collection<StoredObject> objects) {
        BinaryContainer.writeBinary(objects, file);
    }

    /**
     * Opens the file in readonly mode and reads the version of the file format.
     *
     * @param file
     * {@code File} to open
     * @return file version
     */
    public static float getFileVersion(final File file) {

        float fileVersion = 0;

        if (file.exists()) {
            BinaryContainer container = new BinaryContainer(file);

            try {
                container.readBinary();

                List<Config> list = container.query(Config.class);

                if (list.size() == 1) {
                    fileVersion = list.get(0).getFileVersion();
                } else {
                    fileVersion = list.get(0).getFileVersion();
                    logger.severe("A duplicate config object was found");
                }
            } finally {
                container.close();
            }
        }

        return fileVersion;
    }
}
