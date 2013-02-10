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
package jgnash.message;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.LogUtils;

/**
 * Thread safe Message Bus
 *
 * @author Craig Cavanaugh
 *
 */

/*
 * Listeners are stored in a CopyOnWriteArraySet to prevent duplicate listeners
 * and to ease the burden of synchronizing against multiple threads.  The iterator
 * must be used for access, but removal of weak references must be done through the
 * set, not the iterator.
 */
public class MessageBus {

    private static final Logger logger = Logger.getLogger(MessageBus.class.getName());

    @SuppressWarnings("MapReplaceableByEnumMap")
    private final Map<MessageChannel, Set<WeakReference<MessageListener>>> map = new ConcurrentHashMap<>();

    private final ExecutorService pool = Executors.newSingleThreadExecutor(new DefaultDaemonThreadFactory());

    private MessageBusRemoteClient messageBusClient = null;

    private static final Map<String, MessageBus> busMap = new HashMap<>();

    private static final String DEFAULT = "default";

    private MessageBus() {
    }

    /**
     * Set message bus to post remote messages
     *
     * @param host message server name or IP address
     * @param port message server port
     * @return <code>true</code> if connection to the remote server was successful
     */
    public synchronized boolean setRemote(final String host, final int port) {
        disconnectFromServer();
        return connectToServer(host, port);
    }

    /**
     * Set message bus for local operation
     */
    public synchronized void setLocal() {
        disconnectFromServer();
    }

    public static synchronized MessageBus getInstance() {
        return getInstance(DEFAULT);
    }

    public static synchronized MessageBus getInstance(final String name) {
        MessageBus bus = busMap.get(name);

        if (bus == null) {
            bus = new MessageBus();
            busMap.put(name, bus);
        }
        return bus;
    }

    private void disconnectFromServer() {
        if (messageBusClient != null) {
            messageBusClient.disconnectFromServer();
            messageBusClient = null;
        }
    }

    private boolean connectToServer(final String remoteHost, final int remotePort) {             
        if (remoteHost == null || remotePort <= 0) {            
            throw new IllegalArgumentException();
        }

        messageBusClient = new MessageBusRemoteClient(remoteHost, remotePort);

        boolean result = messageBusClient.connectToServer();

        if (!result) {
            messageBusClient = null; //make sure bad client connections are dumped
        }
        return result;
    }

    public void registerListener(final MessageListener listener, final MessageChannel... channels) {
        for (MessageChannel channel : channels) {
            Set<WeakReference<MessageListener>> set = map.get(channel);
            if (set == null) {
                set = new CopyOnWriteArraySet<>();
                map.put(channel, set);
            }

            if (containsListener(listener, channel)) {
                logger.severe("An attempt was made to install a duplicate listener");
                LogUtils.logStackTrace(logger, Level.SEVERE);               
            } else {
                set.add(new WeakReference<>(listener));
            }
        }
    }

    public void unregisterListener(final MessageListener listener, final MessageChannel... channels) {
        for (MessageChannel channel : channels) {
            Set<WeakReference<MessageListener>> set = map.get(channel);

            if (set != null) {
                for (WeakReference<MessageListener> ref : set) {
                    MessageListener l = ref.get();
                    if (l == null || l == listener) {
                        set.remove(ref);
                    }
                }
            }
        }
    }

    private boolean containsListener(final MessageListener listener, final MessageChannel channel) {
        Set<WeakReference<MessageListener>> set = map.get(channel);

        if (set != null) {
            for (WeakReference<MessageListener> ref : set) {
                MessageListener l = ref.get();
                if (l == listener) {
                    return true;
                }
            }
        }
        return false;
    }

    public void fireEvent(final Message message) {
        Set<WeakReference<MessageListener>> set = map.get(message.getChannel());

        if (set != null) {
            pool.execute(new MessageHandler(message, set));
        }
    }

    /**
     * This nested class is used to update any listeners in a separate thread and post the message to the remote message
     * bus if running.
     */
    private final class MessageHandler implements Runnable {

        final Message message;

        final Set<WeakReference<MessageListener>> set;

        MessageHandler(final Message event, final Set<WeakReference<MessageListener>> set) {
            this.message = event;
            this.set = set;
        }

        @Override
        public void run() {
            for (WeakReference<MessageListener> ref : set) {
                MessageListener l = ref.get();
                if (l != null) {
                    l.messagePosted(message);
                }
            }

            /* Post a remote message if configured to do so and filter system events.
             *
             * Do not repost a remote message otherwise it will just loop through the
             * remote message system */
            if (!message.isRemote()) {
                if (messageBusClient != null && message.getChannel() != MessageChannel.SYSTEM) {
                    messageBusClient.sendRemoteMessage(message);
                }
            }
        }
    }

    /*private void _shutdown() {
    pool.shutdown();

    try {
    // Wait a while for existing tasks to terminate
    if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
    pool.shutdownNow(); // Cancel currently executing tasks
    // Wait a while for tasks to respond to being canceled
    if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
    System.err.println("Pool did not terminate");
    }
    }
    } catch (InterruptedException ie) {
    // (Re-)Cancel if current thread also interrupted
    pool.shutdownNow();
    // Preserve interrupt status
    Thread.currentThread().interrupt();
    }
    }*/

    /*public static void shutdown() {
    getInstance()._shutdown();
    }*/
}
