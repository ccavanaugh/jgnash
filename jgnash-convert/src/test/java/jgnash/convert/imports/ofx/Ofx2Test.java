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
package jgnash.convert.imports.ofx;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.util.FileMagic;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit 4 test class
 * 
 * @author Craig Cavanaugh
 */
public class Ofx2Test {

    private OfxV2Parser parser;

    @Before
    public void setUp() throws Exception {
        parser = new OfxV2Parser();
    }

    @Test
    public void parseSpec201() {
                
        try (InputStream stream =  Object.class.getResourceAsStream("/ofx_spec201_stmtrs_example.xml")) {
            parser.parse(stream);

            Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());            
        } catch (IOException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            assertTrue(false);
        }
                
        assertTrue(true);
    }

    @Test
    public void parseActivity() {
        final String testFile = "/activity.ofx";

        URL url = Object.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(new File(url.toURI()));

            try (InputStream stream = Object.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
            } catch (IOException e) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
                assertTrue(false);
            }
        } catch (URISyntaxException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            assertTrue(false);
        }

        assertTrue(true);
    }

    @Test
    public void parseBankOne() {
        final String testFile = "/bank1.ofx";

        URL url = Object.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(new File(url.toURI()));

            try (InputStream stream = Object.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
            } catch (IOException e) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
                assertTrue(false);
            }
        } catch (URISyntaxException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            assertTrue(false);
        }

        assertTrue(true);
    }

    @Test
    public void parseCheckingOne() {
        final String testFile = "/checking1.ofx";

        URL url = Object.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(new File(url.toURI()));

            try (InputStream stream = Object.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
            } catch (IOException e) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
                assertTrue(false);
            }
        } catch (URISyntaxException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            assertTrue(false);
        }

        assertTrue(true);
    }

    @Test
    public void parseChequing() {

        final String testFile = "/chequing.ofx";

        URL url = Object.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(new File(url.toURI()));

            try (InputStream stream = Object.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
            } catch (IOException e) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
                assertTrue(false);
            }
        } catch (URISyntaxException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            assertTrue(false);
        }

        assertTrue(true);
    }

    @Test
    public void parseComptes() {

        final String testFile = "/comptes.ofx";

        URL url = Object.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(new File(url.toURI()));

            try (InputStream stream = Object.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
            } catch (IOException e) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
                assertTrue(false);
            }
        } catch (URISyntaxException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            assertTrue(false);
        }

        assertTrue(true);
    }

    @Test
    public void parseDemobank() {

        final String testFile = "/demobank.ofx";

        try (InputStream stream = Object.class.getResourceAsStream(testFile)) {
            parser.parse(OfxV1ToV2.convertToXML(stream), "ISO-8859-1");

            Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
        } catch (IOException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            assertTrue(false);
        }

        assertTrue(true);
    }

    @Test
    public void parseSample() {
        final String testFile = "/Sample.ofx";

        URL url = Object.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(new File(url.toURI()));

            try (InputStream stream = Object.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
            } catch (IOException e) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
                assertTrue(false);
            }
        } catch (URISyntaxException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            assertTrue(false);
        }

        assertTrue(true);
    }

    @Test
    public void parseFileWithAccents() {
        final String testFile = "/File_with_Accents.ofx";

        URL url = Object.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(new File(url.toURI()));

            try (InputStream stream = Object.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
            } catch (IOException e) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
                assertTrue(false);
            }
        } catch (URISyntaxException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            assertTrue(false);
        }

        assertTrue(true);
    }

}
