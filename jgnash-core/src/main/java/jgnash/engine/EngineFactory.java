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
package jgnash.engine;

import jgnash.engine.jpa.SqlUtils;
import jgnash.engine.message.ChannelEvent;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.xstream.BinaryXStreamDataStore;
import jgnash.engine.xstream.XMLDataStore;
import jgnash.util.FileMagic;
import jgnash.util.FileMagic.FileType;
import jgnash.util.FileUtils;
import jgnash.util.Resource;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.filechooser.FileSystemView;

/**
 * Factory class for obtaining an engine instance
 * <p/>
 * The filename of the database or remote server must be explicitly set before
 * an Engine instance will be returned
 *
 * @author Craig Cavanaugh
 */
public class EngineFactory {

    private static final String LAST_DATABASE = "LastDatabase";

    private static final String LAST_HOST = "LastHost";

    private static final String LAST_PORT = "LastPort";

    private static final String USED_PASSWORD = "LastUsedPassword";

    private static final String LAST_REMOTE = "LastRemote";

    private static final String EXPORT_XML_ON_CLOSE = "ExportXMLOnClose";

    private static final String MAX_BACKUPS = "MaxBackups";

    private static final String REMOVE_BACKUPS = "RemoveBackups";

    private static final String OPEN_LAST = "OpenLast";

    /**
     * Default directory for jGnash data. To be located in the default user
     * directory
     */
    private static final String DEFAULT_DIR = "jGnash";

    private static final Logger logger = Logger.getLogger(EngineFactory.class.getName());

    public static final String DEFAULT = "default";

    private static final Map<String, Engine> engineMap = new HashMap<>();

    private static final Map<String, DataStore> dataStoreMap = new HashMap<>();

    private EngineFactory() {
    }

    public static boolean doesDatabaseExist(final String database, final DataStoreType type) {
        if (FileUtils.fileHasExtension(database)) {
            File file = new File(database);
            return file.canRead();
        }

        File file = new File(database + "." + type.getDataStore().getFileExt());
        return file.canRead();
    }

    public static boolean doesDatabaseExist(final String database) {
        File file = new File(database);
        return file.canRead();
    }

    public static boolean deleteDatabase(final String database) {
        return new File(database).delete();
    }

    /**
     * Returns the engine with the given name
     * @param name engine name to look for
     *
     * @return null if it does not exist
     */
    public static synchronized Engine getEngine(final String name) {
        return engineMap.get(name);
    }

    /**
     * Returns the DataStoreType for a given engine name
     * @param name engine name to look for
     *
     * @return the DataStoreType
     * @throws NullPointerException
     */
    public static synchronized DataStoreType getType(final String name) throws NullPointerException {
        DataStore dataStore = dataStoreMap.get(name);

        return dataStore.getType();
    }

    private static void exportCompressedXML(final String engineName) {
        Engine oldEngine = engineMap.get(engineName);
        DataStore oldDataStore = dataStoreMap.get(engineName);

        exportCompressedXML(oldDataStore.getFileName(), oldEngine.getStoredObjects());
    }

    public static void exportCompressedXML(final String fileName, final Collection<StoredObject> objects) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmm");

        DataStore xmlDataStore = new XMLDataStore();

        File xmlFile = new File(FileUtils.stripFileExtension(fileName) + "-" + dateFormat.format(new Date()) + "." + xmlDataStore.getFileExt());

        // push the intermediary file to the temporary directory
        xmlFile = new File(System.getProperty("java.io.tmpdir"), xmlFile.getName());

        File zipFile = new File(FileUtils.stripFileExtension(fileName) + "-" + dateFormat.format(new Date()) + ".zip");

        xmlDataStore.saveAs(xmlFile, objects);

        FileUtils.compressFile(xmlFile, zipFile);

