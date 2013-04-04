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
package jgnash.engine.xstream;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import jgnash.engine.Config;
import jgnash.engine.DataStore;
import jgnash.engine.Engine;
import jgnash.engine.StoredObject;
import jgnash.util.Resource;

/**
 * XML specific code for data storage and creating an engine
 *
 * @author Craig Cavanaugh
 */
public class XMLDataStore implements DataStore {

    private static final Logger logger = Logger.getLogger(XMLDataStore.class.getName());

    private static final String FILE_EXT = "xml";

    private XMLContainer container;

    /**
     * Close the open
     * <code>Engine</code>
     *
     * @see DataStore#closeEngine()
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
     * @see DataStore#getLocalEngine(java.lang.String, java.lang.String, char[])
     */
    @Override
    public Engine getLocalEngine(final String fileName, final String engineName, final char[] password) {

        File file = new File(fileName);

        container = new XMLContainer(file);

        if (file.exists()) {
            container.readXML();
        }

        Engine engine = new Engine(new XStreamEngineDAO(container), engineName);

        logger.info("Created local XML container and engine");

        return engine;
    }

    /**
     * <code>XMLDataStore</code> will always return false
     *
     * @see DataStore#isRemote()
     */
    @Override
    public boolean isRemote() {
        return false;
    }

    /**
     * Returns the default file extension for this
     * <code>DataStore</code>
     *
     * @see DataStore#getFileExt()
     * @see XMLDataStore#FILE_EXT
     */
    @Override
    public final String getFileExt() {
        return FILE_EXT;
    }

    /**
     * Returns the full path to the file the DataStore is using.
     *
     * @see DataStore#getFileName()
     */
    @Override
    public final String getFileName() {
        return container.getFileName();
    }

    /**
     * XMLDataStore will throw an exception if called
     *
     * @see DataStore#getClientEngine(java.lang.String, int, char[], java.lang.String)
     * @throws UnsupportedOperationException
     */
    @Override
    public Engine getClientEngine(final String host, final int port, final char[] password, final String engineName) {
        throw new UnsupportedOperationException("Client / Server operation not supported for this type.");
    }

    /**
     * Returns the string representation of this
     * <code>DataStore</code>.
     *
     * @return string representation of this
     * <code>DataStore</code>.
     */
    @Override
    public String toString() {
        return Resource.get().getString("DataStoreType.XML");
    }

    /**
     * @see DataStore#saveAs(java.io.File, java.util.Collection)
     */
    @Override
    public void saveAs(final File file, final Collection<StoredObject> objects) {
        XMLContainer.writeXML(objects, file);
    }

    /**
     * Opens the file in readonly mode and reads the version of the file format.
     *
     * @param file
     * <code>File</code> to open
     * @return file version
     */
    public static float getFileVersion(final File file) {

        float fileVersion = 0;

        if (file.exists()) {
            XMLContainer container = new XMLContainer(file);

            try {
                container.readXML();

                List<Config> list = container.query(Config.class);

                if (list.size() == 1) {
                    fileVersion = list.get(0).getFileVersion();
                } else {
                    Logger.getLogger(XMLDataStore.class.getName()).severe("Invalid file");
                }
            } finally {
                container.close();
            }
        }

        return fileVersion;
    }
}
