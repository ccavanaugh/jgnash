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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Parses a 2.x OFX (XML) file
 *
 * @author Craig Cavanaugh
 */
public class OfxV2Parser implements OfxTags {

    private static final Logger logger = Logger.getLogger("OfxV2Parser");

    private static final boolean debug = false;

    public static final String EXTRA_SPACE_REGEX = "\\s+";

    private OfxBank bank;

    public OfxV2Parser() {
        if (debug) {
            try {
                Handler fh = new FileHandler("%h/jgnash-ofx%g.log");
                fh.setFormatter(new SimpleFormatter());
                logger.addHandler(fh);
                logger.setLevel(Level.FINEST);
            } catch (IOException ioe) {
                logger.severe("Could not install file handler");
            }
        }
    }

    /**
     * Parses an InputStream and assumes UTF-8 encoding
     *
     * @param stream InputStream to parse
     */
    public void parse(final InputStream stream) {
        parse(stream, "UTF-8");
    }

    /**
     * Parses an InputStream using a specified encoding
     *
     * @param stream InputStream to parse
     * @param encoding encoding to use
     */
    public void parse(final InputStream stream, final String encoding) {
        logger.entering(OfxV2Parser.class.getName(), "parse");

        bank = new OfxBank();

        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                
        try (InputStream input = new BufferedInputStream(stream)) {
            XMLStreamReader reader = inputFactory.createXMLStreamReader(input, encoding);
            readOfx(reader);            
        } catch (IOException | XMLStreamException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }                             

        logger.exiting(OfxV2Parser.class.getName(), "parse");
    }

    public void parse(final File file) throws FileNotFoundException {
        
        try (InputStream stream = new FileInputStream(file)) {
            parse(stream);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }                
    }

    public void parse(final String string, final String encoding) {
        parse(new ByteArrayInputStream(string.getBytes()), encoding);
    }

    public void parse(final String string) {
        parse(new ByteArrayInputStream(string.getBytes()));
    }

    @SuppressWarnings("fallthrough")
    private void readOfx(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "readOfx");

