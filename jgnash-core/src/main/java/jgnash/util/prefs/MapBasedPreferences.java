/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.util.prefs;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

/**
 * Map based Preferences implementation.  Preferences must be persisted using the
 * {@code exportSubtree(OutputStream os)} and {@code Preferences.importPreferences(InputStream)}
 * methods.
 *
 * @author Craig Cavanaugh
 *
 */
class MapBasedPreferences extends AbstractPreferences {

    private final boolean isUserNode;

    private final Map<String, String> map = new HashMap<>();

    MapBasedPreferences(final MapBasedPreferences parent, final String name, final boolean isUserNode) {
        super(parent, name);
        this.isUserNode = isUserNode;
        newNode = true;
    }

    /**
     {@inheritDoc}
     */
    @Override
    public boolean isUserNode() {
        return isUserNode;
    }

    /**
     {@inheritDoc}
     */
    @Override
    protected void putSpi(final String key, final String value) {
        map.put(key, value);
    }

    /**
     {@inheritDoc}
     */
    @Override
    protected String getSpi(final String key) {
        return map.get(key);
    }

    /**
     {@inheritDoc}
     */
    @Override
    protected void removeSpi(final String key) {
        map.remove(key);
    }

    /**
     {@inheritDoc}
     */
    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        map.clear();
    }

    /**
     {@inheritDoc}
     */
    @Override
    protected String[] keysSpi() throws BackingStoreException {
        return map.keySet().toArray(new String[map.size()]);
    }

    /**
     {@inheritDoc}
     */
    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        return new String[0];
    }

    /**
     {@inheritDoc}
     */
    @Override
    protected AbstractPreferences childSpi(final String name) {
        return new MapBasedPreferences(this, name, isUserNode);
    }

    /**
     {@inheritDoc}
     */
    @Override
    protected void syncSpi() throws BackingStoreException {

    }

    /**
     {@inheritDoc}
     */
    @Override
    protected void flushSpi() throws BackingStoreException {

    }
}
