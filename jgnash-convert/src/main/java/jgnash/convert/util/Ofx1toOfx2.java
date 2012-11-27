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
package jgnash.convert.util;

import jgnash.convert.imports.ofx.OfxV1ToV2;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Commandline class to convert OFX version 1 (SGML) to OFX version 2 (XML)
 *
 * @author Craig Cavanaugh
 */
public class Ofx1toOfx2 {

    @Option(name = "-in", usage = "File to convert")
    private File inFile = null;

    @Option(name = "-out", usage = "File to save to")
    private File outFile = null;

    public static void main(final String args[]) {
        Ofx1toOfx2 main = new Ofx1toOfx2();
        main.convert(args);
    }

    private void convert(final String args[]) {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);

            if (inFile != null && outFile != null) {
                convertToXML(inFile, outFile);
            }
        } catch (CmdLineException e) {
            Logger.getLogger(Ofx1toOfx2.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private static void convertToXML(final File inFile, final File outFile) {

        try (FileWriter writer = new FileWriter(outFile)) {
            String xmlData = OfxV1ToV2.convertToXML(inFile);

            writer.write(xmlData);
        } catch (IOException e) {
            Logger.getLogger(Ofx1toOfx2.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
