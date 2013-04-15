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
package jgnash.engine.jpa;

import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.StoredObject;
import jgnash.engine.StoredObjectComparator;
import jgnash.message.LocalServerListener;
import jgnash.message.MessageBusRemoteServer;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.FileMagic;
import jgnash.util.FileUtils;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
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

/**
 * JPA network server
 *
 * @author Craig Cavanaugh
 */
public class JpaNetworkServer {

    public static final String STOP_SERVER_MESSAGE = "<STOP_SERVER>";

    private volatile boolean stop = false;

    private static final int BACKUP_PERIOD = 2;

    private volatile boolean dirty = false;

    protected EntityManager em;

    public final static int DEFAULT_PORT = 5300;

    public synchronized void startServer(final String fileName, final int port, final char[] password) {

        File file = new File(fileName);

        // create the base directory if needed
        if (!file.exists()) {
            File parent = file.getParentFile();

            if (parent != null && !parent.exists()) {
                boolean result = parent.mkdirs();

                if (!result) {
                    throw new RuntimeException("Could not create directory for file: " + parent.getAbsolutePath());
                }
            }
        }

        FileMagic.FileType type = FileMagic.magic(new File(fileName));

        switch (type) {
            case h2:
                startH2Server(fileName, port, password);
                break;
            case hsql:
                startHsqldbServer(fileName, port, password);
                break;
            default:
                Logger.getLogger(JpaNetworkServer.class.getName()).severe("Not a valid file type for server usage");
        }
    }

    private void startH2Server(final String fileName, final int port, final char[] password) {

        org.h2.tools.Server server = null;

        stop = false;

        // Start the H2 server
        try {
            boolean useSSL = Boolean.parseBoolean(System.getProperties().getProperty("ssl"));

            List<String> serverArgs = new ArrayList<>();

            serverArgs.add("-tcpPort");
            serverArgs.add(String.valueOf(port));
            serverArgs.add("-tcpAllowOthers");

            if (useSSL) {
                serverArgs.add("-tcpSSL");
            }

            server = org.h2.tools.Server.createTcpServer(serverArgs.toArray(new String[serverArgs.size()]));
            server.start();

        } catch (SQLException e) {
            Logger.getLogger(JpaNetworkServer.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }

        final Engine engine = createEngine(Database.H2, fileName, port, password);

        if (engine != null) {

            // Start the message bus and pass the file name so it can be reported to the client
            MessageBusRemoteServer messageServer = new MessageBusRemoteServer(port + 1);
            messageServer.startServer(DataStoreType.H2_DATABASE, fileName, password);

            // Start the backup thread that ensures an XML backup is created at set intervals
            ScheduledExecutorService backupExecutor = Executors.newSingleThreadScheduledExecutor(new DefaultDaemonThreadFactory());

            // run commit every backup period after startup
            backupExecutor.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {
                    if (dirty) {
                        exportXML(engine, fileName);
                        EngineFactory.removeOldCompressedXML(fileName);
                        dirty = false;
                    }
                }
            }, BACKUP_PERIOD, BACKUP_PERIOD, TimeUnit.HOURS);

            LocalServerListener listener =  new LocalServerListener() {
                @Override
                public void messagePosted(final String event) {

                    // look for a remote request to stop the server
                    if (event.startsWith(STOP_SERVER_MESSAGE)) {
                        Logger.getLogger(JpaNetworkServer.class.getName()).info("Remote shutdown request was received");
                        stopServer();
                    }

                    dirty = true;
                }
            };

            messageServer.addLocalListener(listener);

            // wait here forever
            try {
                if (!stop) {
                    this.wait(Long.MAX_VALUE); // wait forever for notify() from stopServer()
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(JpaNetworkServer.class.getName()).log(Level.SEVERE, null, ex);
            }

            messageServer.removeLocalListener(listener);

            backupExecutor.shutdown();

            exportXML(engine, fileName);

            messageServer.stopServer();

            EngineFactory.closeEngine(EngineFactory.DEFAULT);

            em.close();

            if (server != null) {
                server.stop();
            }

            EngineFactory.removeOldCompressedXML(fileName);
        }
    }

    private void startHsqldbServer(final String fileName, final int port, final char[] password) {
        org.hsqldb.server.Server hsqlServer = new org.hsqldb.server.Server();

        hsqlServer.setPort(port);
        hsqlServer.setDatabaseName(0, "jgnash");    // the alias
        hsqlServer.setDatabasePath(0, "file:" + FileUtils.stripFileExtension(fileName));

        hsqlServer.start();

        final Engine engine = createEngine(Database.HSQLDB, fileName, port, password);

        if (engine != null) {

            // Start the message bus and pass the file name so it can be reported to the client
            MessageBusRemoteServer messageServer = new MessageBusRemoteServer(port + 1);
            messageServer.startServer(DataStoreType.HSQL_DATABASE, fileName, password);

            // Start the backup thread that ensures an XML backup is created at set intervals
            ScheduledExecutorService backupExecutor = Executors.newSingleThreadScheduledExecutor(new DefaultDaemonThreadFactory());

            // run commit every backup period after startup
            backupExecutor.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {
                    if (dirty) {
                        exportXML(engine, fileName);
                        EngineFactory.removeOldCompressedXML(fileName);
                        dirty = false;
                    }
                }
            }, BACKUP_PERIOD, BACKUP_PERIOD, TimeUnit.HOURS);


            LocalServerListener listener =  new LocalServerListener() {
                @Override
                public void messagePosted(final String event) {

                    // look for a remote request to stop the server
                    if (event.startsWith(STOP_SERVER_MESSAGE)) {
                        Logger.getLogger(JpaNetworkServer.class.getName()).info("Remote shutdown request was received");
                        stopServer();
                    }

                    dirty = true;
                }
            };

            messageServer.addLocalListener(listener);

            // wait here forever
            try {
                if (!stop) {
                    this.wait(Long.MAX_VALUE); // wait forever for notify() from stopServer()
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(JpaNetworkServer.class.getName()).log(Level.SEVERE, null, ex);
            }

            messageServer.removeLocalListener(listener);

            backupExecutor.shutdown();

            exportXML(engine, fileName);

            messageServer.stopServer();

            EngineFactory.closeEngine(EngineFactory.DEFAULT);

            em.close();

            hsqlServer.stop();

            EngineFactory.removeOldCompressedXML(fileName);
        }
    }

    /**
     * stops this server.
     */
    private synchronized void stopServer() {
        stop = true;
        this.notify();
    }

    private Engine createEngine(final Database database, final String fileName, final int port, final char[] password) {

        Properties properties = JpaConfiguration.getClientProperties(database, fileName, "localhost", port, password);

        Logger.getLogger(JpaNetworkServer.class.getName()).info("Local connection url is: " + properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL));

        Engine engine = null;

        try {
            EntityManagerFactory factory = Persistence.createEntityManagerFactory("jgnash", properties);

            em = factory.createEntityManager();

            Logger.getLogger(JpaH2DataStore.class.getName()).info("Created local JPA container and engine");
            engine = new Engine(new JpaEngineDAO(em, true), EngineFactory.DEFAULT); // treat as a remote engine
        } catch (final Exception e) {
            Logger.getLogger(JpaNetworkServer.class.getName()).log(Level.SEVERE, e.toString(), e);
        }

        return engine;
    }

    private static void exportXML(final Engine engine, final String fileName) {
        ArrayList<StoredObject> list = new ArrayList<>(engine.getStoredObjects());

        Collections.sort(list, new StoredObjectComparator());

        EngineFactory.exportCompressedXML(fileName, list);
    }
}
