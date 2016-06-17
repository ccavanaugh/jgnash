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
package jgnash.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jgnash.util.SearchUtils;

/**
 * Search Engine class.
 *
 * @author Craig Cavanaugh
 */
class SearchEngine {

    private SearchEngine() {
    }

    /**
     * Searches transaction memos.
     *
     * @param pattern       Search pattern, may include wild cards
     * @param transactions  Collection of transactions to search through
     * @param caseSensitive true if search should be case sensitive
     * @return List of transactions that matched the search pattern
     */
    public static List<Transaction> matchMemo(final String pattern, final Collection<Transaction> transactions, final boolean caseSensitive) {
        List<Transaction> match = new ArrayList<>();

        Pattern p = SearchUtils.createSearchPattern(pattern, caseSensitive);

        for (Transaction t : transactions) {
            Matcher m = p.matcher(t.getMemo());

            if (m.matches()) {
                match.add(t);
            }
        }

        return match;
    }

    /**
     * Searches transaction payees.
     *
     * @param pattern       Search pattern, may include wild cards
     * @param transactions  Collection of transactions to search through
     * @param caseSensitive true if search should be case sensitive
     * @return List of transactions that matched the search pattern
     */
    public static List<Transaction> matchPayee(final String pattern, final Collection<Transaction> transactions, final boolean caseSensitive) {
        List<Transaction> match = new ArrayList<>();

        Pattern p = SearchUtils.createSearchPattern(pattern, caseSensitive);

        for (Transaction t : transactions) {
            Matcher m = p.matcher(t.getPayee());

            if (m.matches()) {
                match.add(t);
            }
        }

        return match;
    }

}
