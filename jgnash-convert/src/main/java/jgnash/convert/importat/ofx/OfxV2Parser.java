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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jgnash.convert.common.OfxTags;
import jgnash.convert.importat.ImportSecurity;
import jgnash.convert.importat.ImportTransaction;
import jgnash.engine.TransactionType;
import jgnash.resource.util.ResourceUtils;
import jgnash.util.FileMagic;
import jgnash.util.NotNull;

import static jgnash.convert.importat.ofx.Sanitize.sanitize;

/**
 * StAX based parser for 2.x OFX (XML) files.
 * <p>
 * This parser will intentionally absorb higher level elements and drop through to simplify and reduce code.
 *
 * @author Craig Cavanaugh
 */
public class OfxV2Parser implements OfxTags {

    private static final Logger logger = Logger.getLogger("OfxV2Parser");

    private static final String EXTRA_SPACE_REGEX = "\\s+";

    private static final String ENCODING = StandardCharsets.UTF_8.name();

    private OfxBank bank;

    /**
     * Default language is assumed to be English unless the import file defines it
     */
    private String language = "ENG";

    private int statusCode;

    private String statusSeverity;

    /**
     * Status message from sign-on process
     */
    private String statusMessage;

    private final Pattern extraSpaceRegex = Pattern.compile(EXTRA_SPACE_REGEX);

    /**
     * Support class
     */
    private static class AccountInfo {
        String bankId;
        String accountId;
        String accountType;
        String branchId;
    }

    static void enableDetailedLogFile() {
        try {
            final Handler fh = new FileHandler("%t/jgnash-ofx.log", false);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setLevel(Level.ALL);
        } catch (final IOException ioe) {
            logger.severe(ResourceUtils.getString("Message.Error.LogFileHandler"));
        }
    }

    public static OfxBank parse(@NotNull final Path file) throws Exception {

        final OfxV2Parser parser = new OfxV2Parser();

        if (FileMagic.isOfxV1(file)) {
            logger.info("Parsing OFX Version 1 file");
            parser.parse(OfxV1ToV2.convertToXML(file), FileMagic.getOfxV1Encoding(file));
        } else if (FileMagic.isOfxV2(file)) {
            logger.info("Parsing OFX Version 2 file");
            parser.parseFile(file);
        } else {
            logger.info("Unknown OFX Version");
        }

        if (parser.getBank() == null) {
            throw new Exception("Bank import failed");
        }

        return postProcess(parser.getBank());
    }

    /**
     * Post processes the OFX transactions for import.  Income transactions with a reinvestment transaction will be
     * stripped out. jGnash has a reinvested dividend transaction that reduces overall transaction count.
     *
     * @param ofxBank OfxBank to process
     * @return OfxBank with post processed transactions
     */
    private static OfxBank postProcess(final OfxBank ofxBank) {
        // Clone the original list
        final List<ImportTransaction> importTransactions = ofxBank.getTransactions();

        // Create a list of Reinvested dividends
        final List<ImportTransaction> reinvestedDividends = importTransactions.stream()
                                                                    .filter(importTransaction -> importTransaction.getTransactionType() == TransactionType.REINVESTDIV)
                                                                    .collect(Collectors.toList());

        // Search through the list and remove matching income transactions
        for (final ImportTransaction reinvestDividend : reinvestedDividends) {

            final Iterator<ImportTransaction> iterator = importTransactions.iterator();

            while (iterator.hasNext()) {
                final ImportTransaction otherTran = iterator.next();

                // if this was OFX income and the securities match and the amount match, remove the transaction
                if (reinvestDividend != otherTran && OfxTags.INCOME.equals(otherTran.getTransactionTypeDescription())) {
                    if (otherTran.getAmount().compareTo(reinvestDividend.getAmount().abs()) == 0) {
                        if (otherTran.getSecurityId().equals(reinvestDividend.getSecurityId())) {
                            iterator.remove();  // remove it
                            // reverse sign
                            reinvestDividend.setAmount(reinvestDividend.getAmount().abs());
                        }
                    }
                }
            }
        }

        return ofxBank;
    }

