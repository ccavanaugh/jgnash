/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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

import java.util.UUID;

/**
 * Used to create unique business keys for the database
 *
 * @author Craig Cavanaugh
 */
class UUIDUtil {

    /**
     * Generates a unique id that does not repeat.
     * Each UUID is 36 chars long
     *
     * @return the unique id
     */
    public static String getUID() {
        return UUID.randomUUID().toString();
    }

    private UUIDUtil() {
    }
}
