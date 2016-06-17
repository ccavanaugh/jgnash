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

    private volatile boolean stop = false;

    private static final int BACKUP_PERIOD = 2;

    private volatile boolean dirty = false;

    private EntityManager em;

    private EntityManagerFactory factory;

    private DistributedLockManager distributedLockManager;

    private DistributedAttachmentManager distributedAttachmentManager;

    public static final int DEFAULT_PORT = 5300;

    public static final String DEFAULT_PASSWORD = "";

    private static final String SERVER_ENGINE = "server";

    private static final Logger logger = Logger.getLogger(JpaNetworkServer.class.getName());

    public synchronized void startServer(final String fileName, final int port, final char[] password) {

        final File file = new File(fileName);

        // create the base directory if needed
        if (!file.exists()) {
            final File parent = file.getParentFile();

            if (parent != null && !parent.exists()) {
                boolean result = parent.mkdirs();

                if (!result) {
                    throw new RuntimeException("Could not create directory for file: " + parent.getAbsolutePath());
                }
            }
        }

        final FileMagic.FileType type = FileMagic.magic(new File(fileName));

        switch (type) {
            case h2:
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

    private boolean run(final DataStoreType dataStoreType, final String fileName, final int port,
                        final char[] password) {

        boolean result = false;

        final DistributedLockServer distributedLockServer = new DistributedLockServer(port + 2);
        final boolean lockServerStarted = distributedLockServer.startServer(password);

        final AttachmentTransferServer attachmentTransferServer = new AttachmentTransferServer(port + 3,
                AttachmentUtils.getAttachmentDirectory(Paths.get(fileName)));
        final boolean attachmentServerStarted = attachmentTransferServer.startServer(password);

        if (attachmentServerStarted && lockServerStarted) {
            final Engine engine = createEngine(dataStoreType, fileName, port, password);

            if (engine != null) {

                // Start the message bus and pass the file name so it can be reported to the client
                final MessageBusServer messageBusServer = new MessageBusServer(port + 1);
                result = messageBusServer.startServer(dataStoreType, fileName, password);

                if (result) { // don't continue if the server is not started successfully
                    // Start the backup thread that ensures an XML backup is created at set intervals
                    final ScheduledExecutorService backupExecutor
                            = Executors.newSingleThreadScheduledExecutor(new DefaultDaemonThreadFactory());

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

                    // wait here forever
                    try {
                        while (!stop) { // check for condition, handle a spurious wake up
                            wait(); // wait forever for notify() from stopServer()
                        }
                    } catch (final InterruptedException ex) {
                        Logger.getLogger(JpaNetworkServer.class.getName()).log(Level.SEVERE, null, ex);
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

            server = org.h2.tools.Server.createTcpServer(serverArgs.toArray(new String[serverArgs.size()]));
            server.start();

        } catch (SQLException e) {
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
        hsqlServer.setDatabaseName(0, "jgnash");    // the alias
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
    synchronized private void stopServer() {
        stop = true;
        this.notify();
    }

    private Engine createEngine(final DataStoreType dataStoreType, final String fileName, final int port,
                                final char[] password) {

        final Properties properties = JpaConfiguration.getClientProperties(dataStoreType, fileName, "localhost", port,
                password);

        logger.log(Level.INFO, "Local connection url is: {0}",
                properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL));

        Engine engine = null;

        try {
            factory = Persistence.createEntityManagerFactory("jgnash", properties);

            em = factory.createEntityManager();

            distributedLockManager = new DistributedLockManager("localhost", port + 2);
            distributedLockManager.connectToServer(password);

            distributedAttachmentManager = new DistributedAttachmentManager("localhost", port + 3);
            distributedAttachmentManager.connectToServer(password);

            logger.info("Created local JPA container and engine");

            // this is blocking and not returning
            engine = new Engine(new JpaEngineDAO(em, true), distributedLockManager, distributedAttachmentManager,
                    SERVER_ENGINE); // treat as a remote engine

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
