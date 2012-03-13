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
package jgnash.imports.ofx;

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
 * @version $Id: Ofx2Test.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class Ofx2Test {

    OfxV2Parser parser;

    @Before
    public void setUp() throws Exception {
        parser = new OfxV2Parser();
    }

    @Test
    public void parseSpec201() {

        InputStream stream = null;
        try {
            stream = Object.class.getResourceAsStream("/ofx_spec201_stmtrs_example.xml");

            parser.parse(stream);

            Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, ex);
                assertTrue(false);
            }
        }

        assertTrue(true);
    }

    @Test
    public void parseActivity() {

        InputStream stream = null;
        try {
            stream = Object.class.getResourceAsStream("/activity.ofx");

            parser.parse(OfxV1ToV2.convertToXML(stream));

            Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, ex);
                assertTrue(false);
            }
        }
        assertTrue(true);
    }

    @Test
    public void parseBankOne() {

        InputStream stream = null;
        try {
            stream = Object.class.getResourceAsStream("/bank1.ofx");

            parser.parse(OfxV1ToV2.convertToXML(stream));

            Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, ex);
                assertTrue(false);
            }
        }
        assertTrue(true);
    }

    @Test
    public void parseCheckingOne() {

        InputStream stream = null;
        try {
            stream = Object.class.getResourceAsStream("/checking1.ofx");

            parser.parse(OfxV1ToV2.convertToXML(stream));

            Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, ex);
                assertTrue(false);
            }
        }
        assertTrue(true);
    }

    @Test
    public void parseChequing() {

        InputStream stream = null;
        try {
            stream = Object.class.getResourceAsStream("/chequing.ofx");

            parser.parse(OfxV1ToV2.convertToXML(stream));

            Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, ex);
                assertTrue(false);
            }
        }
        assertTrue(true);
    }

    @Test
    public void parseComptes() {

        InputStream stream = null;
        try {
            stream = Object.class.getResourceAsStream("/comptes.ofx");

            parser.parse(OfxV1ToV2.convertToXML(stream));

            Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, ex);
                assertTrue(false);
            }
        }
        assertTrue(true);
    }

    @Test
    public void parseDemobank() {

        InputStream stream = null;
        try {
            stream = Object.class.getResourceAsStream("/demobank.ofx");
            parser.parse(OfxV1ToV2.convertToXML(stream), "ISO-8859-1");

            Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, ex);
                assertTrue(false);
            }
        }
        assertTrue(true);
    }

    @Test
    public void parseSample() {
        final String testFile = "/Sample.ofx";

        InputStream stream = null;
        try {
            URL url = Object.class.getResource(testFile);
            String encoding = FileMagic.getOfxV1Encoding(new File(url.toURI()));

            stream = Object.class.getResourceAsStream(testFile);
            parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

            Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
        } catch (URISyntaxException ex) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, ex);
                assertTrue(false);
            }
        }
        assertTrue(true);
    }

    @Test
    public void parseFileWithAccents() {
        final String testFile = "/File_with_Accents.ofx";

        InputStream stream = null;
        try {
            URL url = Object.class.getResource(testFile);
            String encoding = FileMagic.getOfxV1Encoding(new File(url.toURI()));

            stream = Object.class.getResourceAsStream(testFile);
            parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

            Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
        } catch (URISyntaxException ex) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, ex);
                assertTrue(false);
            }
        }
        assertTrue(true);
    }

}
