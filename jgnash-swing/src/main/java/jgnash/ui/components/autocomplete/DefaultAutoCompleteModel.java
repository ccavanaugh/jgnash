/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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
package jgnash.ui.components.autocomplete;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Default model for auto complete search.
 * 
 * @author Craig Cavanaugh
 * @author Don Brown
 * @author Pranay Kumar
 */
public class DefaultAutoCompleteModel implements AutoCompleteModel {

    private final List<String> list = new LinkedList<>();

    private boolean ignoreCase;

    private boolean enabled = true;

    private boolean fuzzyMatch = false;

    public void setIgnoreCase(final boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public void setFuzzyMatch(final boolean fuzzyMatch) {
        this.fuzzyMatch = fuzzyMatch;
    }

    /**
     * @see AutoCompleteModel#doLookAhead(java.lang.String)
     */
    @Override
    public String doLookAhead(final String content) {
        if (enabled) {
            return doLookAhead(content, ignoreCase);
        }
        return null;
    }

    /**
     * Perform a brute force linear search top down for the best match
     *
     * @param content string content to search for
     * @param ignoreCase search is case sensitive if set to true
     *
     * @return best match
     */
    private String doLookAhead(final String content, final boolean ignoreCase) {
        if (!content.isEmpty()) {
            synchronized (list) {
                for (String s : list) {

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

    public void addString(final String content) {
        if (content != null && !content.isEmpty()) {
            synchronized (list) {
                if (fuzzyMatch) {
                    if (list.contains(content)) {
                        list.remove(content); // remove old instance
                    }
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
     * Removes all of the strings that have been remembered
     */
    public void purge() {
        synchronized (list) {
            list.clear();
        }
    }

    @Override
    public Collection<?> getAllExtraInfo(final String key) {
        return Collections.EMPTY_LIST;
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
