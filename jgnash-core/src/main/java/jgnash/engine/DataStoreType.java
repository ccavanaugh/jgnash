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
package jgnash.engine;

import jgnash.engine.jpa.JpaH2DataStore;
import jgnash.engine.jpa.JpaH2MvDataStore;
import jgnash.engine.jpa.JpaHsqlDataStore;
import jgnash.engine.xstream.BinaryXStreamDataStore;
import jgnash.engine.xstream.XMLDataStore;
import jgnash.resource.util.ResourceUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Storage type enumeration.
 *
 * @author Craig Cavanaugh
 */
public enum DataStoreType {

    BINARY_XSTREAM(
            ResourceUtils.getString("DataStoreType.Bxds"),
            false,
            BinaryXStreamDataStore.class),
    H2_DATABASE (
            ResourceUtils.getString("DataStoreType.H2") + " (1.3)",
            true,
            JpaH2DataStore.class),
    H2MV_DATABASE (
            ResourceUtils.getString("DataStoreType.H2") + " (1.4)",
            true,
            JpaH2MvDataStore.class),
    HSQL_DATABASE (
            ResourceUtils.getString("DataStoreType.HSQL"),
            true,
            JpaHsqlDataStore.class),
    XML(
            ResourceUtils.getString("DataStoreType.XML"),
            false,
            XMLDataStore.class);


    /* If true, then this DataStoreType can support remote connections */
    public final transient boolean supportsRemote;

    private final transient String description;

    private final transient Class<? extends DataStore> dataStore;

    DataStoreType(final String description, final boolean supportsRemote, final Class<? extends DataStore> dataStore) {
        this.description = description;
        this.supportsRemote = supportsRemote;
        this.dataStore = dataStore;
    }

    public DataStore getDataStore() {
        try {
            return dataStore.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException ex) {
            Logger.getLogger(DataStoreType.class.getName()).log(Level.SEVERE, null, ex);

            throw new RuntimeException(ex);
        }
    }

    @Override
    public String toString() {
        return description;
    }
}
