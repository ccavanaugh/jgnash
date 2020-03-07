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
package jgnash.resource.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is for returning URL's to localized text files.
 *
 * @author Craig Cavanaugh
 */
public class TextResource {

    private static final String ROOT_PATH = "/jgnash/resource/text";

    /**
     * Contains only static methods
     */
    private TextResource() {
    }

    /**
     * Find a locale specific text file given the file name. Multiple lines of text are preserved.
     *
     * @param fileName the file name of the text file to look for.
     * @return a String containing the contents of the file.
     */
    public static String getString(final String fileName) {

        final String root = ClassPathUtils.getLocalizedPath(ROOT_PATH);
        final StringBuilder sb = new StringBuilder();

        try (final InputStream s = TextResource.class.getResourceAsStream(root + "/" + fileName)) {
            if (s != null) {
                try (final BufferedReader b = new BufferedReader(new InputStreamReader(s, StandardCharsets.UTF_8))) {
                    String t = loadConvert(b.readLine());
                    while (t != null) {
                        sb.append(t);
                        t = loadConvert(b.readLine());
                        if (t != null) {
                            sb.append('\n');
                        }
                    }
                }
            }
        } catch (final IOException e) {
            Logger.getLogger(TextResource.class.getName()).log(Level.SEVERE, null, e);
        }

        return sb.toString();
    }

    /**
     * Converts encoded &#92;uxxxx to unicode chars and changes special saved chars to their original forms
     * <p>
     * This was copied from the JDK source code; Properties.java
     *
     * @param string unicode formatted string
     * @return string without unicode characters
     */
    private static String loadConvert(final String string) {

        if (string == null) {
            return null;
        }

        char aChar;

        final int len = string.length();

        final StringBuilder outBuffer = new StringBuilder(len);

        for (int x = 0; x < len; ) {
            aChar = string.charAt(x++);
            if (aChar == '\\') {
                aChar = string.charAt(x++);
                if (aChar == 'u') {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = string.charAt(x++);
                        switch (aChar) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
                        }
                    }
                    outBuffer.append((char) value);
                } else {
                    switch (aChar) {
                        case 't':
                            aChar = '\t';
                            break;
                        case 'r':
                            aChar = '\r';
                            break;
                        case 'n':
                            aChar = '\n';
                            break;
                        case 'f':
                            aChar = '\f';
                            break;
                    }
                    outBuffer.append(aChar);
                }
            } else {
                outBuffer.append(aChar);
            }
        }
        return outBuffer.toString();
    }
}
