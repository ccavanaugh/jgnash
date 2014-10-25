/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.convert.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.convert.imports.ofx.OfxV1ToV2;
import jgnash.util.NotNull;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Commandline class to convert OFX version 1 (SGML) to OFX version 2 (XML)
 *
 * @author Craig Cavanaugh
 */
class Ofx1toOfx2 {

    @Option(name = "-in", usage = "File to convert")
    private final File inFile = null;

    @Option(name = "-out", usage = "File to save to")
    private final File outFile = null;

    public static void main(final String args[]) {
        Ofx1toOfx2 main = new Ofx1toOfx2();
        main.convert(args);
    }

    private void convert(final String args[]) {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);

            Objects.requireNonNull(inFile, "Input file must be specified");
            Objects.requireNonNull(outFile, "Output file must be specified");

            convertToXML(inFile, outFile);
        } catch (NullPointerException | CmdLineException e) {
            Logger.getLogger(Ofx1toOfx2.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private static void convertToXML(@NotNull final File inFile, @NotNull final File outFile) {

        try (final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8)) {
            String xmlData = OfxV1ToV2.convertToXML(inFile);

            writer.write(xmlData);
        } catch (IOException e) {
            Logger.getLogger(Ofx1toOfx2.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
