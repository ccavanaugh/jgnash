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
package jgnash.engine.db4o;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.ObjectServer;
import com.db4o.config.Configuration;
import com.db4o.messaging.MessageRecipient;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.EngineFactory;
import jgnash.engine.StoredObject;
import jgnash.engine.StoredObjectComparator;
import jgnash.engine.TrashObject;
import jgnash.message.LocalServerListener;
import jgnash.message.MessageBusRemoteServer;
import jgnash.message.StopServer;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.FileUtils;
import jgnash.util.Resource;

/**
 * db4o network server
 *
 * @author Craig Cavanaugh
 */
public class Db4oNetworkServer implements MessageRecipient {

    private volatile boolean stop = false;

    private static final int BACKUP_PERIOD = 2;

    private volatile boolean dirty = false;

    public synchronized void runServer(final String fileName, final int port, final String user, final String password) {
        stop = false;

        final ObjectServer server = createObjectServer(fileName, port, user, password);

        if (server != null) {

            // Using the messaging functionality to redirect all messages to this.processMessage
            server.ext().configure().clientServer().setMessageRecipient(this);

            // to identify the thread in a debugger
            Thread.currentThread().setName(this.getClass().getName());

            // We only need low priority since the db4o server has it's own thread.
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

            // Start the message bus
            MessageBusRemoteServer messageServer = new MessageBusRemoteServer(port + 1);
            messageServer.startServer("temp");

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
                Logger.getLogger(Db4oNetworkServer.class.getName()).log(Level.SEVERE, null, ex);
            }

            backupExecutor.shutdown();

            exportXML(server, fileName);
            EngineFactory.removeOldCompressedXML(fileName);

            messageServer.stopServer();
            server.close();
        }
    }

    /**
     * closes this server.
     */
    private synchronized void close() {
        stop = true;
        this.notify();
    }

    private static ObjectServer createObjectServer(final String fileName, final int port, final String user, final String password) {
        ObjectServer objectServer = null;

        try {
            if (!FileUtils.isFileLocked(fileName)) {
                Configuration config = Db4oDataStore.createConfig();

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
                objectServer = Db4o.openServer(config, fileName, port);
                objectServer.grantAccess(user, password);
            } else {
                Logger.getLogger(Db4oNetworkServer.class.getName()).severe(Resource.get().getString("Message.FileIsLocked"));
            }
        } catch (FileNotFoundException e) {
            Logger.getLogger(Db4oNetworkServer.class.getName()).log(Level.SEVERE, e.toString(), e);
        }

        return objectServer;
    }

    /**
     * messaging callback
     *
     * @param container ObjectContainer
     * @param message   message to process
     * @see com.db4o.messaging.MessageRecipient#processMessage(ObjectContainer, Object)
     */
    @Override
    public void processMessage(final ObjectContainer container, final Object message) {
        if (message instanceof StopServer) {
            close();
        }
    }

    private static void exportXML(final ObjectServer server, final String fileName) {
        ObjectContainer container = server.ext().objectContainer();

        ArrayList<StoredObject> list = new ArrayList<>(container.query(StoredObject.class));

        for (Iterator<StoredObject> i = list.iterator(); i.hasNext();) {
            StoredObject o = i.next();

            if (o instanceof TrashObject || o.isMarkedForRemoval()) {
                i.remove();
            }
        }

        Collections.sort(list, new StoredObjectComparator());

        EngineFactory.exportCompressedXML(fileName, list);
    }
}
