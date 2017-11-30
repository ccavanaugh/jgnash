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
package jgnash.util;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.ratios.SimpleRatio;

import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Search Utility class.
 * 
 * @author Craig Cavanaugh
 */
public class SearchUtils {

    private SearchUtils() {
    }

    @NotNull
    public static String closestMatch(final String original, final List<String> stringList, final int threshold) {
        String match = original;

        if (!stringList.isEmpty()) {    // protect original against compares against an empty list

            // scores the match ratio and returns a sorted map use all available cores
            final TreeMap<Integer, String> treeMap = stringList.parallelStream()
                    .collect(Collectors.toMap(s -> FuzzySearch.weightedRatio(original, s), s -> s, (a, b) -> b,
                            TreeMap::new));

            int lastResult = treeMap.lastKey();

            if (lastResult > threshold) {

                // the best match should be the last value
                match = treeMap.lastEntry().getValue();

                System.out.println(original + ", " + match + ", " + lastResult);
            }

            // top 3 matches limited by cutoff....
            FuzzySearch.extractTop(original, stringList, new SimpleRatio(), 3, 50);
        }

        return match;
    }

    /**
     * Creates a Pattern given a DOS style wildcard search pattern.
     * 
     * @param pattern search pattern
     * @param caseSensitive true if the Pattern should be case sensitive
     * @return Pattern according to specified parameters
     */
    public static Pattern createSearchPattern(final String pattern, final boolean caseSensitive) {
        Pattern p;

        if (caseSensitive) {
            p = Pattern.compile(wildcardSearchToRegex(pattern));
        } else {
            p = Pattern.compile(wildcardSearchToRegex(pattern), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }

        return p;
    }

    private static String wildcardSearchToRegex(final String wildcard) {
        StringBuilder buffer = new StringBuilder(wildcard.length() + 2);
        buffer.append('^');

        for (char c : wildcard.toCharArray()) {

            if (c == '*') {
                buffer.append(".*");
            } else if (c == '?') {
                buffer.append('.');
            } else if ("+()^$.{}[]|\\".indexOf(c) != -1) {
                buffer.append('\\').append(c); // escape special regexp-characters
            } else {
                buffer.append(c);
            }
        }

        buffer.append('$');
        return buffer.toString();
    }
}
