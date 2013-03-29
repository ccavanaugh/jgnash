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
import jgnash.engine.TrashObject;
import jgnash.message.LocalServerListener;
import jgnash.message.MessageBusRemoteServer;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.FileUtils;
import jgnash.util.Resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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

    private EntityManagerFactory factory;

    private EntityManager em;

    public synchronized void runServer(final String fileName, final int port, final String user, final String password) {
        stop = false;

        final Engine server = createLocalServer(fileName, port, user, password);

        if (server != null) {

            // Start the message bus
            MessageBusRemoteServer messageServer = new MessageBusRemoteServer(port + 1);
            messageServer.startServer();

            // Start the backup thread that ensures an XML backup is created at set intervals
            ScheduledExecutorService backupExecutor = Executors.newSingleThreadScheduledExecutor(new DefaultDaemonThreadFactory());

            // run commit every backup period after startup
            backupExecutor.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {
                    if (dirty) {
                        exportXML(server, fileName);
                        EngineFactory.removeOldCompressedXML(fileName);
                        dirty = false;
                    }
                }
            }, BACKUP_PERIOD, BACKUP_PERIOD, TimeUnit.HOURS);

            messageServer.addLocalListener(new LocalServerListener() {

                @Override
                public void messagePosted(final String event) {
                    dirty = true;
                }
            });

            try {
                if (!stop) {
                    this.wait(Long.MAX_VALUE); // wait forever for notify() from close()
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(JpaNetworkServer.class.getName()).log(Level.SEVERE, null, ex);
            }

            backupExecutor.shutdown();

            exportXML(server, fileName);
            EngineFactory.removeOldCompressedXML(fileName);

            messageServer.stopServer();

            EngineFactory.closeEngine(EngineFactory.DEFAULT);
        }
    }

    /**
     * closes this server.
     */
    private synchronized void close() {
        stop = true;
        this.notify();
    }

    private Engine createLocalServer(final String fileName, final int port, final String user, final String password) {

        Properties properties = JpaConfiguration.getServerProperties(fileName, port, user, password);

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
                factory = Persistence.createEntityManagerFactory("jgnash", properties);

                em = factory.createEntityManager();

                Logger.getLogger(JpaDataStore.class.getName()).info("Created local JPA container and engine");
                engine = new Engine(new JpaEngineDAO(em, false), EngineFactory.DEFAULT);
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

        for (Iterator<StoredObject> i = list.iterator(); i.hasNext(); ) {
            StoredObject o = i.next();

            if (o instanceof TrashObject || o.isMarkedForRemoval()) {
                i.remove();
            }
        }

        Collections.sort(list, new StoredObjectComparator());

        EngineFactory.exportCompressedXML(fileName, list);
    }
}
