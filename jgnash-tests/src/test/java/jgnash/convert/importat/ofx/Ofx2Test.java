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
package jgnash.convert.importat.ofx;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import jgnash.convert.importat.ImportTransaction;
import jgnash.util.FileMagic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static jgnash.util.LogUtil.logSevere;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test class
 * 
 * @author Craig Cavanaugh
 */
class Ofx2Test {

    private static OfxV2Parser parser;

    @BeforeAll
    static void setUp() {
        OfxV2Parser.enableDetailedLogFile();  // enable debugging
        parser = new OfxV2Parser();
    }

    @Test
    void parseSpec201() {
                
        try (InputStream stream =  Ofx2Test.class.getResourceAsStream("/ofx_spec201_stmtrs_example.xml")) {
            parser.parse(stream);

            assertEquals("ENG", parser.getLanguage());
            assertEquals("INFO", parser.getStatusSeverity());
            assertEquals(0, parser.getStatusCode());

            final OfxBank bank = parser.getBank();
            assertNotNull(bank);

            assertEquals(0, bank.statusCode);
            assertEquals("INFO", bank.statusSeverity);

            assertFalse(parser.getBank().isInvestmentAccount());
        } catch (IOException e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }
    }

    @Test
    void parseActivity() {
        final String testFile = "/activity.ofx";

        final URL url = Ofx2Test.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(Paths.get(url.toURI()));

            try (InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                assertEquals("ENG", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                final OfxBank bank = parser.getBank();
                assertNotNull(bank);

                assertEquals(0, bank.statusCode);
                assertEquals("INFO", bank.statusSeverity);
                assertEquals("Success", bank.statusMessage);

                assertFalse(parser.getBank().isInvestmentAccount());
            } catch (IOException e) {
                logSevere(Ofx2Test.class, e);
                fail();
            }
        } catch (URISyntaxException e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }
    }

