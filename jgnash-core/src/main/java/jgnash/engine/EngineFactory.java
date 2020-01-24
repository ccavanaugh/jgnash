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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.DoubleConsumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import jgnash.engine.jpa.JpaNetworkServer;
import jgnash.engine.jpa.SqlUtils;
import jgnash.engine.message.ChannelEvent;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.xstream.BinaryXStreamDataStore;
import jgnash.engine.xstream.XMLDataStore;
import jgnash.resource.util.OS;
import jgnash.resource.util.ResourceUtils;
import jgnash.util.FileMagic;
import jgnash.util.FileMagic.FileType;
import jgnash.util.FileUtils;
import jgnash.util.Nullable;

/**
 * Factory class for obtaining an engine instance.
 * <p>
 * The filename of the database or remote server must be explicitly set before an Engine instance will be returned
 *
 * @author Craig Cavanaugh
 */
public class EngineFactory {

    public static final char[] EMPTY_PASSWORD = new char[]{};

    public static final String LOCALHOST = "localhost";

    public static final String DEFAULT = "default";

    public static final String REMOTE_PREFIX = "@";

    private static final String LAST_DATABASE = "LastDatabase";

    private static final String LAST_HOST = "LastHost";

    private static final String LAST_PORT = "LastPort";

    private static final String USED_PASSWORD = "LastUsedPassword";

    private static final String LAST_REMOTE = "LastRemote";

    /**
     * Default directory for jGnash data. To be located in the default user
     * directory
     */
    private static final String DEFAULT_DIR = "jGnash";

    private static final Logger logger = Logger.getLogger(EngineFactory.class.getName());

    private static final Map<String, Engine> engineMap = new HashMap<>();

    private static final Map<String, DataStore> dataStoreMap = new HashMap<>();

    private EngineFactory() {
    }

    /**
     * Registers a {@code Handler} with the class logger.
     * This also ensures the static logger is initialized.
     *
     * @param handler {@code Handler} to register
     */
    public static void addLogHandler(final Handler handler) {
        logger.addHandler(handler);
    }

    public static boolean doesDatabaseExist(final String database, final DataStoreType type) {
        if (FileUtils.fileHasExtension(database)) {
            return Files.isReadable(Paths.get(database));
        }

        return Files.isReadable(Paths.get(database + type.getDataStore().getFileExt()));
    }

    public static boolean deleteDatabase(final String database) {
        try {
            return Files.deleteIfExists(Paths.get(database));
        } catch (final IOException e) {
            logger.warning(e.getLocalizedMessage());
            return false;
        }
    }

    /**
     * Returns the engine with the given name.
     *
     * @param name engine name to look for
     * @return returns {@code null} if it does not exist
     */
    @Nullable
    public static synchronized Engine getEngine(final String name) {
        return engineMap.get(name);
    }

    private static void exportCompressedXML(final String engineName) {
        final Engine oldEngine = engineMap.get(engineName);
        final DataStore oldDataStore = dataStoreMap.get(engineName);

        exportCompressedXML(oldDataStore.getFileName(), oldEngine.getStoredObjects());
    }

    public static void exportCompressedXML(final String fileName, final Collection<StoredObject> objects) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

        final DataStore xmlDataStore = new XMLDataStore();

        Path xmlFile = Paths.get(FileUtils.stripFileExtension(fileName) + "-" + dateTimeFormatter.format(LocalDateTime.now())
                + xmlDataStore.getFileExt());

        // push the intermediary file to the temporary directory
        xmlFile = Paths.get(System.getProperty("java.io.tmpdir") + xmlFile.getFileSystem().getSeparator()
                + xmlFile.getFileName().toString());

        xmlDataStore.saveAs(xmlFile, objects, ignored -> { });

        Path zipFile = Paths.get(FileUtils.stripFileExtension(fileName) + "-" + dateTimeFormatter.format(LocalDateTime.now())
                + ".zip");

