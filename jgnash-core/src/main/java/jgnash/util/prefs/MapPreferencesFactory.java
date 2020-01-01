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

import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Map based Preferences implementation.
 *
 * @author Craig Cavanaugh*
 */
@SuppressWarnings("unused")
public class MapPreferencesFactory implements PreferencesFactory {

    private static final Preferences user = new MapBasedPreferences(null, "", true);

    private static final Preferences system = new MapBasedPreferences(null, "", false);

    /**
     * Returns the system root preference node.  (Multiple calls on this
     * method will return the same object reference.)
     */
    @Override
    public Preferences systemRoot() {
        return system;
    }

    /**
     * Returns the user root preference node corresponding to the calling
     * user.  In a server, the returned value will typically depend on
     * some implicit client-context.
     */
    @Override
    public Preferences userRoot() {
        return user;
    }
}
