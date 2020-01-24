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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit test for Encoding and Decoding arrays of doubles.
 *
 * @author Craig Cavanaugh
 */
class EncodeDecodeTest {

    @Test
    void testEncodeDecodeDoubleArrays() {
        final double[] base = new double[]{10.231, 11.35, 45.34, 2.0, 4.0, 9.0, 0};
        final String result = EncodeDecode.encodeDoubleArray(base);

        assertNotNull(result);
        System.out.println(result);

        assertEquals("10.23,11.35,45.34,2,4,9,0", result);

        final double[] returnResult = EncodeDecode.decodeDoubleArray(result);

        assertArrayEquals(base, returnResult, .001);
    }

    @Test
    void testEncodeStringCollection() {
        List<String> items = Arrays.asList("apple", "pear", "peach", "grapes");

        assertEquals("apple,pear,peach,grapes", EncodeDecode.encodeStringCollection(items));
    }

    @Test
    void testHexDecode() {
        assertEquals("0x00000000", EncodeDecode.longToColorString(0));
        assertEquals(0, EncodeDecode.colorStringToLong("0x00000000"));

        assertEquals("0x00FFFFFF", EncodeDecode.longToColorString(16777215));
        assertEquals(16777215, EncodeDecode.colorStringToLong("0x00FFFFFF"));
    }
}
