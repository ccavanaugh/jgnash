/*
 * Copyright (C) 2008 Arnout Engelen
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
package net.bzzt.swift.mt940;

import net.bzzt.swift.mt940.exporter.Mt940Exporter;
import net.bzzt.swift.mt940.parser.Mt940Parser;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.ParseException;

import jgnash.convert.imports.ImportBank;

/**
 * Tests for SWIFT Mt940 importing
 * 
 * @author arnouten
 */
public class Mt940Test {

    /**
     * Rudimentary test for mt940 importing: creates a parser, parses a given
     * mt940 file and checks that indeed, 18 transactions have been generated.
     * 
     * Then, converts the parsed file to an ImportBank and verifies that there's
     * still 18 transactions.
     * 
     * @throws Exception
     *             throws assert exception
     */
    @Test
    public void testMt940() throws Exception {
        int nTransactions = 18;

        Mt940Parser parser = new Mt940Parser();

        InputStream inputStream = this.getClass().getResourceAsStream("/bank1.STA");

        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(inputStream))) {
            Mt940File file = parser.parse(reader);

            assertEquals(nTransactions, file.getEntries().size());

            ImportBank bank = Mt940Exporter.convert(file);
            assertEquals(nTransactions, bank.getTransactions().size());
        }
    }

    /**
     * Test parsing an (anonimized) mt940 file as produced by the Rabobank
     * online bank
     * 
     * @throws ParseException
     * @throws IOException
     */
    @Test
    public void testMt940Rabobank() throws IOException, ParseException {
        int nTransactions = 6;

        Mt940Parser parser = new Mt940Parser();

        InputStream inputStream = this.getClass().getResourceAsStream("/rabobank.swi");        
        
        try(LineNumberReader reader = new LineNumberReader(new InputStreamReader(inputStream))) {
            Mt940File file = parser.parse(reader);
            assertEquals(nTransactions, file.getEntries().size());

            ImportBank bank = Mt940Exporter.convert(file);
            assertEquals(nTransactions, bank.getTransactions().size());            
        }        
    }
}
