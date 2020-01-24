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
package jgnash.util;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for working with Collections.
 * 
 * @author Craig Cavanaugh
 */
public class CollectionUtils {
    
    private CollectionUtils() {}

    /**
     * Sorts a map by it's value.
     *
     * @param map Map to sort
     * @param <K> key
     * @param <V> value
     * @return sorted Map
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(final Map<K, V> map) {
        final List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());

        list.sort(Map.Entry.comparingByValue());

        final Map<K, V> sortedMap = new LinkedHashMap<>();

        for (final Map.Entry<K, V> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
}
