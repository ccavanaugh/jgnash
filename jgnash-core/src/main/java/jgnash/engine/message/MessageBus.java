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
package jgnash.engine.message;

import java.lang.ref.WeakReference;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.DataStoreType;
import jgnash.util.DefaultDaemonThreadFactory;

/**
 * Thread safe Message Bus.
 *
 * Listeners are stored in a CopyOnWriteArraySet to prevent duplicate listeners
 * and to ease the burden of synchronizing against multiple threads.  The iterator
 * must be used for access, but removal of weak references must be done through the
 * set, not the iterator.
 *
 * @author Craig Cavanaugh
 */
public class MessageBus {

    /**
     * Maximum wait to get a valid response from the remote message bus before giving up.
     */
    private static final long MAX_LATENCY = 5L * 1000L;

    private static final Logger logger = Logger.getLogger(MessageBus.class.getName());

    private final ConcurrentMap<MessageChannel, Set<WeakReference<MessageListener>>> map = new ConcurrentHashMap<>();

    private final ExecutorService pool = Executors.newSingleThreadExecutor(new DefaultDaemonThreadFactory("Message Bus Executor"));

    private MessageBusClient messageBusClient = null;

    private static final Map<String, MessageBus> busMap = new HashMap<>();

    private static final String DEFAULT = "default";

    /** Name given to this message bus instance. */
    private final String busName;

    private MessageBus(final String busName) {
        this.busName = busName;
    }

    /**
     * Set message bus to post remote messages.
     *
     * @param host message server name or IP address
     * @param port message server port
     * @param password connection password
     * @return {@code true} if connection to the remote server was successful
     */
    public synchronized boolean setRemote(final String host, final int port, final char[] password) {
        disconnectFromServer();
        return connectToServer(host, port, password);
    }

    /**
     * Set message bus for local operation.
     */
    public synchronized void setLocal() {
        disconnectFromServer();
    }

    /**
     * Returns the message bus instance intended for UI use only.
     *
     * @return an instance of a MessageBus
     */
    public static synchronized MessageBus getInstance() {
        return getInstance(DEFAULT);
    }

    public static synchronized MessageBus getInstance(final String name) {
        return busMap.computeIfAbsent(name, k -> new MessageBus(name));
    }

    public String getRemoteDataBasePath() {
        if (messageBusClient != null) {
            return messageBusClient.getDataBasePath();
        }
        return null;
    }

    public DataStoreType getRemoteDataStoreType() {
        if (messageBusClient != null) {
            return messageBusClient.getDataStoreType();
        }
        return null;
    }

    private void disconnectFromServer() {
        if (messageBusClient != null) {
            messageBusClient.disconnectFromServer();
            messageBusClient = null;
        }
    }

    /**
     * Issues a shutdown request to a remote server.
     *
     * @param remoteHost message server name or IP address
     * @param remotePort message server port
     * @param password connection password   
     */
    public void shutDownRemoteServer(final String remoteHost, final int remotePort, final char[] password) {
        if (remoteHost == null || remotePort <= 0) {
            throw new IllegalArgumentException();
        }

        MessageBusClient client = new MessageBusClient(remoteHost, remotePort, busName);

        if (client.connectToServer(password)) {
            client.sendRemoteShutdownRequest();
        }
    }

    private boolean connectToServer(final String remoteHost, final int remotePort, final char[] password) {
        if (remoteHost == null || remotePort <= 0) {
            throw new IllegalArgumentException();
        }

        messageBusClient = new MessageBusClient(remoteHost, remotePort, busName);

        boolean result = messageBusClient.connectToServer(password);

        if (result) {
            final LocalDateTime start = LocalDateTime.now();

            // wait for the server response to the remote database path for a max delay before timing out
            // this is the handshake that a good connection was made
            while (getRemoteDataBasePath() == null || getRemoteDataStoreType() == null) {
                if (ChronoUnit.MILLIS.between(start, LocalDateTime.now()) > MAX_LATENCY) {
                    disconnectFromServer();
                    logger.warning("Did not receive a valid response from the server");
                    result = false;
                    break;
                }
            }
        } else {
            messageBusClient = null; //make sure bad client connections are dumped
        }

        return result;
    }

    public void registerListener(final MessageListener listener, final MessageChannel... channels) {
        for (final MessageChannel channel : channels) {
            final Set<WeakReference<MessageListener>> set
                    = map.computeIfAbsent(channel, k -> new CopyOnWriteArraySet<>());

            if (containsListener(listener, channel)) {
                logger.severe("An attempt was made to install a duplicate listener");
                logStackTrace();
            } else {
                set.add(new WeakReference<>(listener));
            }
        }
    }

    private static void logStackTrace() {
        final StringBuilder trace = new StringBuilder("Stack Trace" + System.lineSeparator());

        for (final StackTraceElement element : Thread.currentThread().getStackTrace()) {
            trace.append("\tat ").append(element).append(System.lineSeparator());
        }

        logger.log(Level.SEVERE, trace.toString());
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

    /**
     * Fires an event and blocks until all listeners have processed it.
     *
     * @param message {@code Message} to send
     */
    public void fireBlockingEvent(final Message message) {
        final Future<Void> future = fireEvent(message);

        // spin until everyone has consumed the event
        while(!future.isDone()) {
            Thread.onSpinWait();
        }
    }

    /**
     * Fires and event to all listeners and return immediately with a {@code Future}
     * @param message {@code Message} to send
     *
     * @return {@code Future} indicating when all listeners have processed the event
     */
    public Future<Void> fireEvent(final Message message) {

        return pool.submit(() -> {
            final Set<WeakReference<MessageListener>> staleListener = new HashSet<>();

            // Look for and post to local listeners
            final Set<WeakReference<MessageListener>> set = map.get(message.getChannel());

            if (set != null) {
                for (final WeakReference<MessageListener> ref : set) {
                    MessageListener l = ref.get();
                    if (l != null) {
                        l.messagePosted(message);
                    } else {
                        staleListener.add(ref);
                    }
                }
            }

            // purge stale references to prevent a slowdown and wasted memory during a long application session
            for (final WeakReference<MessageListener> staleReference : staleListener) {
                set.remove(staleReference);
            }

            /* Post a remote message if configured to do so and filter system events.
             *
             * Do not re-post a remote message otherwise it will just loop through the
             * remote message system
             * */
            if (!message.isRemote() && messageBusClient != null && message.getChannel() != MessageChannel.SYSTEM) {
                messageBusClient.sendRemoteMessage(message);
            }

            return null;
        });
    }
}
