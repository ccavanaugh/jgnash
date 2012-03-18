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
package jgnash.util;

import java.awt.*;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * EncodeDecode is used to encode/decode various objects using a String
 *
 * @author Craig Cavanaugh
 *
 */

public class EncodeDecode {

    private static final Pattern COMMA_DELIMITER_PATTERN = Pattern.compile(",");

    private static final Pattern LOCALE_DELIMITER_PATTERN = Pattern.compile("\\x2E");

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
        StringBuilder buf = new StringBuilder();
        buf.append(bounds.x);
        buf.append(',');
        buf.append(bounds.y);
        buf.append(',');
        buf.append(bounds.width);
        buf.append(',');
        buf.append(bounds.height);
        return buf.toString();
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
            } catch (NumberFormatException nfe) {
                rectangle = null;
            }
        }
        return rectangle;
    }

    public static String encodeLocale(final Locale locale) {
        StringBuilder buf = new StringBuilder();
        buf.append(locale.getLanguage());
        if (!locale.getCountry().equals("")) {
            buf.append('.');
            buf.append(locale.getCountry());
            if (!locale.getVariant().equals("")) {
                buf.append('.');
                buf.append(locale.getVariant());
            }
        }
        return buf.toString();
    }

    public static Locale decodeLocale(final String locale) {
        if (locale == null || locale.equals("") || locale.equals("null")) {
            return Locale.getDefault();
        } else if (locale.indexOf('.') == -1) {
            return new Locale(locale);
        } else {
            String[] array = LOCALE_DELIMITER_PATTERN.split(locale);
            if (array.length == 3) {
                return new Locale(array[0], array[1], array[2]);
            } else if (array.length == 2) {
                return new Locale(array[0], array[1]);
            } else { // should not happen
                return Locale.getDefault();
            }
        }
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
     * @return the boolean array, null if zero length or array was null
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
        return null;
    }
}