    /**
     * Parse a date. Time zone and seconds are ignored
     * <p>
     * YYYYMMDDHHMMSS.XXX [gmt offset:tz name]
     *
     * @param date String form of the date
     * @return parsed date
     */
    private static LocalDate parseDate(final String date) {
        int year = Integer.parseInt(date.substring(0, 4)); // year
        int month = Integer.parseInt(date.substring(4, 6)); // month
        int day = Integer.parseInt(date.substring(6, 8)); // day

        return LocalDate.of(year, month, day);
    }

    private static BigDecimal parseAmount(final String amount) {

        /* Must trim the amount for a clean parse
         * Some banks leave extra spaces before the value
         */

        try {
            return new BigDecimal(amount.trim());
        } catch (final NumberFormatException e) {
            if (amount.contains(",")) { // OFX file in not valid and uses commas for decimal separators

                // Use the French locale as it uses commas for decimal separators
                DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(Locale.FRENCH);
                df.setParseBigDecimal(true);    // force return value of BigDecimal

                try {
                    return (BigDecimal) df.parseObject(amount.trim());
                } catch (final ParseException pe) {
                    logger.log(Level.INFO, "Parse amount was: {0}", amount);
                    logger.log(Level.SEVERE, e.getLocalizedMessage(), pe);
                }
            }

            return BigDecimal.ZERO; // give up at this point
        }
    }

    private static boolean parseBoolean(final String bool) {
        return !bool.isEmpty() && bool.startsWith("T");
    }

