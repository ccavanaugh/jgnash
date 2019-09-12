/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import jgnash.engine.AttachmentUtils;
import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineException;
import jgnash.engine.EngineFactory;
import jgnash.engine.StoredObject;
import jgnash.engine.attachment.AttachmentTransferServer;
import jgnash.engine.attachment.DistributedAttachmentManager;
import jgnash.engine.concurrent.DistributedLockManager;
import jgnash.engine.concurrent.DistributedLockServer;
import jgnash.engine.message.LocalServerListener;
import jgnash.engine.message.MessageBusServer;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.FileMagic;
import jgnash.util.FileUtils;

/**
 * JPA network server.
 *
 * @author Craig Cavanaugh
 */
public class JpaNetworkServer {

    public static final String STOP_SERVER_MESSAGE = "<STOP_SERVER>";

    public static final int MESSAGE_SERVER_INCREMENT = 1;

    static final int LOCK_SERVER_INCREMENT = 2;

    static final int TRANSFER_SERVER_INCREMENT = 3;

    private volatile boolean stop = false;

    private static final int BACKUP_PERIOD = 2;

    private volatile boolean dirty = false;

    private EntityManager em;

    private EntityManagerFactory factory;

    private DistributedLockManager distributedLockManager;

    private DistributedAttachmentManager distributedAttachmentManager;

    public static final int DEFAULT_PORT = 5300;

    private static final String SERVER_ENGINE = "server";

    private static final Logger logger = Logger.getLogger(JpaNetworkServer.class.getName());

    private Runnable callback = null;

    public synchronized void startServer(final String fileName, final int port, final char[] password) {

        final Path file = Paths.get(fileName);

        // create the base directory if needed
        if (!Files.exists(file)) {
            final Path parent = file.getParent();

            if (parent != null && !Files.exists(parent)) {
                try {
                    Files.createDirectories(parent);
                } catch (IOException e) {
                    throw new EngineException("Could not create directory for file: " + parent.toString());
                }
            }
        }

        final FileMagic.FileType type = FileMagic.magic(Paths.get(fileName));

        switch (type) {
            case h2:
            case h2mv:
                runH2Server(fileName, port, password);
                break;
            case hsql:
                runHsqldbServer(fileName, port, password);
                break;
            default:
                logger.severe("Not a valid file type for server usage");
        }

        System.exit(0); // force exit
    }

    public synchronized void startServer(final String fileName, final int port, final char[] password,
                                         final Runnable callback) {
        this.callback = callback;
        this.startServer(fileName, port, password);
    }

    private synchronized boolean run(final DataStoreType dataStoreType, final String fileName, final int port,
                        final char[] password) {

        boolean result = false;

        final DistributedLockServer distributedLockServer = new DistributedLockServer(port + LOCK_SERVER_INCREMENT);
        final boolean lockServerStarted = distributedLockServer.startServer(password);

        final AttachmentTransferServer attachmentTransferServer
                = new AttachmentTransferServer(port + TRANSFER_SERVER_INCREMENT,
                AttachmentUtils.getAttachmentDirectory(Paths.get(fileName)));
        final boolean attachmentServerStarted = attachmentTransferServer.startServer(password);

        if (attachmentServerStarted && lockServerStarted) {
            final Engine engine = createEngine(dataStoreType, fileName, port, password);

            if (engine != null) {

                // Start the message bus and pass the file name so it can be reported to the client
                final MessageBusServer messageBusServer = new MessageBusServer(port + MESSAGE_SERVER_INCREMENT);
                result = messageBusServer.startServer(dataStoreType, fileName, password);

                if (result) { // don't continue if the server is not started successfully
                    // Start the backup thread that ensures an XML backup is created at set intervals
                    final ScheduledExecutorService backupExecutor
                            = Executors.newSingleThreadScheduledExecutor(
                                    new DefaultDaemonThreadFactory("JPA Network Server Executor"));

                    // run commit every backup period after startup
                    backupExecutor.scheduleWithFixedDelay(() -> {
                        if (dirty) {
                            exportXML(engine, fileName);
                            EngineFactory.removeOldCompressedXML(fileName, engine.getRetainedBackupLimit());
                            dirty = false;
                        }
                    }, BACKUP_PERIOD, BACKUP_PERIOD, TimeUnit.HOURS);

                    final LocalServerListener listener = event -> {

                        // look for a remote request to stop the server
                        if (event.startsWith(STOP_SERVER_MESSAGE)) {
                            logger.info("Remote shutdown request was received");
                            stopServer();
                        }

                        dirty = true;
                    };

                    messageBusServer.addLocalListener(listener);

                    // if a callback has been registered, call it
                    if (callback != null) {
                        callback.run();
                    }

                    // wait here forever
                    try {
                        while (!stop) { // check for condition, handle a spurious wake up
                            wait(); // wait forever for notify() from stopServer()
                        }
                    } catch (final InterruptedException ex) {
                        logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
                        Thread.currentThread().interrupt();
                    }

                    messageBusServer.removeLocalListener(listener);

                    backupExecutor.shutdown();

                    exportXML(engine, fileName);

                    messageBusServer.stopServer();

                    EngineFactory.closeEngine(SERVER_ENGINE);

                    EngineFactory.removeOldCompressedXML(fileName, engine.getRetainedBackupLimit());

                    distributedLockManager.disconnectFromServer();
                    distributedAttachmentManager.disconnectFromServer();

                    distributedLockServer.stopServer();
                    attachmentTransferServer.stopServer();

                    em.close();

                    factory.close();
                }
            }
        } else {
            if (lockServerStarted) {
                distributedLockServer.stopServer();
            }

            if (attachmentServerStarted) {
                attachmentTransferServer.stopServer();
            }
        }
        return result;
    }