        if (!xmlFile.delete()) {
            logger.log(Level.WARNING, "Was not able to delete the temporary file: {0}", xmlFile.getAbsolutePath());
        }
    }

    public static void removeOldCompressedXML(final String fileName) {

        File file = new File(fileName);

        String baseFile = FileUtils.stripFileExtension(file.getName());

        List<File> fileList = FileUtils.getDirectoryListing(file.getParentFile(), baseFile + "-*.zip");

        int maxFiles = maximumBackups();

        if (fileList.size() > maxFiles) {
            for (int i = 0; i < fileList.size() - maxFiles; i++) {
                if (!fileList.get(i).delete()) {
                    logger.log(Level.WARNING, "Unable to delete the file: {0}", fileList.get(i).getAbsolutePath());
                }
            }
        }
    }

    public static synchronized void closeEngine(final String engineName) {
        Engine oldEngine = engineMap.get(engineName);
        DataStore oldDataStore = dataStoreMap.get(engineName);

        if (oldEngine != null) {
            Message message = new Message(MessageChannel.SYSTEM, ChannelEvent.FILE_CLOSING, oldEngine);
            MessageBus.getInstance(engineName).fireEvent(message);

            if (exportXMLOnClose() && !oldDataStore.isRemote()) {
                exportCompressedXML(engineName);
            }

            if (removeOldBackups() && !oldDataStore.isRemote()) {
                removeOldCompressedXML(oldDataStore.getFileName());
            }

            oldEngine.shutdown();

            MessageBus.getInstance(engineName).setLocal();

            oldDataStore.closeEngine();

            engineMap.remove(engineName);
            dataStoreMap.remove(engineName);
        }
    }

    /**
     * Boots a local Engine for a preexisting file. The API determines the
     * correct file type and uses the correct DataStoreType for engine
     * initialization. If successful, a new
     * <code>Engine</code> instance will be returned.
     *
     * @param fileName   filename to load
     * @param engineName engine identifier
     * @return new
     *         <code>Engine</code> instance if successful, null otherwise
     * @see Engine
     */
    public static synchronized Engine bootLocalEngine(final String fileName, final String engineName, final char[] password) throws Exception {
        DataStoreType type = getDataStoreByType(new File(fileName));

        if (type != null) {
            return bootLocalEngine(fileName, engineName, password, type);
        }

        return null;
    }

    /**
     * Boots a local Engine for a file. If the file does not exist, it will be
     * created. Otherwise it will be loaded. If successful, a new
     * <code>Engine</code> instance will be returned.
     *
     * @param fileName   filename to load or create
     * @param engineName engine identifier
     * @param password   password for the file
     * @param type       <code>DataStoreType</code> type to use for storage
     * @return new <code>Engine</code> instance if successful
     * @see Engine
     * @see DataStoreType
     */
    public static synchronized Engine bootLocalEngine(final String fileName, final String engineName, final char[] password, final DataStoreType type) throws UnsupportedOperationException {

        if (!type.supportsLocal) {
            throw new UnsupportedOperationException("Local operation not supported for this type.");
        }

        MessageBus.getInstance(engineName).setLocal();

        DataStore dataStore = type.getDataStore();

        Engine engine = dataStore.getLocalEngine(fileName, engineName, password);

        if (engine != null) {
            logger.info(Resource.get().getString("Message.EngineStart"));
            engineMap.put(engineName, engine);
            dataStoreMap.put(engineName, dataStore);

            Message message = new Message(MessageChannel.SYSTEM, ChannelEvent.FILE_LOAD_SUCCESS, engine);
            MessageBus.getInstance(engineName).fireEvent(message);

            if (engineName.equals(EngineFactory.DEFAULT)) {
                Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

                pref.putBoolean(USED_PASSWORD, password.length > 0);
                pref.put(LAST_DATABASE, fileName);
                pref.putBoolean(LAST_REMOTE, false);
            }
        }
        return engine;
    }

    public static synchronized Engine bootClientEngine(final String host, final int port, final char[] password, final String engineName) throws Exception {

        if (engineMap.get(engineName) != null) {
            throw new RuntimeException("A stale engine was found in the map");
        }

        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        Engine engine = null;

        // start the client message bus
        if (MessageBus.getInstance(engineName).setRemote(host, port + 1, password)) {
            pref.putInt(LAST_PORT, port);
            pref.put(LAST_HOST, host);
            pref.putBoolean(LAST_REMOTE, true);

            MessageBus messageBus = MessageBus.getInstance(engineName);

            // after starting the remote message bus, it should receive the path on the server
            String remoteDataBasePath = messageBus.getRemoteDataBasePath();
            DataStoreType dataStoreType = messageBus.getRemoteDataStoreType();

            if (remoteDataBasePath == null || remoteDataBasePath.isEmpty() || dataStoreType == null) {
                throw new Exception("Invalid connection wih the message bus");
            }

            logger.log(Level.INFO, "Remote path was {0}", remoteDataBasePath);
            logger.log(Level.INFO, "Remote data store was {0}", dataStoreType.name());
            logger.log(Level.INFO, "Engine name was {0}", engineName);

            DataStore dataStore = dataStoreType.getDataStore();

            // connect to the remote server
            engine = dataStore.getClientEngine(host, port, password, remoteDataBasePath);

            if (engine != null) {
                logger.info(Resource.get().getString("Message.EngineStart"));

                engineMap.put(engineName, engine);
                dataStoreMap.put(engineName, dataStore);

                // remember if the user used a password for the last session
                pref.putBoolean(USED_PASSWORD, password.length > 0);

                Message message = new Message(MessageChannel.SYSTEM, ChannelEvent.FILE_LOAD_SUCCESS, engine);
                MessageBus.getInstance(engineName).fireEvent(message);
            }
        }

        return engine;
    }

    private static DataStoreType getDataStoreByType(final File file) {
        FileType type = FileMagic.magic(file);

        if (type == FileType.jGnash2XML) {
            return DataStoreType.XML;
        } else if (type == FileType.BinaryXStream) {
            return DataStoreType.BINARY_XSTREAM;
        } else if (type == FileType.h2) {
            return DataStoreType.H2_DATABASE;
        } else if (type == FileType.hsql) {
            return DataStoreType.HSQL_DATABASE;
        }

        return null;
    }

    public static DataStoreType getDataStoreByType(final String fileName) {
        return getDataStoreByType(new File(fileName));
    }

    public static float getFileVersion(final File file, final char[] password) {
        float version = 0;

        FileType type = FileMagic.magic(file);

        if (type == FileType.jGnash2XML) {
            version = XMLDataStore.getFileVersion(file);
        } else if (type == FileType.BinaryXStream) {
            version = BinaryXStreamDataStore.getFileVersion(file);
        } else if (type == FileType.h2 || type == FileType.hsql) {
            try {
                version = SqlUtils.getFileVersion(file.getAbsolutePath(), password);
            } catch (final Exception e) {
                version = 0;
            }
        }

        return version;
    }

    /**
     * Returns the default path to the database without a file extension.
     *
     * @return Default path to the database
     */
    public static synchronized String getDefaultDatabase() {
        String base = FileSystemView.getFileSystemView().getDefaultDirectory().getAbsolutePath();
        String userName = System.getProperty("user.name");
        String fileSep = System.getProperty("file.separator");

        return base + fileSep + DEFAULT_DIR + fileSep + userName;
    }

    /**
     * Returns the last open database. If a database has not been opened, then
     * the default database will be returned.
     *
     * @return Last open or default database
     */
    public static synchronized String getLastDatabase() {
        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        return pref.get(LAST_DATABASE, getDefaultDatabase());
    }

    public static synchronized String getActiveDatabase() {
        if (getLastRemote()) {
            return "@" + getLastHost();
        }

        return getLastDatabase();
    }

    /**
     * Returns the host of the last remote database connection. If a remote
     * database has not been opened, then the default host will be returned.
     *
     * @return Last remote database host or default
     */
    public static synchronized String getLastHost() {
        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        return pref.get(LAST_HOST, "localhost");
    }

    /**
     * Returns the port of the last remote database connection. If a remote
     * database has not been opened, then the default port will be returned.
     *
     * @return Last remote database port or default
     */
    public static synchronized int getLastPort() {
        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        return pref.getInt(LAST_PORT, 5300);
    }

    /**
     * Returns true if the last connection was made to a remote host.
     *
     * @return true if the last connection was made to a remote host
     */
    public static synchronized boolean getLastRemote() {
        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        return pref.getBoolean(LAST_REMOTE, false);
    }

    public static synchronized boolean usedPassword() {
        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        return pref.getBoolean(USED_PASSWORD, false);
    }

    public static synchronized void setExportXMLOnClose(final boolean export) {
        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        pref.putBoolean(EXPORT_XML_ON_CLOSE, export);
    }

    public static synchronized void setOpenLastOnStartup(final boolean last) {
        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        pref.putBoolean(OPEN_LAST, last);
    }

    public static synchronized boolean openLastOnStartup() {
        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        return pref.getBoolean(OPEN_LAST, true);
    }

    public static synchronized boolean exportXMLOnClose() {
        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        return pref.getBoolean(EXPORT_XML_ON_CLOSE, true);
    }

    public static synchronized int maximumBackups() {
        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        return pref.getInt(MAX_BACKUPS, 10);
    }

    public static synchronized void setMaximumBackups(final int max) {
        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        pref.putInt(MAX_BACKUPS, max);
    }

    public static synchronized boolean removeOldBackups() {
        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        return pref.getBoolean(REMOVE_BACKUPS, true);
    }

    public static synchronized void setRemoveOldBackups(final boolean backup) {
        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        pref.putBoolean(REMOVE_BACKUPS, backup);
    }
}