    /**
     * Parses an InputStream and assumes UTF-8 encoding
     * <p>
     * Illegal characters are corrected automatically if found
     *
     * @param stream InputStream to parse
     */
    public void parse(final InputStream stream) {

        final StringBuilder stringBuilder = new StringBuilder();

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, ENCODING))) {
            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            parse(sanitize(stringBuilder.toString()), ENCODING);
        } catch (final IOException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }
    }

    /**
     * Parses an InputStream using a specified encoding
     *
     * @param stream   InputStream to parse
     * @param encoding encoding to use
     */
    private void parse(final InputStream stream, final String encoding) {
        logger.entering(OfxV2Parser.class.getName(), "parse");

        bank = new OfxBank();

        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        try (final InputStream input = new BufferedInputStream(stream)) {
            XMLStreamReader reader = inputFactory.createXMLStreamReader(input, encoding);
            readOfx(reader);
        } catch (IOException | XMLStreamException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }

        logger.exiting(OfxV2Parser.class.getName(), "parse");
    }

    private void parseFile(final Path path) {

        try (final InputStream stream = new BufferedInputStream(Files.newInputStream(path))) {
            parse(stream);
        } catch (final IOException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }
    }

    public void parse(final String string, final String encoding) throws UnsupportedEncodingException {
        parse(new ByteArrayInputStream(string.getBytes(encoding)), encoding);
    }

    private void readOfx(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "readOfx");

        while (reader.hasNext()) {

            final int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (reader.getLocalName()) {
                    case OFX:   // consume the OFX header here
                        break;
                    case SIGNONMSGSRSV1:
                        parseSignOnMessageSet(reader);
                        break;
                    case BANKMSGSRSV1:
                        parseBankMessageSet(reader);
                        break;
                    case CREDITCARDMSGSRSV1:
                        parseCreditCardMessageSet(reader);
                        break;
                    case INVSTMTMSGSRSV1:
                        parseInvestmentAccountMessageSet(reader);
                        break;
                    case SECLISTMSGSRSV1:
                        parseSecuritesMessageSet(reader);
                        break;
                    default:
                        logger.log(Level.WARNING, "Unknown message set {0}", reader.getLocalName());
                        break;
                }
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "readOfx");
    }

    private void parseInvestmentAccountMessageSet(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseInvestmentAccountMessageSet");

        final QName parsingElement = reader.getName();

        parse:
        while (reader.hasNext()) {
            final int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case STATUS:
                            parseStatementStatus(reader);
                            break;
                        case CURDEF:
                            bank.currency = reader.getElementText();
                            break;
                        case INVACCTFROM:
                            parseAccountInfo(bank, parseAccountInfo(reader));
                            break;
                        case INVTRANLIST:
                            parseInvestmentTransactionList(reader);
                            break;
                        case INVSTMTRS:     // consume the statement download
                            break;
                        case INVPOSLIST:    // consume the securities position list; TODO: Use for reconcile
                        case INV401KBAL:    // consume 401k balance aggregate
                        case INV401K:       // consume 401k account info aggregate
                        case INVBAL:        // consume the investment account balance; TODO: Use for reconcile
                        case INVOOLIST:     // consume open orders
                            consumeElement(reader);
                            break;
                        case INVSTMTTRNRS:
                        case TRNUID:
                        case DTASOF:    // statement date
                            break;
                        default:
                            logger.log(Level.WARNING, "Unknown INVSTMTMSGSRSV1 element: {0}", reader.getLocalName());
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.fine("Found the end of the investment account message set aggregate");
                        break parse;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseInvestmentAccountMessageSet");
    }

    /**
     * Parses a BANKMSGSRSV1 element
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    private void parseBankMessageSet(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseBankMessageSet");

        final QName parsingElement = reader.getName();

        while (reader.hasNext()) {
            final int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case STATUS:
                            parseStatementStatus(reader);
                            break;
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
                            parseAccountInfo(bank, parseAccountInfo(reader));
                            break;
                        case BANKTRANLIST:
                            parseBankTransactionList(reader);
                            break;
                        case STMTTRNRS: // consume it
                        case TRNUID:
                        case STMTRS:
                            break;
                        default:
                            logger.log(Level.WARNING, "Unknown BANKMSGSRSV1 element: {0}", reader.getLocalName());
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.fine("Found the end of the bank message set aggregate");
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
    private void parseCreditCardMessageSet(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseCreditCardMessageSet");

        final QName parsingElement = reader.getName();

        while (reader.hasNext()) {
            final int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case STATUS:
                            parseStatementStatus(reader);
                            break;
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
                            parseAccountInfo(bank, parseAccountInfo(reader));
                            break;
                        case BANKTRANLIST:
                            parseBankTransactionList(reader);
                            break;
                        default:
                            logger.log(Level.WARNING, "Unknown CREDITCARDMSGSRSV1 element: {0}", reader.getLocalName());
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.fine("Found the end of the credit card message set aggregate");
                        break;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseCreditCardMessageSet");
    }

    /**
     * Parses a SECLISTMSGSRSV1 element
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    private void parseSecuritesMessageSet(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseSecuritesMessageSet");

        final QName parsingElement = reader.getName();

        while (reader.hasNext()) {
            final int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    if (SECLIST.equals(reader.getLocalName())) {
                        parseSecuritiesList(reader);
                    } else {
                        logger.log(Level.WARNING, "Unknown SECLISTMSGSRSV1 element: {0}", reader.getLocalName());
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.fine("Found the end of the sercurites set");
                        break;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseSecuritesMessageSet");
    }

    private void parseSecuritiesList(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseSecuritiesList");

        final QName parsingElement = reader.getName();

        parse:
        while (reader.hasNext()) {
            final int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case ASSETCLASS:
                        case OPTTYPE:
                        case STRIKEPRICE:
                        case DTEXPIRE:
                        case SHPERCTRCT:
                        case YIELD:
                            break;  // just consume it, not used
                        case SECID: // underlying stock for an Option.  Not used, so consume it here
                        case UNIQUEID:
                        case UNIQUEIDTYPE:
                            break;
                        case SECINFO:
                            parseSecurity(reader);
                            break;
                        case MFINFO: // just consume it
                        case OPTINFO:
                        case STOCKINFO:
                            break;
                        default:
                            logger.log(Level.WARNING, "Unknown SECLIST element: {0}", reader.getLocalName());
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.fine("Found the end of the securities list");
                        break parse;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseSecuritiesList");
    }

    private void parseSecurity(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseSecurity");

        final QName parsingElement = reader.getName();

        final ImportSecurity importSecurity = new ImportSecurity();

        parse:
        while (reader.hasNext()) {
            final int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case FIID:
                        case SECID: // consume it:
                            break;
                        case UNIQUEIDTYPE:
                            importSecurity.idType = reader.getElementText().trim();
                            break;
                        case UNIQUEID:
                            importSecurity.setId(reader.getElementText().trim());
                            break;
                        case SECNAME:
                            importSecurity.setSecurityName(reader.getElementText().trim());
                            break;
                        case TICKER:
                            importSecurity.setTicker(reader.getElementText().trim());
                            break;
                        case UNITPRICE:
                            importSecurity.setUnitPrice(parseAmount(reader.getElementText()));
                            break;
                        case DTASOF:
                            importSecurity.setLocalDate(parseDate(reader.getElementText()));
                            break;
                        case CURRENCY: // consume the currency aggregate for unit price and handle here
                        case ORIGCURRENCY:
                            break;
                        case RATING:    // consume, not used
                            break;
                        case CURSYM:
                            importSecurity.setCurrency(reader.getElementText().trim());
                            break;
                        case CURRATE:
                            importSecurity.setCurrencyRate(parseAmount(reader.getElementText()));
                            break;
                        default:
                            logger.log(Level.WARNING, "Unknown SECINFO element: {0}", reader.getLocalName());
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.fine("Found the end of the security info");
                        break parse;
                    }
                default:
            }
        }

        bank.addSecurity(importSecurity);

        logger.exiting(OfxV2Parser.class.getName(), "parseSecurity");
    }

    /**
     * Parses a BANKTRANLIST element
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    private void parseBankTransactionList(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseBankTransactionList");

        final QName parsingElement = reader.getName();

        parse:
        while (reader.hasNext()) {
            final int event = reader.next();

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
                        default:
                            logger.log(Level.WARNING, "Unknown BANKTRANLIST element: {0}", reader.getLocalName());
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.fine("Found the end of the bank transaction list");
                        break parse;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseBankTransactionList");
    }

    private void parseInvestmentTransactionList(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseInvestmentTransactionList");

        final QName parsingElement = reader.getName();

        parse:
        while (reader.hasNext()) {
            final int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case DTSTART:
                            bank.dateStart = parseDate(reader.getElementText());
                            break;
                        case DTEND:
                            bank.dateEnd = parseDate(reader.getElementText());
                            break;
                        case BUYSTOCK:
                        case BUYMF:
                        case BUYOTHER:
                        case INCOME:
                        case REINVEST:
                        case SELLSTOCK:
                            parseInvestmentTransaction(reader);
                            break;
                        case INVBANKTRAN:
                            parseBankTransaction(reader);
                            break;
                        default:
                            logger.log(Level.WARNING, "Unknown INVTRANLIST element: {0}", reader.getLocalName());
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.fine("Found the end of the investment transaction list");
                        break parse;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseInvestmentTransactionList");
    }

    private void parseInvestmentTransaction(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseInvestmentTransaction");

        final QName parsingElement = reader.getName();

        final ImportTransaction tran = new ImportTransaction();

        // set the descriptive transaction type text as well
        tran.setTransactionTypeDescription(parsingElement.toString());

        // extract the investment transaction type from the element name
        switch (parsingElement.toString()) {
            case BUYMF:
            case BUYOTHER:
            case BUYSTOCK:
                tran.setTransactionType(TransactionType.BUYSHARE);
                break;
            case SELLMF:
            case SELLOTHER:
            case SELLSTOCK:
                tran.setTransactionType(TransactionType.SELLSHARE);
                break;
            case INCOME:    // dividend
                tran.setTransactionType(TransactionType.DIVIDEND);
                break;
            case REINVEST:
                tran.setTransactionType(TransactionType.REINVESTDIV);
                break;
            default:
                logger.log(Level.WARNING, "Unknown investment transaction type: {0}", parsingElement.toString());
                break;
        }

        parse:
        while (reader.hasNext()) {
            final int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case DTSETTLE:
                            tran.setDatePosted(parseDate(reader.getElementText()));
                            break;
                        case DTTRADE:
                            tran.setDateUser(parseDate(reader.getElementText()));
                            break;
                        case TOTAL: // total of the investment transaction
                            tran.setAmount(parseAmount(reader.getElementText()));
                            break;
                        case FITID:
                            tran.setFITID(reader.getElementText());
                            break;
                        case UNIQUEID:  // the security for the transaction
                            tran.setSecurityId(reader.getElementText());
                            break;
                        case UNIQUEIDTYPE:  // the type of security for the transaction
                            tran.setSecurityType(reader.getElementText());
                            break;
                        case UNITS:
                            tran.setUnits(parseAmount(reader.getElementText()));
                            break;
                        case UNITPRICE:
                            tran.setUnitPrice(parseAmount(reader.getElementText()));
                            break;
                        case FEES:  // investment fees
                            tran.setFees(parseAmount(reader.getElementText()));
                            break;
                        case COMMISSION:    // investment commission
                            tran.setCommission(parseAmount(reader.getElementText()));
                            break;
                        case INCOMETYPE:
                            tran.setIncomeType(reader.getElementText());
                            break;
                        case SUBACCTSEC:
                        case SUBACCTFROM:
                        case SUBACCTTO:
                        case SUBACCTFUND:
                            tran.setSubAccount(reader.getElementText());
                            break;
                        case CHECKNUM:
                            tran.setCheckNumber(reader.getElementText());
                            break;
                        case NAME:
                        case PAYEE: // either PAYEE or NAME will be used
                            tran.setPayee(removeExtraWhiteSpace(reader.getElementText()));
                            break;
                        case MEMO:
                            tran.setMemo(removeExtraWhiteSpace(reader.getElementText()));
                            break;
                        case CATEGORY:  // Chase bank mucking up the OFX standard
                            break;
                        case SIC:
                            tran.setSIC(reader.getElementText());
                            break;
                        case REFNUM:
                            tran.setRefNum(reader.getElementText());
                            break;
                        case PAYEEID:
                            tran.setPayeeId(removeExtraWhiteSpace(reader.getElementText()));
                            break;
                        case CURRENCY:  // currency used for the transaction
                            //tran.currency = reader.getElementText();
                            consumeElement(reader);
                            break;
                        case ORIGCURRENCY:
                            tran.setCurrency(reader.getElementText());
                            break;
                        case TAXEXEMPT:
                            tran.setTaxExempt(parseBoolean(reader.getElementText()));
                            break;
                        case BUYTYPE:
                        case INVBUY:    // consume
                        case INVTRAN:
                        case SECID:
                            break;
                        case LOANID:    // consume 401k loan information
                        case LOANPRINCIPAL:
                        case LOANINTEREST:
                            break;
                        case INV401KSOURCE: // consume 401k information
                        case DTPAYROLL:
                        case PRIORYEARCONTRIB:
                            break;
                        default:
                            logger.log(Level.WARNING, "Unknown investment transaction element: {0}",
                                    reader.getLocalName());
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        //logger.fine("Found the end of the investment transaction");
                        break parse;
                    }
                default:
            }
        }

        bank.addTransaction(tran);

        logger.exiting(OfxV2Parser.class.getName(), "parseInvestmentTransaction");
    }

    private String removeExtraWhiteSpace(final String string) {
        return extraSpaceRegex.matcher(string).replaceAll(" ").trim();
    }

    /**
     * Parses a STMTTRN element
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    private void parseBankTransaction(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseBankTransaction");

        final QName parsingElement = reader.getName();

        final ImportTransaction tran = new ImportTransaction();

        parse:
        while (reader.hasNext()) {
            final int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case TRNTYPE:
                            tran.setTransactionTypeDescription(reader.getElementText());
                            break;
                        case DTPOSTED:
                            tran.setDatePosted(parseDate(reader.getElementText()));
                            break;
                        case DTUSER:
                            tran.setDateUser(parseDate(reader.getElementText()));
                            break;
                        case TRNAMT:
                            tran.setAmount(parseAmount(reader.getElementText()));
                            break;
                        case FITID:
                            tran.setFITID(reader.getElementText());
                            break;
                        case CHECKNUM:
                            tran.setCheckNumber(reader.getElementText());
                            break;
                        case NAME:
                        case PAYEE: // either PAYEE or NAME will be used
                            tran.setPayee(removeExtraWhiteSpace(reader.getElementText()));
                            break;
                        case MEMO:
                            tran.setMemo(removeExtraWhiteSpace(reader.getElementText()));
                            break;
                        case CATEGORY:  // Chase bank mucking up the OFX standard
                            break;
                        case SIC:
                            tran.setSIC(reader.getElementText());
                            break;
                        case REFNUM:
                            tran.setRefNum(reader.getElementText());
                            break;
                        case PAYEEID:
                            tran.setPayeeId(removeExtraWhiteSpace(reader.getElementText()));
                            break;
                        case CURRENCY:
                        case ORIGCURRENCY:
                            tran.setCurrency(reader.getElementText());
                            break;
                        case SUBACCTFUND: // transfer into / out off an investment account
                            tran.setSubAccount(reader.getElementText());
                            break;
                        case STMTTRN:   // consume, occurs with an investment account transfer
                            break;
                        case BANKACCTTO:
                        case CCACCTTO:
                        case INVACCTTO:
                            parseAccountInfo(tran, parseAccountInfo(reader));
                            break;
                        default:
                            logger.log(Level.WARNING, "Unknown STMTTRN element: {0}", reader.getLocalName());
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.fine("Found the end of the bank transaction");
                        break parse;
                    }
                default:
            }
        }

        bank.addTransaction(tran);

        logger.exiting(OfxV2Parser.class.getName(), "parseBankTransaction");
    }

    private static void parseAccountInfo(final ImportTransaction importTransaction, final AccountInfo accountInfo) {
        importTransaction.setAccountTo(accountInfo.accountId);
    }

    /**
     * Parses a BANKACCTFROM element
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    private static AccountInfo parseAccountInfo(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseAccountInfo");

        final QName parsingElement = reader.getName();

        final AccountInfo accountInfo = new AccountInfo();

        parse:
        while (reader.hasNext()) {
            final int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case BANKID:
                        case BROKERID:  // normally a URL per the OFX specification
                            accountInfo.bankId = reader.getElementText();
                            break;
                        case ACCTID:
                            accountInfo.accountId = reader.getElementText();
                            break;
                        case ACCTTYPE:
                            accountInfo.accountType = reader.getElementText();
                            break;
                        case BRANCHID:
                            accountInfo.branchId = reader.getElementText();
                            break;
                        default:
                            logger.log(Level.WARNING, "Unknown BANKACCTFROM element: {0}", reader.getLocalName());
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.fine("Found the end of the bank and account info aggregate");
                        break parse;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseAccountInfo");

        return accountInfo;
    }

    private static void parseAccountInfo(final OfxBank ofxBank, final AccountInfo accountInfo) {
        ofxBank.bankId = accountInfo.bankId;
        ofxBank.branchId = accountInfo.branchId;
        ofxBank.accountId = accountInfo.accountId;
        ofxBank.accountType = accountInfo.accountType;
    }

    /**
     * Parses a LEDGERBAL element
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    private void parseLedgerBalance(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseLedgerBalance");

        final QName parsingElement = reader.getName();

        parse:
        while (reader.hasNext()) {
            final int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case BALAMT:
                            bank.ledgerBalance = parseAmount(reader.getElementText());
                            break;
                        case DTASOF:
                            bank.ledgerBalanceDate = parseDate(reader.getElementText());
                            break;
                        default:
                            logger.log(Level.WARNING, "Unknown ledger balance information {0}", reader.getLocalName());
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.fine("Found the end of the ledger balance aggregate");
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
    private void parseAvailableBalance(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseAvailableBalance");

        final QName parsingElement = reader.getName();

        parse:
        while (reader.hasNext()) {
            final int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case BALAMT:
                            bank.availBalance = parseAmount(reader.getElementText());
                            break;
                        case DTASOF:
                            bank.availBalanceDate = parseDate(reader.getElementText());
                            break;
                        default:
                            logger.log(Level.WARNING, "Unknown AVAILBAL element {0}", reader.getLocalName());
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.fine("Found the end of the available balance aggregate");
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
    private void parseSignOnMessageSet(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseSignOnMessageSet");

        final QName parsingElement = reader.getName();

        parse:
        while (reader.hasNext()) {
            final int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case LANGUAGE:
                            language = reader.getElementText();
                            break;
                        case STATUS:
                            parseSignOnStatus(reader);
                            break;
                        case FI:
                        case FID:
                        case ORG:
                        case INTUBID:
                        case INTUUSERID:
                        default:
                            break;
                    }

                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.fine("Found the end of the sign-on message set aggregate");
                        break parse;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseSignOnMessageSet");
    }

    /**
     * Parses a STATUS element from the SignOn element
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    private void parseSignOnStatus(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseSignOnStatus");

        final QName parsingElement = reader.getName();

        parse:
        while (reader.hasNext()) {
            final int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case CODE:
                            try {
                                statusCode = Integer.parseInt(reader.getElementText());
                            } catch (final NumberFormatException ex) {
                                logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
                            }
                            break;
                        case SEVERITY:
                            statusSeverity = reader.getElementText();
                            break;
                        case MESSAGE:   // consume it, not used
                            statusMessage = reader.getElementText();
                            break;
                        default:
                            logger.log(Level.WARNING, "Unknown STATUS element {0}", reader.getLocalName());
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.fine("Found the end of the statusCode response");
                        break parse;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseSignOnStatus");
    }

    /**
     * Parses a STATUS element from the statement element
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    private void parseStatementStatus(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "parseSignOnStatus");

        final QName parsingElement = reader.getName();

        parse:
        while (reader.hasNext()) {
            final int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case CODE:
                            try {
                                bank.statusCode = Integer.parseInt(reader.getElementText());
                            } catch (final NumberFormatException ex) {
                                logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
                            }
                            break;
                        case SEVERITY:
                            bank.statusSeverity = reader.getElementText();
                            break;
                        case MESSAGE:
                            bank.statusMessage = reader.getElementText();
                            break;
                        default:
                            logger.log(Level.WARNING, "Unknown STATUS element {0}", reader.getLocalName());
                            break;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.fine("Found the end of the statement status response");
                        break parse;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "parseStatementStatus");
    }

    /**
     * Consumes an element that will not be used
     *
     * @param reader shared XMLStreamReader
     * @throws XMLStreamException XML parsing error has occurred
     */
    private static void consumeElement(final XMLStreamReader reader) throws XMLStreamException {
        logger.entering(OfxV2Parser.class.getName(), "consumeElement");

        final QName parsingElement = reader.getName();

        parse:
        while (reader.hasNext()) {
            final int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    reader.getLocalName();

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(parsingElement)) {
                        logger.log(Level.FINEST, "Found the end of consumed element {0}", reader.getName());
                        break parse;
                    }
                default:
            }
        }

        logger.exiting(OfxV2Parser.class.getName(), "consumeElement");
    }

    public OfxBank getBank() {
        logger.log(Level.INFO, "OFX Status was: {0}", statusCode);
        logger.log(Level.INFO, "Status Level was: {0}", statusSeverity);
        logger.log(Level.INFO, "File language was: {0}", language);

        return bank;
    }

    int getStatusCode() {
        return statusCode;
    }

    String getStatusSeverity() {
        return statusSeverity;
    }

    public String getLanguage() {
        return language;
    }

    String getStatusMessage() {
        return statusMessage;
    }
}
