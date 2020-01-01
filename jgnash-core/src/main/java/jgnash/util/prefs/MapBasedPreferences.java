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
package jgnash.util.prefs;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;

/**
 * Map based Preferences implementation.  Preferences must be persisted using the
 * {@code exportSubtree(OutputStream os)} and {@code Preferences.importPreferences(InputStream)}
 * methods.
 *
 * @author Craig Cavanaugh
 */
@SuppressWarnings("unused")
class MapBasedPreferences extends AbstractPreferences {

    private final boolean isUserNode;

    private final Map<String, String> map = new HashMap<>();

    MapBasedPreferences(final MapBasedPreferences parent, final String name, final boolean isUserNode) {
        super(parent, name);
        this.isUserNode = isUserNode;
        newNode = true;
    }

    @Override
    public boolean isUserNode() {
        return isUserNode;
    }

    @Override
    protected void putSpi(final String key, final String value) {
        map.put(key, value);
    }

    @Override
    protected String getSpi(final String key) {
        return map.get(key);
    }

    @Override
    protected void removeSpi(final String key) {
        map.remove(key);
    }

    @Override
    protected void removeNodeSpi() {
        map.clear();
    }

    @Override
    protected String[] keysSpi() {
        return map.keySet().toArray(new String[0]);
    }

    @Override
    protected String[] childrenNamesSpi() {
        return new String[0];
    }

    @Override
    protected AbstractPreferences childSpi(final String name) {
        return new MapBasedPreferences(this, name, isUserNode);
    }

    @Override
    protected void syncSpi() {
        // implementation not needed, memory based
    }

    @Override
    protected void flushSpi() {
        // implementation not needed, memory based
    }
}
