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

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.StoredObject;
import jgnash.engine.StoredObjectComparator;
import jgnash.message.LocalServerListener;
import jgnash.message.MessageBusRemoteServer;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.FileUtils;
import jgnash.util.Resource;
import org.h2.tools.Server;

import java.io.File;
import java.io.FileNotFoundException;
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

    private volatile boolean stop = false;

    private static final int BACKUP_PERIOD = 2;

    private volatile boolean dirty = false;

    protected EntityManager em;

    private Server server;

    public synchronized void startServer(final String fileName, final int port, final char[] password, final boolean webConsole) {
        stop = false;

        // Start the H2 server
        try {

            boolean useSSL = Boolean.parseBoolean(System.getProperties().getProperty("ssl"));

            List<String> serverArgs= new ArrayList<>();

            serverArgs.add("-tcpPort");
            serverArgs.add(String.valueOf(port));
            serverArgs.add("-tcpAllowOthers");

            if (useSSL)  {
                serverArgs.add("-tcpSSL");
            }

            if (webConsole) {
                serverArgs.add("-web");
            }

            server = Server.createTcpServer(serverArgs.toArray(new String[serverArgs.size()]));
            server.start();
        } catch (SQLException e) {
            Logger.getLogger(JpaNetworkServer.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }

        final Engine engine = createEngine(fileName, port, password);

        if (engine != null) {

            // Start the message bus and pass the file name so it can be reported to the client
            MessageBusRemoteServer messageServer = new MessageBusRemoteServer(port + 1);
            messageServer.startServer(fileName, password);

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

            messageServer.addLocalListener(new LocalServerListener() {

                @Override
                public void messagePosted(final String event) {

                    // look for a remote request to stop the server
                    if (event.startsWith("<STOP_SERVER>")) {
                        stopServer();
                    }

                    dirty = true;
                }
            });

            // wait here forever
            try {
                if (!stop) {
                    this.wait(Long.MAX_VALUE); // wait forever for notify() from stopServer()
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(JpaNetworkServer.class.getName()).log(Level.SEVERE, null, ex);
            }

            backupExecutor.shutdown();

            exportXML(engine, fileName);
            EngineFactory.removeOldCompressedXML(fileName);

            messageServer.stopServer();

            EngineFactory.closeEngine(EngineFactory.DEFAULT);

            server.stop();
        }
    }

    /**
     * stops this server.
     */
    private synchronized void stopServer() {
        stop = true;
        this.notify();
    }

    private Engine createEngine(final String fileName, final int port, final char[] password) {

        Properties properties = JpaConfiguration.getClientProperties(fileName, "localhost", port, password);

        Logger.getLogger(JpaNetworkServer.class.getName()).info("Local connection url is: " + properties.getProperty(JpaConfiguration.JAVAX_PERSISTENCE_JDBC_URL));

        Engine engine = null;

        try {
            if (!FileUtils.isFileLocked(fileName)) {

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
                EntityManagerFactory factory = Persistence.createEntityManagerFactory("jgnash", properties);

                em = factory.createEntityManager();

                Logger.getLogger(JpaDataStore.class.getName()).info("Created local JPA container and engine");
                engine = new Engine(new JpaEngineDAO(em, true), EngineFactory.DEFAULT); // treat as a remote engine
            } else {
                Logger.getLogger(JpaNetworkServer.class.getName()).severe(Resource.get().getString("Message.FileIsLocked"));
            }
        } catch (FileNotFoundException e) {
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
