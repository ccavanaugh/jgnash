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
package jgnash.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A HashMap that allows multiple values for the same key.  If multiple
 * values exist for a given key, the values are treated as a FILO buffer.
 * <p>
 * This class is thread-safe
 *
 * @param <K> Key class
 * @param <V> Object class
 * @author Craig Cavanaugh
 */
public class MultiHashMap<K, V> extends HashMap<K, Object> {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @SuppressWarnings("unchecked")
    @Override
    public Object put(final K key, final Object value) {
        if (value instanceof ArrayList<?>) {
            throw new IllegalArgumentException("ArrayLists are not allowed");
        }

        lock.writeLock().lock();

        try {
            if (containsKey(key)) {
                Object val = super.get(key);
                if (val instanceof ArrayList<?>) {
                    ArrayList<Object> l = (ArrayList<Object>) val;
                    l.add(value);
                    return l.get(l.size() - 2);
                }
                ArrayList<Object> l = new ArrayList<>();
                l.add(val);
                l.add(value);
                super.put(key, l);
                return val;
            }
            return super.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * By default the last value in, is the first value out.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Object get(final Object key) {
        lock.readLock().lock();

        try {
            Object v = super.get(key);
            if (v instanceof ArrayList<?>) {
                ArrayList l = (ArrayList) v;
                return l.get(l.size() - 1);
            }
            return v;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns all of the objects associated with a given key.  This is guaranteed to
     * return a valid list.
     *
     * @param key The key to use
     * @return The list of values associated with the key
     */
    @SuppressWarnings("unchecked")
    public List<V> getAll(final Object key) {
        lock.readLock().lock();

        try {
            Object v = super.get(key);
            if (v instanceof ArrayList) {
                return Collections.unmodifiableList((ArrayList<V>) v);
            } else if (v == null) {
                return Collections.emptyList();
            } else {
                return Collections.singletonList((V) v);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes", "element-type-mismatch"})
    @Override
    public Object remove(final Object key) {

        lock.writeLock().lock();

        try {
            Object v = super.get(key);
            if (v instanceof ArrayList) {
                ArrayList l = (ArrayList) v;
                if (l.size() == 2) {
                    v = l.get(1);
                    super.put((K) key, l.get(0)); // remove the ArrayList

                    return v;
                }
                return l.remove(l.size() - 1);
            }
            return super.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a specific value.
     *
     * @param key   The key of the value to remove
     * @param value The specific value to remove
     * @return {@code true} if the map changed
     */
    @SuppressWarnings("rawtypes")
    public boolean removeValue(final K key, final V value) {

        boolean result = false;

        lock.writeLock().lock();

        try {
            Object v = super.get(key);
            if (v instanceof ArrayList) {
                ArrayList l = (ArrayList) v;
                if (l.remove(value)) {
                    if (l.size() == 1) { // remove the ArrayList
                        super.put(key, value);
                    }
                    result = true;
                }
            } else if (v != null && v.equals(value)) {
                result = super.remove(key) != null;
            }
        } finally {
            lock.writeLock().unlock();
        }

        return result;
    }
}