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
package jgnash.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A HashMap that allows multiple values for the same key.  If multiple
 * values exist for a given key, the values are treated as a FILO buffer.
 * <p/>
 * This class is thread-safe
 *
 * @author Craig Cavanaugh
 *
 * @param <K> Key class
 * @param <V> Object class
 */
public class MultiHashMap<K, V> extends HashMap<K, Object> {

    private static final long serialVersionUID = -631344712179108976L;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @SuppressWarnings("unchecked")
    @Override
    public Object put(K key, Object value) {
        if (value instanceof ArrayList<?>) {
            throw new IllegalArgumentException("ArrayLists are not allowed");
        }

        Lock writeLock = lock.writeLock();

        try {
            writeLock.lock();

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
            writeLock.unlock();
        }
    }

    /**
     * By default the last value in, is the first value out
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Object get(Object key) {
        Lock readLock = lock.readLock();

        try {
            readLock.lock();

            Object v = super.get(key);
            if (v instanceof ArrayList<?>) {
                ArrayList l = (ArrayList) v;
                return l.get(l.size() - 1);
            }
            return v;
        } finally {
            readLock.unlock();
        }
    }

    //     /**
    //      * Returns all of the objects associated with a given key.  This is guaranteed to
    //      * return a valid list.
    //      *
    //      * @param key The key to use
    //      * @return The list of values associated with the key
    //      */
    //     @SuppressWarnings("unchecked")
    //     public List getAll(Object key) {
    //         Lock readLock = lock.readLock();
    //
    //         try {
    //             readLock.lock();
    //
    //             Object v = super.get(key);
    //             if (v instanceof ArrayList) {
    //                 return Collections.unmodifiableList((ArrayList) v);
    //             } else if (v == null) {
    //                 return Collections.EMPTY_LIST;
    //             } else {
    //                 return Collections.singletonList(v);
    //             }
    //         } finally {
    //             readLock.unlock();
    //         }
    //     }
    
    @SuppressWarnings({"unchecked", "rawtypes", "element-type-mismatch"})
    @Override
    public Object remove(Object key) {
        Lock writeLock = lock.writeLock();

        try {
            writeLock.lock();
         
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
            writeLock.unlock();
        }
    }

    /**
     * Removes a specific value
     *
     * @param key   The key of the value to remove
     * @param value The specific value to remove
     * @return the remove value, null if it was not found in the map
     */
    @SuppressWarnings("rawtypes")
    public Object remove(K key, V value) {
        Lock writeLock = lock.writeLock();

        try {
            writeLock.lock();

            Object v = super.get(key);
            if (v instanceof ArrayList) {
                ArrayList l = (ArrayList) v;
                if (l.remove(value)) {
                    if (l.size() == 1) { // remove the ArrayList
                        super.put(key, value);
                    }
                    return value;
                }
                return null;
            } else if (v != null && v.equals(value)) {
                return super.remove(key);
            }
            return null;
        } finally {
            writeLock.unlock();
        }
    }
}