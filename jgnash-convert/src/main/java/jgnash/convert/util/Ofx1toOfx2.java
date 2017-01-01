/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import joptsimple.OptionParser;
import joptsimple.OptionSet;

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

import static java.util.Arrays.asList;

/**
 * Commandline class to convert OFX version 1 (SGML) to OFX version 2 (XML)
 *
 * @author Craig Cavanaugh
 */
class Ofx1toOfx2 {
    private static final String HELP_OPTION_SHORT = "h";
    private static final String HELP_OPTION_LONG = "help";
    private static final String IN_OPTION = "in";
    private static final String OUT_OPTION = "out";

    public static void main(final String args[]) {
        Ofx1toOfx2 main = new Ofx1toOfx2();
        main.convert(args);
    }

    private void convert(final String args[]) {
        final OptionParser parser = buildParser();

        try {
            final OptionSet options = parser.parse(args);

            final File inFile = (File) options.valueOf(IN_OPTION);
            final File outFile = (File) options.valueOf(OUT_OPTION);

            Objects.requireNonNull(inFile, "Input file must be specified");
            Objects.requireNonNull(outFile, "Output file must be specified");

            convertToXML(inFile, outFile);
        } catch (final Exception exception) {
            if (parser != null) {
                try {
                    parser.printHelpOn(System.err);
                } catch (IOException e) {
                    Logger.getLogger(Ofx1toOfx2.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            }
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

    private static OptionParser buildParser() {
        final OptionParser parser = new OptionParser() {
            {
                acceptsAll(asList(HELP_OPTION_SHORT, HELP_OPTION_LONG), "This help").forHelp();
                accepts(IN_OPTION, "File to convert").withRequiredArg().required().ofType(File.class);
                accepts(OUT_OPTION, "File to save to").withRequiredArg().required().ofType(File.class);
            }
        };

        parser.allowsUnrecognizedOptions();

        return parser;
    }
}
