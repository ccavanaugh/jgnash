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
package jgnash.exports.ofx;

import jgnash.engine.Account;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Primary class for OFX export.  The SGML format is used instead of the newer XML
 * to offer the best compatability with older importers
 *
 * @author Craig Cavanaugh
 */
public class OfxExport {

    private static final String[] OFXHEADER = new String[]{"OFXHEADER:100", "DATA:OFXSGML", "VERSION:102", "SECURITY:NONE",
            "ENCODING:USASCII", "CHARSET:1252", "COMPRESSION:NONE", "OLDFILEUID:NONE", "NEWFILEUID:NONE"};

    public static void exportAccount(final Account account, final Date startDate, final Date endDate, final File file) {

        if (account == null || startDate == null || endDate == null || file == null) {
            throw new RuntimeException();
        }

        try (IndentedPrintWriter writer = new IndentedPrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.forName("windows-1252"))))) {

            // write the required header
            for (String line : OFXHEADER) {
                writer.println(line);
            }

            // start of data
            writer.println("<OFX>");

            // write data


            // finished
            writer.println("</OFX>");
        } catch (IOException e) {
            Logger.getLogger(OfxExport.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

    }


    /**
     * Support class to make writing indented SGML easier
     */
    private static class IndentedPrintWriter extends PrintWriter {

        public IndentedPrintWriter(final Writer out) {
            super(out);
        }


        public void println(int indentDepth, String x) {
            write("  "); // fix
            println(x);
        }
    }
}
