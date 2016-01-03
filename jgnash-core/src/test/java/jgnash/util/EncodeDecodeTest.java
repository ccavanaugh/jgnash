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
package jgnash.util;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Craig Cavanaugh
 */
public class EncodeDecodeTest {

    @Test
    public void testEncodeDecodeDoubleArrays() {
        final double[] base = new double[]{10.231, 11.35, 45.34, 2.0, 4.0, 9.0, 0};
        final String result = EncodeDecode.encodeDoubleArray(base);

        assertNotNull(result);

        assertEquals("10.23,11.35,45.34,2,4,9,0", result);

        final double[] returnResult = EncodeDecode.decodeDoubleArray(result);

        assertArrayEquals(base, returnResult, .001);
    }
}
