/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
package jgnash.engine.search;

import java.util.regex.Pattern;

import jgnash.util.SearchUtils;

/**
 * Transaction payee matcher
 *
 * @author Craig Cavanaugh
 *
 */
abstract class AbstractStringMatcher implements Matcher {

    Pattern p;

    /**
     * Creates a Matcher for Strings
     *
     * @param pattern       DOS style wildcard search pattern
     * @param caseSensitive should be true if match should be case sensitive
     */
    AbstractStringMatcher(final String pattern, final boolean caseSensitive) {
        if (pattern == null) {
            throw new RuntimeException();
        }

        p = SearchUtils.createSearchPattern(pattern, caseSensitive);
    }
}