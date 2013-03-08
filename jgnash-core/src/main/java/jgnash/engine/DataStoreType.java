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
package jgnash.engine;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.db4o.Db4oDataStore;
import jgnash.engine.xstream.BinaryXStreamDataStore;
import jgnash.engine.xstream.XMLDataStore;
import jgnash.util.Resource;

/**
 * Storage type enumeration
 *
 * @author Craig Cavanaugh
 */
public enum DataStoreType {

    DB4O(
            Resource.get().getString("DataStoreType.Db4o"),
            true,
            true,
            Db4oDataStore.class),
    BINARY_XSTREAM(
            Resource.get().getString("DataStoreType.Bxds"),
            true,
            false,
            BinaryXStreamDataStore.class),
    XML(
            Resource.get().getString("DataStoreType.XML"),
            true,
            false,
            XMLDataStore.class);


    /* If true, then this DataStoreType can support remote connections */
    final transient boolean supportsRemote;

    /* If true, then this DataStoreType can support local connections */
    final transient boolean supportsLocal;

    private final transient String description;

    private final transient Class<? extends DataStore> dataStore;

    private DataStoreType(final String description, final boolean supportsLocal, final boolean supportsRemote, final Class<? extends DataStore> dataStore) {
        this.description = description;
        this.supportsLocal = supportsLocal;
        this.supportsRemote = supportsRemote;
        this.dataStore = dataStore;
    }

    public DataStore getDataStore() {
        try {
            Constructor<?> storeConst = dataStore.getDeclaredConstructor();
            return (DataStore) storeConst.newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
            Logger.getLogger(DataStoreType.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null; // unable to create object
    }

    @Override
    public String toString() {
        return description;
    }
}
