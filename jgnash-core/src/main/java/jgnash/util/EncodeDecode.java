/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * EncodeDecode is used to encode/decode various objects using a String
 *
 * @author Craig Cavanaugh
 */
public class EncodeDecode {

    private static final Pattern COMMA_DELIMITER_PATTERN = Pattern.compile(",");

    private static final char COMMA_DELIMITER = ',';

    private EncodeDecode() {
    }

    /*public static String encodeDimension(Dimension d) {
        StringBuilder buf = new StringBuilder();
        buf.append(d.width);
        buf.append(',');
        buf.append(d.height);
        return buf.toString();
    }

    public static Dimension decodeDimension(String d) {
        if (d == null) {
            return null;
        }

        Dimension rect = null;
        String[] array = d.split(",");
        if (array.length == 2) {
            try {
                rect = new Dimension();
                rect.width = Integer.parseInt(array[0]);
                rect.height = Integer.parseInt(array[1]);
            } catch (NumberFormatException nfe) {
                rect = null;
            }
        }
        return rect;
    }*/

    public static String encodeRectangle(final Rectangle bounds) {
        return String.valueOf(bounds.x) + COMMA_DELIMITER + bounds.y + COMMA_DELIMITER + bounds.width
                + COMMA_DELIMITER + bounds.height;
    }

    public static String encodeRectangle2D(final double x, final double y, final double width, final double height) {
        return String.valueOf(x) + COMMA_DELIMITER + y + COMMA_DELIMITER + width + COMMA_DELIMITER + height;
    }

    public static Rectangle decodeRectangle(final String bounds) {
        if (bounds == null) {
            return null;
        }

        Rectangle rectangle = null;
        String[] array = COMMA_DELIMITER_PATTERN.split(bounds);
        if (array.length == 4) {
            try {
                rectangle = new Rectangle();
                rectangle.x = Integer.parseInt(array[0]);
                rectangle.y = Integer.parseInt(array[1]);
                rectangle.width = Integer.parseInt(array[2]);
                rectangle.height = Integer.parseInt(array[3]);
            } catch (final NumberFormatException nfe) {
                Logger.getLogger(EncodeDecode.class.getName()).log(Level.SEVERE, null, nfe);
                rectangle = null;
            }
        }
        return rectangle;
    }

    public static Rectangle2D.Double decodeRectangle2D(final String bounds) {
        if (bounds == null) {
            return null;
        }

        Rectangle2D.Double rectangle = null;
        String[] array = COMMA_DELIMITER_PATTERN.split(bounds);
        if (array.length == 4) {
            try {
                rectangle = new Rectangle2D.Double();
                rectangle.x = Float.parseFloat(array[0]);
                rectangle.y = Float.parseFloat(array[1]);
                rectangle.width = Float.parseFloat(array[2]);
                rectangle.height = Float.parseFloat(array[3]);
            } catch (final NumberFormatException nfe) {
                Logger.getLogger(EncodeDecode.class.getName()).log(Level.SEVERE, null, nfe);
                rectangle = null;
            }
        }
        return rectangle;
    }

    /**
     * Encodes a double array as a comma separated {@code String}. Values will be rounded to 2 decimal places
     *
     * @param doubleArray array of doubles to encode
     * @return resultant {@code String}
     */
    public static String encodeDoubleArray(final double[] doubleArray) {
        final DecimalFormat df = new DecimalFormat("#.##");

        final StringBuilder result = new StringBuilder();

        for (final double value : doubleArray) {
            result.append(df.format(value));
            result.append(COMMA_DELIMITER);
        }

        return result.length() > 0 ? result.substring(0, result.length() - 1) : null;
    }

    /**
     * Decodes a comma separated list of {@code double} into a primitive {@code double} array
     *
     * @param string Comma separated string
     * @return primitive {@code double} array
     */
    public static double[] decodeDoubleArray(@NotNull final String string) {
        String[] array = COMMA_DELIMITER_PATTERN.split(string);

        double[] doubles = new double[array.length];

        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = Double.parseDouble(array[i]);
        }

        return doubles;
    }

    /**
     * Encodes a boolean array as a string of 1's and 0's
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
     * Turns a string of "10101" into a boolean array
     *
     * @param array array to decode
     * @return the boolean array, zero length if string is null or zero length
     */
    public static boolean[] decodeBooleanArray(final String array) {
        if (array != null) {
            int len = array.length();
            if (len > 0) {
                boolean b[] = new boolean[len];
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

        // precondition check for the delimiter existence
        for (String string : list) {
            if (string.indexOf(COMMA_DELIMITER) > -1) {
                throw new RuntimeException("The list of strings may not contain a " + COMMA_DELIMITER);
            }
        }

        StringBuilder result = new StringBuilder();

        for (String string : list) {
            result.append(string);
            result.append(COMMA_DELIMITER);
        }

        return result.length() > 0 ? result.substring(0, result.length() - 1) : null;
    }

    public static Collection<String> decodeStringCollection(final String string) {
        if (string == null || string.isEmpty()) {
            return Collections.emptyList();
        }

        return java.util.Arrays.asList(COMMA_DELIMITER_PATTERN.split(string));
    }

}
