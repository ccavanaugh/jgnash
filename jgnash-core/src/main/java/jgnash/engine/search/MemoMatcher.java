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
package jgnash.engine.search;

import jgnash.engine.Transaction;

/**
 * Transaction memo matcher
 *
 * @author Craig Cavanaugh
 *
 */
public class MemoMatcher extends AbstractStringMatcher {

    /**
     * Creates a Matcher for Transaction memos
     *
     * @param pattern       DOS style wildcard search pattern
     * @param caseSensitive should be true if match should be case sensitive
     */
    public MemoMatcher(final String pattern, final boolean caseSensitive) {
        super(pattern, caseSensitive);
    }

    @Override
    public boolean matches(final Transaction t) {
        java.util.regex.Matcher m = p.matcher(t.getMemo());

        return m.matches();
    }
}