        FileUtils.compressFile(xmlFile, zipFile);

        try {
            Files.delete(xmlFile);
        } catch (final IOException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            logger.log(Level.WARNING, "Was not able to delete the temporary file: {0}", xmlFile);
        }
    }

    public static void removeOldCompressedXML(final String fileName, final int limit) {
        final Path path = Paths.get(fileName);

        String baseFile = FileUtils.stripFileExtension(path.toString());

        // '\' on Windows platform must be replaced with '\\' to prevent an exception
        if (OS.isSystemWindows()) {
            baseFile = baseFile.replace("\\","\\\\");
        }

        // old files use the base file name plus a '-' and a 8 digit date plus a '-' and a 4 digit time stamp
        final List<Path> fileList = FileUtils.getDirectoryListing(path.getParent(), baseFile + "-\\d{8}-\\d{4}.zip");

        if (fileList.size() > limit) {
            for (int i = 0; i < fileList.size() - limit; i++) {
                try {
                    Files.delete(fileList.get(i));
                } catch (final IOException e) {
                    logger.log(Level.WARNING, "Unable to delete the file: {0}", fileList.get(i));
                }
            }
        }
    }

    public static synchronized void closeEngine(final String engineName) {
        Engine oldEngine = engineMap.get(engineName);
        DataStore oldDataStore = dataStoreMap.get(engineName);

        if (oldEngine != null) {

            // stop and wait for all working background services to complete
            oldEngine.stopBackgroundServices();

            // Post a message so the GUI knows what is going on
            final Message message = new Message(MessageChannel.SYSTEM, ChannelEvent.FILE_CLOSING, oldEngine);
            MessageBus.getInstance(engineName).fireBlockingEvent(message);  // block until event has been completely processed

            if (oldEngine.isFileDirty()) {  // should a backup file be created?
                // Dump an XML backup
                if (oldEngine.createBackups() && oldDataStore.isLocal()) {
                    exportCompressedXML(engineName);
                }

                // Purge old backups
                if (oldEngine.removeOldBackups() && oldDataStore.isLocal()) {
                    removeOldCompressedXML(oldDataStore.getFileName(), oldEngine.getRetainedBackupLimit());
                }
            } else {
                logger.info("File was not dirty");
            }

            // Initiate a complete shutdown
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
     * {@code Engine} instance will be returned.
     *
     * @param fileName   filename to load
     * @param engineName engine identifier
     * @param password   connection password
     * @return new {@code Engine} instance if successful, null otherwise
     * @see Engine
     */
    public static synchronized Engine bootLocalEngine(final String fileName, final String engineName,
                                                      final char[] password) {
        final DataStoreType type = getDataStoreByType(fileName);

        Engine engine = null;

        if (type != null) {
            engine = bootLocalEngine(fileName, engineName, password, type);
        }

        return engine;
    }

    /**
     * Boots a local Engine for a file. If the file does not exist, it will be created. Otherwise it will be loaded.
     * If successful, a new {@code Engine} instance will be returned.
     *
     * @param fileName   filename to load or create
     * @param engineName engine identifier
     * @param password   password for the file
     * @param type       {@code DataStoreType} type to use for storage
     * @return new {@code Engine} instance if successful
     * @see Engine
     * @see DataStoreType
     */
    public static synchronized Engine bootLocalEngine(final String fileName, final String engineName,
                                                      final char[] password, final DataStoreType type) {

        Instant start = Instant.now();

        MessageBus.getInstance(engineName).setLocal();

        final DataStore dataStore = type.getDataStore();

        final Engine engine = dataStore.getLocalEngine(fileName, engineName, password);

        if (engine != null) {
            logger.info(ResourceUtils.getString("Message.EngineStart"));
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

            logger.log(Level.INFO, "Boot time was {0} milliseconds",
                    ChronoUnit.MILLIS.between(start, Instant.now()));
        }
        return engine;
    }

    public static synchronized Engine bootClientEngine(final String host, final int port, final char[] password,
                                                       final String engineName) {

        if (engineMap.get(engineName) != null) {
            throw new EngineException("A stale engine was found in the map");
        }

        final Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        Engine engine = null;

        // start the client message bus
        if (MessageBus.getInstance(engineName).setRemote(host, port + JpaNetworkServer.MESSAGE_SERVER_INCREMENT,
                password)) {

            pref.putInt(LAST_PORT, port);
            pref.put(LAST_HOST, host);
            pref.putBoolean(LAST_REMOTE, true);

            final MessageBus messageBus = MessageBus.getInstance(engineName);

            // after starting the remote message bus, it should receive the path on the server
            final String remoteDataBasePath = messageBus.getRemoteDataBasePath();
            final DataStoreType dataStoreType = messageBus.getRemoteDataStoreType();

            if (remoteDataBasePath == null || remoteDataBasePath.isEmpty() || dataStoreType == null) {
                throw new EngineException("Invalid connection wih the message bus");
            }

            logger.log(Level.INFO, "Remote path was {0}", remoteDataBasePath);
            logger.log(Level.INFO, "Remote data store was {0}", dataStoreType.name());
            logger.log(Level.INFO, "Engine name was {0}", engineName);

            DataStore dataStore = dataStoreType.getDataStore();

            // connect to the remote server
            engine = dataStore.getClientEngine(host, port, password, remoteDataBasePath);

            if (engine != null) {
                logger.info(ResourceUtils.getString("Message.EngineStart"));

                engineMap.put(engineName, engine);
                dataStoreMap.put(engineName, dataStore);

                // remember if the user used a password for the last session
                pref.putBoolean(USED_PASSWORD, password.length > 0);

                final Message message = new Message(MessageChannel.SYSTEM, ChannelEvent.FILE_LOAD_SUCCESS, engine);
                MessageBus.getInstance(engineName).fireEvent(message);
            }
        }

        return engine;
    }

    private static DataStoreType getDataStoreByType(final Path file) {
        final FileType type = FileMagic.magic(file);

        switch (type) {
            case jGnash2XML:
                return DataStoreType.XML;
            case BinaryXStream:
                return DataStoreType.BINARY_XSTREAM;
            case h2:
                return DataStoreType.H2_DATABASE;
            case h2mv:
                return DataStoreType.H2MV_DATABASE;
            case hsql:
                return DataStoreType.HSQL_DATABASE;
            default:            	
            	break;
        }

        return null;
    }

    public static DataStoreType getDataStoreByType(final String fileName) {
        return getDataStoreByType(Paths.get(fileName));
    }

    public static float getFileVersion(final Path file, final char[] password) {
        float version = 0;

        final FileType type = FileMagic.magic(file);

        switch (type) {
            case jGnash2XML:
                version = XMLDataStore.getFileVersion(file);
                break;
            case BinaryXStream:
                version = BinaryXStreamDataStore.getFileVersion(file);
                break;
            case h2:
            case h2mv:
            case hsql:
                try {
                    version = SqlUtils.getFileVersion(file.toString(), password);
                } catch (final Exception e) {
                    version = 0;
                }
                break;
            default:
            	break;
        }

        return version;
    }

    /**
     * Returns the default path to the database without a file extension.
     *
     * @return Default path to the database
     */
    public static synchronized String getDefaultDatabase() {
        final String base = System.getProperty("user.home");
        final String userName = System.getProperty("user.name");

        return base + FileUtils.SEPARATOR + DEFAULT_DIR + FileUtils.SEPARATOR + userName;
    }

    /**
     * Returns the last open database. If a database has not been opened, then
     * the default database will be returned.
     *
     * @return Last open or default database
     */
    public static synchronized String getLastDatabase() {
        final Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        return pref.get(LAST_DATABASE, getDefaultDatabase());
    }

    public static synchronized String getActiveDatabase() {
        if (getLastRemote()) {
            return REMOTE_PREFIX + getLastHost();
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
        final Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        return pref.get(LAST_HOST, EngineFactory.LOCALHOST);
    }

    /**
     * Returns the port of the last remote database connection. If a remote
     * database has not been opened, then the default port will be returned.
     *
     * @return Last remote database port or default
     */
    public static synchronized int getLastPort() {
        final Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        return pref.getInt(LAST_PORT, JpaNetworkServer.DEFAULT_PORT);
    }

    /**
     * Returns true if the last connection was made to a remote host.
     *
     * @return true if the last connection was made to a remote host
     */
    public static synchronized boolean getLastRemote() {
        final Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        return pref.getBoolean(LAST_REMOTE, false);
    }

    public static synchronized boolean usedPassword() {
        final Preferences pref = Preferences.userNodeForPackage(EngineFactory.class);

        return pref.getBoolean(USED_PASSWORD, false);
    }

    /**
     * Saves the active database as a new file/format
     *
     * @param destination new file
     * @param percentCompleteConsumer progress consumer
     * @throws IOException IO error
     */
    public static void saveAs(final String destination, final DoubleConsumer percentCompleteConsumer) throws IOException {

        final String fileExtension = "." + FileUtils.getFileExtension(destination);
        DataStoreType newFileType = DataStoreType.BINARY_XSTREAM;   // default for a new file

        if (fileExtension.length() > 1) {   // should have more than just the period in it
            for (final DataStoreType type : DataStoreType.values()) {
                if (type.getDataStore().getFileExt().equalsIgnoreCase(fileExtension)) {
                    newFileType = type;
                    break;
                }
            }
        }

        final Path newFile = Paths.get(FileUtils.stripFileExtension(destination)
                + newFileType.getDataStore().getFileExt());

        final Path current = Paths.get(EngineFactory.getActiveDatabase());

        // don't perform the save if the destination is going to overwrite the current database
        if (!current.equals(newFile)) {
            final DataStoreType currentType = dataStoreMap.get(EngineFactory.DEFAULT).getType();

            // Need to create an interim copy when converting a relational database
            if (currentType.supportsRemote && newFileType.supportsRemote) {
                final Path tempFile = Files.createTempFile("jgnash-tmp", BinaryXStreamDataStore.FILE_EXT);

                Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                if (engine != null) {
                    // Get collection of object to persist
                    Collection<StoredObject> objects = engine.getStoredObjects();

                    // Write everything to a temporary file
                    DataStoreType.BINARY_XSTREAM.getDataStore().saveAs(tempFile, objects, value -> {
                        percentCompleteConsumer.accept(value * 0.5);   // doing it twice
                    });

                    // Close the current file
                    EngineFactory.closeEngine(EngineFactory.DEFAULT);

                    // Boot the engine using the temporary file
                    EngineFactory.bootLocalEngine(tempFile.toString(), EngineFactory.DEFAULT,
                            EngineFactory.EMPTY_PASSWORD);

                    engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                    if (engine != null) {

                        // Get collection of object to persist
                        objects = engine.getStoredObjects();

                        // Write everything to the new file and close
                        newFileType.getDataStore().saveAs(newFile, objects,
                                value -> percentCompleteConsumer.accept(0.5 + value * 0.5));
                        EngineFactory.closeEngine(EngineFactory.DEFAULT);

                        percentCompleteConsumer.accept(1);

                        // Boot the engine with the new file
                        EngineFactory.bootLocalEngine(newFile.toString(), EngineFactory.DEFAULT,
                                EngineFactory.EMPTY_PASSWORD);
                    }

                    try {
                        Files.delete(tempFile);
                    } catch (final IOException ioe) {
                        Logger.getLogger(EngineFactory.class.getName())
                                .info(ResourceUtils.getString("Message.Error.RemoveTempFile"));
                    }
                }
            } else {    // Simple
                Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                if (engine != null) {
                    final Collection<StoredObject> objects = engine.getStoredObjects();
                    newFileType.getDataStore().saveAs(newFile, objects, percentCompleteConsumer);
                    EngineFactory.closeEngine(EngineFactory.DEFAULT);

                    EngineFactory.bootLocalEngine(newFile.toString(), EngineFactory.DEFAULT,
                            EngineFactory.EMPTY_PASSWORD);
                }
            }
        }
    }

    /**
     * Saves a closed database as a new file/format
     *
     * @param fileName file to save a copy of
     * @param newFileName new file
     * @param password password
     * @param percentCompleteConsumer progress consumer
     * 
     * @throws IOException IO error
     */
    public static void saveAs(final String fileName, final String newFileName, final char[] password,
                              final DoubleConsumer percentCompleteConsumer) throws IOException {

        Objects.requireNonNull(fileName);
        Objects.requireNonNull(newFileName);
        Objects.requireNonNull(password);

        final String ENGINE = UUID.randomUUID().toString();    // create a temporary engine ID for utility use only

        final String fileExtension = "." + FileUtils.getFileExtension(newFileName);

        DataStoreType newFileType = DataStoreType.BINARY_XSTREAM;   // default for a new file

        // Determine the data store type given the file extension
        if (fileExtension.length() > 1) {   // should have more than just the period in it
            for (final DataStoreType type : DataStoreType.values()) {
                if (type.getDataStore().getFileExt().equalsIgnoreCase(fileExtension)) {
                    newFileType = type;
                    break;
                }
            }
        }

        final Path newFile = Paths.get(FileUtils.stripFileExtension(newFileName)
                + newFileType.getDataStore().getFileExt());

        final Path current = Paths.get(fileName);

        // don't perform the save if the destination is going to overwrite the current database
        if (!current.equals(newFile)) {

            // Need to know the data store type for correct behavior
            final DataStoreType currentType = EngineFactory.getDataStoreByType(fileName);

            Objects.requireNonNull(currentType);    // fail if type is null

            // Create a utility engine instead of using the default
            Engine engine = EngineFactory.bootLocalEngine(fileName, ENGINE, password);

            if (currentType.supportsRemote && newFileType.supportsRemote) { // Relational database
                final Path tempFile = Files.createTempFile("jgnash", BinaryXStreamDataStore.FILE_EXT);

                if (engine != null) {
                    // Get collection of object to persist
                    Collection<StoredObject> objects = engine.getStoredObjects();

                    // Write everything to a temporary file
                    DataStoreType.BINARY_XSTREAM.getDataStore().saveAs(tempFile, objects,  value -> {
                        percentCompleteConsumer.accept(value * 0.5);   // doing it twice
                    });
                    EngineFactory.closeEngine(ENGINE);

                    // Boot the engine with the temporary file
                    engine = EngineFactory.bootLocalEngine(tempFile.toString(), ENGINE, EngineFactory.EMPTY_PASSWORD);

                    if (engine != null) {

                        // Get collection of object to persist
                        objects = engine.getStoredObjects();

                        // Write everything to the new file
                        newFileType.getDataStore().saveAs(newFile, objects,
                                value -> percentCompleteConsumer.accept(0.5 + value * 0.5));
                        EngineFactory.closeEngine(ENGINE);

                        // reset the password
                        SqlUtils.changePassword(newFileName, EngineFactory.EMPTY_PASSWORD, password);
                        percentCompleteConsumer.accept(1);
                    }

                    try {
                        Files.delete(tempFile);
                    } catch (final IOException ioe) {
                        Logger.getLogger(EngineFactory.class.getName())
                                .info(ResourceUtils.getString("Message.Error.RemoveTempFile"));
                    }
                }
            } else {    // Simple
                if (engine != null) {
                    final Collection<StoredObject> objects = engine.getStoredObjects();
                    newFileType.getDataStore().saveAs(newFile, objects, percentCompleteConsumer);
                    EngineFactory.closeEngine(ENGINE);
                }
            }
        }
    }
}
