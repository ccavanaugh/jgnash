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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.property.SimpleBooleanProperty;

import jgnash.uifx.Options;
import jgnash.util.Nullable;

/**
 * Default model for auto complete search.
 * 
 * @author Craig Cavanaugh
 * @author Don Brown
 * @author Pranay Kumar
 */
abstract class DefaultAutoCompleteModel<E> implements AutoCompleteModel<E> {

    private final List<String> list = new LinkedList<>();

    private final SimpleBooleanProperty autoCompleteEnabled = new SimpleBooleanProperty(true);

    final SimpleBooleanProperty ignoreCaseEnabled = new SimpleBooleanProperty(false);

    private final SimpleBooleanProperty fuzzyMatchEnabled = new SimpleBooleanProperty(false);

    /**
     * Must be set to true when the initial load has been completed
     */
    final AtomicBoolean loadComplete = new AtomicBoolean(false);

    DefaultAutoCompleteModel() {

        // bind preferences to the global options
        autoCompleteEnabled.bind(Options.useAutoCompleteProperty());
        ignoreCaseEnabled.bind(Options.autoCompleteIsCaseSensitiveProperty().not());
        fuzzyMatchEnabled.bind(Options.useFuzzyMatchForAutoCompleteProperty());
    }


    @Override
    public String doLookAhead(final String content) {
        if (autoCompleteEnabled.get()) {
            return doLookAhead(content, ignoreCaseEnabled.get());
        }
        return null;
    }

    /**
     * Perform a brute force linear search top down for the best match.
     *
     * @param content content to search for
     * @param ignoreCase true is search is case insensitive
     * @return best match if found, null otherwise
     */
    private @Nullable String doLookAhead(final String content, final boolean ignoreCase) {
        if (!content.isEmpty()) {
            synchronized (list) {
                for (final String s : list) {

                    if (ignoreCase) {
                        if (s.equalsIgnoreCase(content)) {
                            break;
                        }
                    } else {
                        if (s.equals(content)) {
                            break;
                        }
                    }

                    if (startsWith(s, content, ignoreCase)) {
                        return s;
                    }
                }
            }
        }
        return null;
    }

    void addString(final String content) {
        if (content != null && !content.isEmpty()) {
            synchronized (list) {
                if (fuzzyMatchEnabled.get()) {
                    list.remove(content);       // remove old instance
                    list.add(0, content); // push it to the top of the search list
                } else {
                    int index = Collections.binarySearch(list, content);

                    if (index < 0) {
                        list.add(-index - 1, content);
                    }
                }
            }
        }
    }

    /**
     * Removes all of the strings that have been remembered.
     */
    void purge() {
        synchronized (list) {
            list.clear();
        }
    }

    void load() {
        loadComplete.set(true);
    }

    public final AtomicBoolean isLoadComplete() {
        return loadComplete;
    }

    /**
     * Returns extra information that might be stored with a
     * found string returned by doLookAhead().  This information
     * can be used to populate other fields based on matching the
     * string key.
     *
     * @param key The string key most likely returned from doLookAhead()
     * @return A list of objects that would give extra information about the key
     */
    @Override
    public Collection<E> getAllExtraInfo(final String key) {
        return Collections.emptyList();
    }

    /**
     * Tests if the source string starts with the prefix string. Case is
     * ignored.
     * 
     * @param source the source String.
     * @param prefix the prefix String.
     * @param ignoreCase true if case should be ignored
     * @return true, if the source starts with the prefix string.
     */
    private static boolean startsWith(final String source, final String prefix, final boolean ignoreCase) {
        return prefix.length() <= source.length() && source.regionMatches(ignoreCase, 0, prefix, 0, prefix.length());
    }
}