        while (reader.hasNext()) {

            int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case SIGNONMSGSRSV1:
                            parseSignonMessageSet(reader);
                            break;
                        case BANKMSGSRSV1:
                            parseBankMessageSet(reader);
                            break;
                        case CREDITCARDMSGSRSV1:
                            parseCreditCardMessageSet(reader);
                            break;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "readOfx");
    }

    /**
     * Parses a BANKMSGSRSV1 element
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    @SuppressWarnings("fallthrough")
    private void parseBankMessageSet(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseBankMessageSet");

        QName parsingElement = reader.getName();

        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case CURDEF:
                            bank.currency = reader.getElementText();
                            break;
                        case LEDGERBAL:
                            parseLedgerBalance(reader);
                            break;
                        case AVAILBAL:
                            parseAvailableBalance(reader);
                            break;
                        case BANKACCTFROM:
                            parseAccountInfo(reader);
                            break;
                        case BANKTRANLIST:
                            parseBankTransactionList(reader);
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.info("Found the end of the bank message set aggregate");
                        break;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseBankMessageSet");
    }

    /**
     * Parses a CREDITCARDMSGSRSV1 element
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    @SuppressWarnings("fallthrough")
    private void parseCreditCardMessageSet(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseCreditCardMessageSet");

        QName parsingElement = reader.getName();

        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case CURDEF:
                            bank.currency = reader.getElementText();
                            break;
                        case LEDGERBAL:
                            parseLedgerBalance(reader);
                            break;
                        case AVAILBAL:
                            parseAvailableBalance(reader);
                            break;
                        case CCACCTFROM:
                            parseAccountInfo(reader);
                            break;
                        case BANKTRANLIST:
                            parseBankTransactionList(reader);
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.info("Found the end of the credit card message set aggregate");
                        break;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseCreditCardMessageSet");
    }

    /**
     * Parses a BANKTRANLIST element
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    @SuppressWarnings("fallthrough")
    private void parseBankTransactionList(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseBankTransactionList");

        QName parsingElement = reader.getName();

        parse:
        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case DTSTART:
                            bank.dateStart = parseDate(reader.getElementText());
                            break;
                        case DTEND:
                            bank.dateEnd = parseDate(reader.getElementText());
                            break;
                        case STMTTRN:
                            parseBankTransaction(reader);
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.info("Found the end of the bank transaction list");
                        break parse;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseBankTransactionList");
    }

    /**
     * Parses a STMTTRN element
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    @SuppressWarnings("fallthrough")
    private void parseBankTransaction(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseBankTransaction");

        QName parsingElement = reader.getName();

        OfxTransaction tran = new OfxTransaction();

        parse:
        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case TRNTYPE:
                            tran.transactionType = reader.getElementText();
                            break;
                        case DTPOSTED:
                            tran.datePosted = parseDate(reader.getElementText());
                            break;
                        case DTUSER:
                            tran.dateUser = parseDate(reader.getElementText());
                            break;
                        case TRNAMT:
                            tran.amount = parseAmount(reader.getElementText());
                            break;
                        case FITID:
                            tran.transactionID = reader.getElementText();
                            break;
                        case CHECKNUM:
                            tran.checkNumber = reader.getElementText();
                            break;
                        case NAME:
                            tran.payee = reader.getElementText().replaceAll(EXTRA_SPACE_REGEX, " ").trim();
                            break;
                        case MEMO:
                            tran.memo = reader.getElementText().replaceAll(EXTRA_SPACE_REGEX, " ").trim();
                            break;
                        case SIC:
                            tran.sic = reader.getElementText();
                            break;
                        case REFNUM:
                            tran.refNum = reader.getElementText();
                            break;
                        case PAYEEID:
                            tran.payeeId = reader.getElementText().replaceAll(EXTRA_SPACE_REGEX, " ").trim();
                            break;
                        case CURRENCY:
                            tran.currency = reader.getElementText();
                            break;
                        case ORIGCURRENCY:
                            tran.currency = reader.getElementText();
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.info("Found the end of the banktransaction");
                        break parse;
                    }
                default:
            }
        }

        bank.addTransaction(tran);

        logger.exiting(OfxV2Parser.class.getName(), "parseBankTransaction");
    }

    /**
     * Parses a BANKACCTFROM element
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    @SuppressWarnings("fallthrough")
    private void parseAccountInfo(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseAccountInfo");

        QName parsingElement = reader.getName();

        parse:
        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case BANKID:
                            bank.bankId = reader.getElementText();
                            break;
                        case ACCTID:
                            bank.accountId = reader.getElementText();
                            break;
                        case ACCTTYPE:
                            bank.accountType = reader.getElementText();
                            break;
                        case BRANCHID:
                            bank.branchId = reader.getElementText();
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.info("Found the end of the bank and account info aggregate");
                        break parse;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseAccountInfo");
    }

    /**
     * Parses a LEDGERBAL element
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    @SuppressWarnings("fallthrough")
    private void parseLedgerBalance(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseLedgerBalance");

        QName parsingElement = reader.getName();

        parse:
        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case BALAMT:
                            bank.ledgerBalance = parseAmount(reader.getElementText());
                            break;
                        case DTASOF:
                            bank.ledgerBalanceDate = parseDate(reader.getElementText());
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.info("Found the end of the ledger balance aggregate");
                        break parse;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseLedgerBalance");
    }

    /**
     * Parses a AVAILBAL element
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    @SuppressWarnings("fallthrough")
    private void parseAvailableBalance(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseAvailableBalance");

        QName parsingElement = reader.getName();

        parse:
        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case BALAMT:
                            bank.ledgerBalance = parseAmount(reader.getElementText());
                            break;
                        case DTASOF:
                            bank.ledgerBalanceDate = parseDate(reader.getElementText());
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.info("Found the end of the ledger balance aggregate");
                        break parse;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseAvailableBalance");
    }

    /**
     * Parses a SIGNONMSGSRSV1 element
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    @SuppressWarnings("fallthrough")
    private static void parseSignonMessageSet(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseSignonMessageSet");

        QName parsingElement = reader.getName();

        parse:
        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.info("Found the end of the sign-on message set aggregate");
                        break parse;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseSignonMessageSet");
    }

    public OfxBank getBank() {
        return bank;
    }

    /**
     * Parse a date. Time zone and seconds are ignored
     * <p/>
     * YYYYMMDDHHMMSS.XXX [gmt offset:tz name]
     *
     * @param date String form of the date
     * @return parsed date
     */
    private static Date parseDate(final String date) {
        int year = Integer.parseInt(date.substring(0, 4)); // year
        int month = Integer.parseInt(date.substring(4, 6)); // month
        int day = Integer.parseInt(date.substring(6, 8)); // day

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day, 0, 0, 0);

        return calendar.getTime();
    }

    private static BigDecimal parseAmount(final String amount) {

        /* Must trim the amount for a clean parse
         * Some banks leave extra spaces before the value
         */

        try {
            return new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            logger.log(Level.INFO, "Parse amount was: {0}", amount);
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            return BigDecimal.ZERO;
        }
    }
}
