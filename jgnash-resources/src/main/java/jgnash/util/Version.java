/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import java.util.ResourceBundle;

/**
 * Utility class for application version
 *
 * @author Craig Cavanaugh
 */
public class Version {

    private static final String JGNASH_RESOURCE_CONSTANTS = "jgnash/resource/constants";

    private static final String VERSION = "version";

    private static final String NAME = "name";

    private Version() {
        // Utility class
    }

    public static String getAppVersion() {
        return ResourceBundle.getBundle(JGNASH_RESOURCE_CONSTANTS).getString(VERSION);
    }

    public static String getAppName() {
        return ResourceBundle.getBundle(JGNASH_RESOURCE_CONSTANTS).getString(NAME);
    }
}