    private void runH2Server(final String fileName, final int port, final char[] password) {
        org.h2.tools.Server server = null;

        stop = false;

        try {
            final List<String> serverArgs = new ArrayList<>();

            serverArgs.add("-tcpPort");
            serverArgs.add(String.valueOf(port));
            serverArgs.add("-tcpAllowOthers");

            /*boolean useSSL = Boolean.parseBoolean(System.getProperties().getProperty(EncryptionManager.ENCRYPTION_FLAG));

            if (useSSL) {
                serverArgs.add("-tcpSSL");
            }*/

            server = org.h2.tools.Server.createTcpServer(serverArgs.toArray(new String[0]));
            server.start();

        } catch (final SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        // Start the message server and engine, this should block until closed
        if (!run(DataStoreType.H2_DATABASE, fileName, port, password)) {
            logger.severe("Failed to start the server");
        }

        if (server != null) {
            server.stop();
        }
    }

    private void runHsqldbServer(final String fileName, final int port, final char[] password) {
        org.hsqldb.server.Server hsqlServer = new org.hsqldb.server.Server();

        hsqlServer.setPort(port);
        hsqlServer.setDatabaseName(0, JpaConfiguration.UNIT_NAME);    // the alias
        hsqlServer.setDatabasePath(0, "file:" + FileUtils.stripFileExtension(fileName));

        hsqlServer.start();

        // Start the message server and engine, this should block until closed
        if (!run(DataStoreType.HSQL_DATABASE, fileName, port, password)) {
            logger.severe("Failed to start the server");
        }

        hsqlServer.stop();
    }

    /**
     * stops this server.
     */
    private synchronized void stopServer() {
        stop = true;
        this.notifyAll();
    }

    private Engine createEngine(final DataStoreType dataStoreType, final String fileName, final int port,
                                final char[] password) {

        final Properties properties = JpaConfiguration.getClientProperties(dataStoreType, fileName, EngineFactory.LOCALHOST, port,
                password);

        logger.log(Level.INFO, "Local connection url is: {0}",
                properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL));

        Engine engine = null;

        try {
            // An exception will be thrown if the password is not correct, or the database did not have a password
            if (SqlUtils.isConnectionValid(properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL))) {

                /* specifies the unit name and properties.  Unit name can be used to specify a different persistence
                   unit defined in persistence.xml */
                factory = Persistence.createEntityManagerFactory(JpaConfiguration.UNIT_NAME, properties);
                em = factory.createEntityManager();

                distributedLockManager = new DistributedLockManager(EngineFactory.LOCALHOST, port + LOCK_SERVER_INCREMENT);
                distributedLockManager.connectToServer(password);

                distributedAttachmentManager = new DistributedAttachmentManager(EngineFactory.LOCALHOST, port + TRANSFER_SERVER_INCREMENT);
                distributedAttachmentManager.connectToServer(password);

                logger.info("Created local JPA container and engine");

                engine = new Engine(new JpaEngineDAO(em, true), distributedLockManager, distributedAttachmentManager,
                        SERVER_ENGINE); // treat as a remote engine
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }

        return engine;
    }

    private static void exportXML(final Engine engine, final String fileName) {
        ArrayList<StoredObject> list = new ArrayList<>(engine.getStoredObjects());

        EngineFactory.exportCompressedXML(fileName, list);
    }
}
