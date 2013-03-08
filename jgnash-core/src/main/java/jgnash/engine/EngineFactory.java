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

import jgnash.engine.db4o.Db4oDataStore;
import jgnash.engine.xstream.BinaryXStreamDataStore;
import jgnash.engine.xstream.XMLDataStore;
import jgnash.message.ChannelEvent;
import jgnash.message.Message;
import jgnash.message.MessageBus;
import jgnash.message.MessageChannel;
import jgnash.util.FileMagic;
import jgnash.util.FileMagic.FileType;
import jgnash.util.FileUtils;
import jgnash.util.Resource;

/**
 * Factory class for obtaining an engine instance
 *
 * The filename of the database or remote server must be explicitly set before
 * an Engine instance will be returned
 *
 * @author Craig Cavanaugh
 */
public class EngineFactory {

    private static final String LAST_DATABASE = "LastDatabase";

    private static final String LAST_HOST = "LastHost";

    private static final String LAST_USER = "LastUser";

    private static final String LAST_PORT = "LastPort";

    private static final String LAST_PASSWORD = "LastPassword";

    private static final String LAST_USEDPASSWORD = "LastUsedPassword";

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

    public static synchronized Engine getEngine(final String name) {
        return engineMap.get(name);
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
     * Provides access to the engines <code>DataStore</code>.  This is intended for internal use only
     *
     * @param engineName name of the engine
     * @return DataStore for the named engine.
     */
    public static synchronized DataStore getDataStore(final String engineName) {
        return dataStoreMap.get(engineName);
    }

    /**
     * Boots a local Engine for a preexisting file. The API determines the
     * correct file type and uses the correct DataStoreType for engine
     * initialization. If successful, a new
     * <code>Engine</code> instance will be returned.
     *
     * @param fileName filename to load
     * @param engineName engine identifier
     * @return new
     * <code>Engine</code> instance if successful, null otherwise
     * @see Engine
     */
    public static synchronized Engine bootLocalEngine(final String fileName, final String engineName) {
        DataStoreType type = getDataStoreByType(new File(fileName));

        if (type != null) {
            return bootLocalEngine(fileName, engineName, type);
        }

        return null;
    }

    /**
     * Boots a local Engine for a file. If the file does not exist, it will be
     * created. Otherwise it will be loaded. If successful, a new
     * <code>Engine</code> instance will be returned.
     *
     * @param fileName filename to load or create
     * @param engineName engine identifier
     * @param type
     * <code>DataStoreType</code> type to use for storage
     * @return new
     * <code>Engine</code> instance if successful
     * @see Engine
     * @see DataStoreType
     */
    public static synchronized Engine bootLocalEngine(final String fileName, final String engineName, final DataStoreType type) {

        if (!type.supportsLocal) {
            throw new UnsupportedOperationException("Local operation not supported for this type.");
        }

        MessageBus.getInstance(engineName).setLocal();

        DataStore dataStore = type.getDataStore();

        Engine engine = dataStore.getLocalEngine(fileName, engineName);

        if (engine != null) {
            logger.info(Resource.get().getString("Message.EngineStart"));
            engineMap.put(engineName, engine);
            dataStoreMap.put(engineName, dataStore);

            Message message = new Message(MessageChannel.SYSTEM, ChannelEvent.FILE_LOAD_SUCCESS, engine);
            MessageBus.getInstance(engineName).fireEvent(message);

            if (engineName.equals(EngineFactory.DEFAULT)) {
                Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

                pref.put(LAST_DATABASE, fileName);
                pref.putBoolean(LAST_REMOTE, false);
            }
        }
        return engine;
    }

    public static synchronized Engine bootClientEngine(final String host, final int port, final String user, final String password, final String engineName, final boolean savePassword) {

        return bootClientEngine(host, port, user, password, engineName, savePassword, DataStoreType.DB4O);
    }

    private static synchronized Engine bootClientEngine(final String host, final int port, final String user, final String password, final String engineName, final boolean savePassword, final DataStoreType type) {

        assert engineMap.get(engineName) == null;

        if (!type.supportsRemote) {
            throw new UnsupportedOperationException("Client / Server operation not supported for this type.");
        }

        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        Engine engine = null;

        // start the client message bus
        if (MessageBus.getInstance(engineName).setRemote(host, port + 1)) {
            pref.putInt(LAST_PORT, port);
            pref.put(LAST_HOST, host);
            pref.put(LAST_USER, user);
            pref.putBoolean(LAST_REMOTE, true);

            DataStore dataStore = type.getDataStore();

            // connect to the remote server
            engine = dataStore.getClientEngine(host, port, user, password, engineName);

            if (engine != null) {
                logger.info(Resource.get().getString("Message.EngineStart"));

                engineMap.put(engineName, engine);
                dataStoreMap.put(engineName, dataStore);

                Message message = new Message(MessageChannel.SYSTEM, ChannelEvent.FILE_LOAD_SUCCESS, engine);
                MessageBus.getInstance(engineName).fireEvent(message);

                if (password != null) {
                    // remember if the user used a password for the last session
                    pref.putBoolean(LAST_USEDPASSWORD, password.length() > 0);

                    if (savePassword) {
                        pref.put(LAST_PASSWORD, password);
                    } else {
                        pref.put(LAST_PASSWORD, "");
                    }
                } else {
                    pref.putBoolean(LAST_USEDPASSWORD, false);
                    pref.put(LAST_PASSWORD, "");
                }
            }
        }

        return engine;
    }

    private static DataStoreType getDataStoreByType(final File file) {
        FileType type = FileMagic.magic(file);

        if (type == FileType.db4o) {
            return DataStoreType.DB4O;
        } else if (type == FileType.jGnash2XML) {
            return DataStoreType.XML;
        } else if (type == FileType.BinaryXStream) {
            return DataStoreType.BINARY_XSTREAM;
        }

        return null;
    }

    public static float getFileVersion(final File file) {
        float version = 0;

        FileType type = FileMagic.magic(file);

        if (type == FileType.db4o) {
            return Db4oDataStore.getFileVersion(file);
        } else if (type == FileType.jGnash2XML) {
            return XMLDataStore.getFileVersion(file);
        } else if (type == FileType.BinaryXStream) {
            return BinaryXStreamDataStore.getFileVersion(file);
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
        String filesep = System.getProperty("file.separator");

        return base + filesep + DEFAULT_DIR + filesep + userName;
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
            return getLastUser() + "@" + getLastHost();
        }

        return getLastDatabase();
    }

    /**
     * Returns the user of the last open database. If a database has not been
     * opened, then the default user will be returned.
     *
     * @return Last database user or default
     */
    public static synchronized String getLastUser() {
        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        return pref.get(LAST_USER, "sa");
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

    /**
     * Returns the password of the last open database. If a database has not
     * been opened, then the default password will be returned.
     *
     * @return Last database user or default
     */
    public static synchronized String getLastPassword() {
        Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        return pref.get(LAST_PASSWORD, "");
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
