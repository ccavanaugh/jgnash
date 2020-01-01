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
package jgnash.convert.importat.qif;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;

/**
 * An extended LineNumberReader to help ease the pain of parsing
 * a QIF file
 *
 * @author Craig Cavanaugh
 */
class QifReader extends LineNumberReader {

    private static final boolean debug = false;

    QifReader(Reader in) {
        super(in, 8192);
    }

    void mark() throws IOException {
        super.mark(256);
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        if (debug) {
            System.out.println("Reset");
        }
    }

    /**
     * Takes a peek at the next line and eats and empty line if found
     *
     * @return next readable line, null if at the end of the file
     * @throws IOException IO exception
     */
    String peekLine() throws IOException {
        String peek;
        while (true) {
            mark();
            peek = readLine();
            if (peek != null) {
                peek = peek.trim();
                reset();
                if (peek.isEmpty()) {
                    readLine(); // eat the empty line
                    if (debug) {
                        System.out.println("*EMPTY LINE*");
                    }
                } else {
                    return peek.trim();
                }
            } else {
                return null;
            }
        }
    }

    @Override
    public String readLine() throws IOException {
        while (true) {
            try {
                String line = super.readLine().trim();
                if (debug) {
                    System.out.println("Line " + getLineNumber() + ": " + line);
                }
                if (!line.isEmpty()) {
                    return line;
                }
                if (debug) {
                    System.out.println("*EMPTY LINE*");
                }
            } catch (NullPointerException e) {
                return null;
            }
        }
    }
}