    @Test
    void parseBankOne() {
        final String testFile = "/bank1.ofx";

        URL url = Ofx2Test.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(Paths.get(url.toURI()));

            try (InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                assertEquals("ENG", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, parser.getBank().statusCode);
                assertEquals("INFO", parser.getBank().statusSeverity);
                assertNull(parser.getBank().statusMessage);

                assertFalse(parser.getBank().isInvestmentAccount());
            } catch (IOException e) {
                logSevere(Ofx2Test.class, e);
                fail();
            }
        } catch (URISyntaxException e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }
    }

    @Test
    void parseBankTwo() {
        final String testFile = "/bank2.ofx";

        URL url = Ofx2Test.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(Paths.get(url.toURI()));

            try (InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                assertEquals("ENG", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, parser.getBank().statusCode);
                assertEquals("INFO", parser.getBank().statusSeverity);
                assertNull(parser.getBank().statusMessage);

                assertFalse(parser.getBank().isInvestmentAccount());
            } catch (IOException e) {
                logSevere(Ofx2Test.class, e);
                fail();
            }
        } catch (URISyntaxException e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }
    }


    /**
     * Test for amounts that use a comma as a decimal SEPARATOR
     */
    @Test
    void parseBankOneCommas() {
        final String testFile = "/bank1-commas.ofx";

        URL url = Ofx2Test.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(Paths.get(url.toURI()));

            try (InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                assertEquals("ENG", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, parser.getBank().statusCode);
                assertEquals("INFO", parser.getBank().statusSeverity);
                assertNull(parser.getBank().statusMessage);

                assertEquals(new BigDecimal("524.10"), parser.getBank().ledgerBalance);
                assertEquals(new BigDecimal("519.10"), parser.getBank().availBalance);

                List<ImportTransaction> transactions = parser.getBank().getTransactions();

                assertEquals(new BigDecimal("-130.00"), transactions.get(0).getAmount());
                assertEquals(new BigDecimal("-120.00"), transactions.get(1).getAmount());
                assertEquals(new BigDecimal("300.01"), transactions.get(2).getAmount());
                assertEquals(new BigDecimal("160.50"), transactions.get(3).getAmount());

                assertFalse(parser.getBank().isInvestmentAccount());
            } catch (IOException e) {
                logSevere(Ofx2Test.class, e);
                fail();
            }
        } catch (URISyntaxException e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }
    }

    @Test
    void parseCheckingOne() {
        final String testFile = "/checking1.ofx";

        URL url = Ofx2Test.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(Paths.get(url.toURI()));

            try (InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                assertEquals("ENG", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, parser.getBank().statusCode);
                assertEquals("INFO", parser.getBank().statusSeverity);
                assertNull(parser.getBank().statusMessage);

                assertFalse(parser.getBank().isInvestmentAccount());
            } catch (IOException e) {
                logSevere(Ofx2Test.class, e);
                fail();
            }
        } catch (URISyntaxException e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }
    }

    @Test
    void parseChequing() {

        final String testFile = "/chequing.ofx";

        URL url = Ofx2Test.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(Paths.get(url.toURI()));

            try (InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                assertEquals("ENG", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, parser.getBank().statusCode);
                assertEquals("INFO", parser.getBank().statusSeverity);
                assertEquals("OK", parser.getBank().statusMessage);

                assertFalse(parser.getBank().isInvestmentAccount());
            } catch (IOException e) {
                logSevere(Ofx2Test.class, e);
                fail();
            }
        } catch (URISyntaxException e) {
            logSevere(Ofx2Test.class, e);
           fail();
        }
    }

    @Test
    void parseComptes() {

        final String testFile = "/comptes.ofx";

        URL url = Ofx2Test.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(Paths.get(url.toURI()));

            try (InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                assertEquals("FRA", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, parser.getBank().statusCode);
                assertEquals("INFO", parser.getBank().statusSeverity);
                assertNull(parser.getBank().statusMessage);

                assertFalse(parser.getBank().isInvestmentAccount());
            } catch (IOException e) {
                logSevere(Ofx2Test.class, e);
                fail();
            }
        } catch (URISyntaxException e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }
    }

    @Test
    void parseDemobank() {

        final String testFile = "/demobank.ofx";

        try (InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {
            parser.parse(OfxV1ToV2.convertToXML(stream), "ISO-8859-1");

            assertEquals("JPN", parser.getLanguage());
            assertEquals("INFO", parser.getStatusSeverity());
            assertEquals(0, parser.getStatusCode());

            assertEquals(0, parser.getBank().statusCode);
            assertEquals("INFO", parser.getBank().statusSeverity);
            assertNull(parser.getBank().statusMessage);

            assertFalse(parser.getBank().isInvestmentAccount());
        } catch (IOException e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }
    }

    @Test
    void parseSample() {
        final String testFile = "/Sample.ofx";

        URL url = Ofx2Test.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(Paths.get(url.toURI()));

            assertTrue(FileMagic.isOfxV1(Paths.get(url.toURI())));

            try (InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                assertEquals("JPN", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, parser.getBank().statusCode);
                assertEquals("INFO", parser.getBank().statusSeverity);
                assertNull(parser.getBank().statusMessage);

                assertFalse(parser.getBank().isInvestmentAccount());
            } catch (IOException e) {
                logSevere(Ofx2Test.class, e);
                fail();
            }
        } catch (URISyntaxException e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }
    }

    @Test
    void parseFileWithAccents() {
        final String testFile = "/File_with_Accents.ofx";

        URL url = Ofx2Test.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(Paths.get(url.toURI()));

            try (InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {
                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                assertEquals("FRA", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, parser.getBank().statusCode);
                assertEquals("INFO", parser.getBank().statusSeverity);
                assertEquals("OK", parser.getBank().statusMessage);
            } catch (IOException e) {
                logSevere(Ofx2Test.class, e);
                fail();
            }
        } catch (URISyntaxException e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }

    }

    @Test
    void parseInvest() {
        final String testFile = "/invest.xml";

        final URL url = Ofx2Test.class.getResource(testFile);

        try {
            assertTrue(FileMagic.isOfxV2(Paths.get(url.toURI())));

            try (final InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {
                parser.parse(stream);

                OfxBank ofxBank = parser.getBank();

                assertEquals("ENG", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, ofxBank.statusCode);
                assertEquals("INFO", ofxBank.statusSeverity);
                assertNull(ofxBank.statusMessage);

                assertEquals(2, ofxBank.getTransactions().size());
                assertEquals(3, ofxBank.getSecurityList().size());

                assertTrue(ofxBank.isInvestmentAccount());
            } catch (final IOException e) {
                logSevere(Ofx2Test.class, e);
                fail();
            }
        } catch (final URISyntaxException e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }
    }

    @Test
    void parse401k() {
        final String testFile = "/401k.xml";

        final URL url = Ofx2Test.class.getResource(testFile);

        try {
            assertTrue(FileMagic.isOfxV2(Paths.get(url.toURI())));

            try (final InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {
                parser.parse(stream);

                OfxBank ofxBank = parser.getBank();

                assertEquals("ENG", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, ofxBank.statusCode);
                assertEquals("INFO", ofxBank.statusSeverity);
                assertNull(ofxBank.statusMessage);

                assertEquals(3, ofxBank.getTransactions().size());
                assertEquals(3, ofxBank.getSecurityList().size());


                assertTrue(ofxBank.isInvestmentAccount());
            } catch (final IOException e) {
                logSevere(Ofx2Test.class, e);
                fail();
            }
        } catch (final URISyntaxException e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }
    }

    @Test
    void parse401kWithHeader() {
        final String testFile = "/401k-header.xml";

        final URL url = Ofx2Test.class.getResource(testFile);

        try {
            assertTrue(FileMagic.isOfxV2(Paths.get(url.toURI())));

            try (final InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {
                parser.parse(stream);

                OfxBank ofxBank = parser.getBank();

                assertEquals("ENG", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertEquals(0, ofxBank.statusCode);
                assertEquals("INFO", ofxBank.statusSeverity);
                assertNull(ofxBank.statusMessage);

                assertEquals(3, ofxBank.getTransactions().size());
                assertEquals(3, ofxBank.getSecurityList().size());


                assertTrue(ofxBank.isInvestmentAccount());
            } catch (final IOException e) {
                logSevere(Ofx2Test.class, e);
                fail();
            }
        } catch (final URISyntaxException e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }
    }

    @Test
    void parseInvest2() {
        final String testFile = "/invest2.xml";

        final URL url = Ofx2Test.class.getResource(testFile);

        try {
            assertTrue(FileMagic.isOfxV2(Paths.get(url.toURI())));

            try (final InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {
                parser.parse(stream);

                OfxBank ofxBank = parser.getBank();

                assertEquals("ENG", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());
                assertEquals("The operation succeeded.", parser.getStatusMessage());

                assertEquals(0, ofxBank.statusCode);
                assertEquals("INFO", ofxBank.statusSeverity);
                assertNull(ofxBank.statusMessage);

                assertEquals(60, ofxBank.getTransactions().size());
                assertEquals(6, ofxBank.getSecurityList().size());

                assertTrue(ofxBank.isInvestmentAccount());
            } catch (final IOException e) {
                logSevere(Ofx2Test.class, e);
                fail();
            }
        } catch (final URISyntaxException e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }
    }

    @Test
    void parseUglyFile() throws Exception {
        final String testFile = "/uglyFormat.ofx";

        final Path path = Paths.get(Ofx2Test.class.getResource(testFile).toURI());

        assertTrue(Files.exists(path));

        assertTrue(FileMagic.isOfxV1(path));

        try (final InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {
            String result = OfxV1ToV2.convertToXML(stream);
            System.out.println(result);
        } catch (final IOException e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }

        final URL url = Ofx2Test.class.getResource(testFile);
        String encoding;

        try {
            encoding = FileMagic.getOfxV1Encoding(Paths.get(url.toURI()));

            try (final InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {

                parser.parse(OfxV1ToV2.convertToXML(stream), encoding);

                assertEquals("ENG", parser.getLanguage());
                assertEquals("INFO", parser.getStatusSeverity());
                assertEquals(0, parser.getStatusCode());

                assertFalse(parser.getBank().isInvestmentAccount());

                assertEquals(5, parser.getBank().getTransactions().size());

                // System.out.println(parser.getBank().getTransactions().get(4).getPayee());

                assertTrue(parser.getBank().getTransactions().get(4).getPayee().contains("& &"));

                assertTrue(parser.getBank().getTransactions().get(4).getPayee().contains("\""));

                assertTrue(parser.getBank().getTransactions().get(4).getPayee().contains("'"));

                assertTrue(parser.getBank().getTransactions().get(4).getPayee().contains("Am'ount"));

                assertFalse(parser.getBank().getTransactions().get(0).getPayee().contains("&"));
            } catch (IOException e) {
                logSevere(Ofx2Test.class, e);
                fail();
            }
        } catch (final URISyntaxException e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }
    }

    @Test
    void parseUglyFile2() throws Exception {
        final String testFile = "/test_fails.ofx";

        final Path path = Paths.get(Ofx2Test.class.getResource(testFile).toURI());

        assertTrue(Files.exists(path));

        assertTrue(FileMagic.isOfxV2(path));

        try {

            try (final InputStream stream = Ofx2Test.class.getResourceAsStream(testFile)) {
                parser.parse(stream);

                OfxBank ofxBank = parser.getBank();

                assertNotNull(ofxBank);

                assertEquals("ENG", parser.getLanguage());
                assertEquals(0, parser.getStatusCode());
                assertEquals("INFO", parser.getStatusSeverity());

                assertNull(ofxBank.statusMessage);

                assertEquals(2, ofxBank.getTransactions().size());

                assertFalse(ofxBank.isInvestmentAccount());
            } catch (final IOException e) {
                logSevere(Ofx2Test.class, e);
                fail();
            }
        } catch (final Exception e) {
            logSevere(Ofx2Test.class, e);
            fail();
        }
    }

}
