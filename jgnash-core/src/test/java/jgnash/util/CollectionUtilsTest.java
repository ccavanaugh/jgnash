/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Craig Cavanaugh
 */
public class CollectionUtilsTest {

    @Test
    public void partitionOdd() throws Exception {
        final List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);

        final List<List<Integer>> partitions = CollectionUtils.partition(numbers, 2);

        assertEquals(7, partitions.size());
        assertEquals(1, partitions.get(6).size());
    }

    @Test
    public void partitionEven() throws Exception {
        final List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);

        final List<List<Integer>> partitions = CollectionUtils.partition(numbers, 2);

        assertEquals(7, partitions.size());
        assertEquals(2, partitions.get(6).size());
    }
}