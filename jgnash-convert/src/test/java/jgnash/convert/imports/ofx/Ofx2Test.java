/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
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

            assertEquals("ENG", parser.getLanguage());
            assertEquals("INFO", parser.getStatusSeverity());
            assertEquals(0, parser.getStatusCode());

            final OfxBank bank = parser.getBank();
            assertNotNull(bank);

            assertEquals(0, bank.statusCode);
            assertEquals("INFO", bank.statusSeverity);

            Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());            
        } catch (IOException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            fail();
        }
    }

    @Test
    public void parseActivity() {
        final String testFile = "/activity.ofx";

        final URL url = Object.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(new File(url.toURI()));

            try (InputStream stream = Object.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                assertEquals("ENG", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                final OfxBank bank = parser.getBank();
                assertNotNull(bank);

                assertEquals(0, bank.statusCode);
                assertEquals("INFO", bank.statusSeverity);
                assertEquals("Success", bank.statusMessage);

                Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
            } catch (IOException e) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
                fail();
            }
        } catch (URISyntaxException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            fail();
        }
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

                assertEquals("ENG", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, parser.getBank().statusCode);
                assertEquals("INFO", parser.getBank().statusSeverity);
                assertEquals(null, parser.getBank().statusMessage);

                Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
            } catch (IOException e) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
                fail();
            }
        } catch (URISyntaxException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            fail();
        }
    }


    /**
     * Test for amounts that use a comma as a decimal separator
     */
    @Test
    public void parseBankOneCommas() {
        final String testFile = "/bank1-commas.ofx";

        URL url = Object.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(new File(url.toURI()));

            try (InputStream stream = Object.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                assertEquals("ENG", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, parser.getBank().statusCode);
                assertEquals("INFO", parser.getBank().statusSeverity);
                assertEquals(null, parser.getBank().statusMessage);

                assertEquals(new BigDecimal("524.10"), parser.getBank().ledgerBalance);
                assertEquals(new BigDecimal("519.10"), parser.getBank().availBalance);

                List<OfxTransaction> transactions = parser.getBank().getTransactions();

                assertEquals(new BigDecimal("-130.00"), transactions.get(0).getAmount());
                assertEquals(new BigDecimal("-120.00"), transactions.get(1).getAmount());
                assertEquals(new BigDecimal("300.01"), transactions.get(2).getAmount());
                assertEquals(new BigDecimal("160.50"), transactions.get(3).getAmount());

                Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
            } catch (IOException e) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
                fail();
            }
        } catch (URISyntaxException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            fail();
        }
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

                assertEquals("ENG", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, parser.getBank().statusCode);
                assertEquals("INFO", parser.getBank().statusSeverity);
                assertEquals(null, parser.getBank().statusMessage);

                Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
            } catch (IOException e) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
                fail();
            }
        } catch (URISyntaxException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            fail();
        }
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

                assertEquals("ENG", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, parser.getBank().statusCode);
                assertEquals("INFO", parser.getBank().statusSeverity);
                assertEquals("OK", parser.getBank().statusMessage);

                Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
            } catch (IOException e) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
                fail();
            }
        } catch (URISyntaxException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
           fail();
        }
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

                assertEquals("FRA", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, parser.getBank().statusCode);
                assertEquals("INFO", parser.getBank().statusSeverity);
                assertEquals(null, parser.getBank().statusMessage);

                Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
            } catch (IOException e) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
                fail();
            }
        } catch (URISyntaxException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            fail();
        }
    }

    @Test
    public void parseDemobank() {

        final String testFile = "/demobank.ofx";

        try (InputStream stream = Object.class.getResourceAsStream(testFile)) {
            parser.parse(OfxV1ToV2.convertToXML(stream), "ISO-8859-1");

            assertEquals("JPN", parser.getLanguage());
            assertEquals("INFO", parser.getStatusSeverity());
            assertEquals(0, parser.getStatusCode());

            assertEquals(0, parser.getBank().statusCode);
            assertEquals("INFO", parser.getBank().statusSeverity);
            assertEquals(null, parser.getBank().statusMessage);

            Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
        } catch (IOException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            fail();
        }
    }

    @Test
    public void parseSample() {
        final String testFile = "/Sample.ofx";

        URL url = Object.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(new File(url.toURI()));

            assertTrue(FileMagic.isOfxV1(new File(url.toURI())));

            try (InputStream stream = Object.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                assertEquals("JPN", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, parser.getBank().statusCode);
                assertEquals("INFO", parser.getBank().statusSeverity);
                assertEquals(null, parser.getBank().statusMessage);

                Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
            } catch (IOException e) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
                fail();
            }
        } catch (URISyntaxException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            fail();
        }
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

                assertEquals("FRA", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, parser.getBank().statusCode);
                assertEquals("INFO", parser.getBank().statusSeverity);
                assertEquals("OK", parser.getBank().statusMessage);

                Logger.getLogger(Ofx2Test.class.getName()).log(Level.INFO, parser.getBank().toString());
            } catch (IOException e) {
                Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
                assertTrue(false);
            }
        } catch (URISyntaxException e) {
            Logger.getLogger(Ofx2Test.class.getName()).log(Level.SEVERE, null, e);
            fail();
        }

    }

}
