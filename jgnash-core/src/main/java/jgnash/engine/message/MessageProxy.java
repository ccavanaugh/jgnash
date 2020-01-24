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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A Utility class that forwards messages to registered listeners.
 * <p>
 * This is useful for classes that must process a message first and then forward to other listeners for secondary
 * processing.
 *
 * @author Craig Cavanaugh
 */
public class MessageProxy {

    /**
     * Message Listeners.
     */
    private final List<WeakReference<MessageListener>> messageListeners = new ArrayList<>();

    /**
     * Concurrency lock.
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Shared thread pool for all instances of MessageProxy.
     */
    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

    /**
     * Register a listener.
     *
     * @param messageListener listener to register with this proxy
     */
    public final void addMessageListener(final MessageListener messageListener) {

        lock.lock();

        try {
            messageListeners.add(new WeakReference<>(messageListener));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove a listener.
     *
     * @param messageListener listener to register with this proxy
     */
    public final void removeMessageListener(final MessageListener messageListener) {
        lock.lock();

        try {
            Iterator<WeakReference<MessageListener>> iterator = messageListeners.iterator();

            while (iterator.hasNext()) {
                WeakReference<MessageListener> reference = iterator.next();

                MessageListener actionListener = reference.get();

                if (actionListener == null || actionListener == messageListener) {
                    iterator.remove();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Forwards a message to an listeners.
     *
     * @param message message to forward
     */
    public final void forwardMessage(final Message message) {

        lock.lock();

        try {

            THREAD_POOL.submit(() -> {
                Iterator<WeakReference<MessageListener>> iterator = messageListeners.iterator();

                while (iterator.hasNext()) {
                    WeakReference<MessageListener> reference = iterator.next();

                    final MessageListener actionListener = reference.get();

                    if (actionListener != null) {
                        THREAD_POOL.submit(() -> actionListener.messagePosted(message));
                    } else {
                        iterator.remove();
                    }
                }
            });
        } finally {
            lock.unlock();
        }
    }
}
