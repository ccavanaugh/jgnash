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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;

/**
 * EncodeDecode is used to encode/decode various objects using a String.
 *
 * @author Craig Cavanaugh
 */
public class EncodeDecode {

    public static final Pattern COMMA_DELIMITER_PATTERN = Pattern.compile(",");

    private static final char COMMA_DELIMITER = ',';

    private EncodeDecode() {
    }

    /**
     * Encodes a double array as a comma separated {@code String}. Values will be rounded to 2 decimal places.
     * <p>
     * The format is forced to use '.' as the decimal separator because some locales will use a comma.
     *
     * @param doubleArray array of doubles to encode
     * @return resultant {@code String}
     */
    public static String encodeDoubleArray(final double[] doubleArray) {
        final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);

        final DecimalFormat df = (DecimalFormat) numberFormat;
        df.applyPattern("#.##");

        return Arrays.stream(doubleArray).mapToObj(df::format).collect(joining(String.valueOf(COMMA_DELIMITER)));
    }

    /**
     * Decodes a comma separated list of {@code double} into a primitive {@code double} array.
     *
     * @param string Comma separated string
     * @return primitive {@code double} array
     */
    public static double[] decodeDoubleArray(@NotNull final String string) {
        return Arrays.stream(COMMA_DELIMITER_PATTERN.split(string)).mapToDouble(Double::parseDouble).toArray();
    }

    /**
     * Encodes a boolean array as a string of 1's and 0's.
     *
     * @param array a boolean array to encode as a String
     * @return A string of 1's and 0's representing the boolean array
     */
    public static String encodeBooleanArray(final boolean[] array) {
        StringBuilder buf = new StringBuilder();
        if (array != null) {
            for (boolean anArray : array) {
                if (anArray) {
                    buf.append('1');
                } else {
                    buf.append('0');
                }
            }
            return buf.toString();
        }
        return null;
    }

    /**
     * Turns a string of "10101" into a boolean array.
     *
     * @param array array to decode
     * @return the boolean array, zero length if string is null or zero length
     */
    public static boolean[] decodeBooleanArray(final String array) {
        if (array != null) {
            int len = array.length();
            if (len > 0) {
                boolean[] b = new boolean[len];
                for (int i = 0; i < len; i++) {
                    if (array.charAt(i) == '1') {
                        b[i] = true;
                    }
                }
                return b;
            }
        }
        return new boolean[0];
    }

    public static String encodeStringCollection(final Collection<String> list) {
        return encodeStringCollection(list, COMMA_DELIMITER);
    }

    public static String encodeStringCollection(final Collection<String> list, final char delimiter) {

        // precondition check for the delimiter existence
        for (final String string : list) {
            if (string.indexOf(delimiter) > -1) {
                throw new RuntimeException("The list of strings may not contain a " + delimiter);
            }
        }

        return list.stream().collect(joining(String.valueOf(delimiter)));
    }

    public static Collection<String> decodeStringCollection(final String string) {
        return decodeStringCollection(string, COMMA_DELIMITER);
    }

    public static Collection<String> decodeStringCollection(final String string, final char delimiter) {
        if (string == null || string.isEmpty()) {
            return Collections.emptyList();
        }

        Pattern pattern = Pattern.compile(String.valueOf(delimiter));

        return java.util.Arrays.asList(pattern.split(string));
    }

    /**
     * Converts a hex based color into an integer for compact storage
     * @param color # based hex color
     * @return int value
     */
    public static long colorStringToLong(final String color) {
        return Long.parseUnsignedLong(color.replaceAll("0x",""), 16);
    }

    /**
     * Converts a int based color into a hex format used by UI classes
     * @param color int value
     * @return hex based color string
     */
    public static String longToColorString(final long color) {
        return String.format("0x%08X", color);
    }
}
