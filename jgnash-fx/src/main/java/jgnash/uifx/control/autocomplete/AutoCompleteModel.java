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
package jgnash.uifx.control.autocomplete;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Auto complete model interface.
 *
 * @author Craig Cavanaugh
 * @author Don Brown
 */
public interface AutoCompleteModel<E> {

    /**
     * Performs a search to find the best matching
     * String to the supplied String.  Returns null if nothing
     * is found
     *
     * @param content content to search for
     * @return string that was found if any
     */
    String doLookAhead(String content);

    /**
     * Returns extra information that might be stored with a
     * found string returned by doLookAhead().  This information
     * can be used to populate other fields based on matching the
     * string key.
     *
     * @param key The string key most likely returned from doLookAhead()
     * @return A collection of objects that would give extra information about the key
     */
    Collection<E> getAllExtraInfo(String key);

    /**
     * Atomic indicating if the initial load of data is complete to support
     * a full search
     *
     * @return AtomicBoolean indicating completion
     */
    AtomicBoolean isLoadComplete();
}
