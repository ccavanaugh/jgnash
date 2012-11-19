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
import jgnash.engine.AccountType;
import jgnash.engine.Transaction;
import jgnash.imports.ofx.OfxTags;
import jgnash.util.FileUtils;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Primary class for OFX export.  The SGML format is used instead of the newer XML
 * to offer the best compatibility with older importers
 *
 * @author Craig Cavanaugh
 */
public class OfxExport implements OfxTags {

    private static final String[] OFXHEADER = new String[]{"OFXHEADER:100", "DATA:OFXSGML", "VERSION:102", "SECURITY:NONE",
            "ENCODING:USASCII", "CHARSET:1252", "COMPRESSION:NONE", "OLDFILEUID:NONE", "NEWFILEUID:NONE"};

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    private Account account;

    private Date startDate;

    private Date endDate;

    private File file;

    public OfxExport(final Account account, final Date startDate, final Date endDate, final File file) {
        this.account = account;
        this.startDate = startDate;
        this.endDate = endDate;
        this.file = file;
    }

    public void exportAccount() {

        final Date exportDate = new Date();

        int indentLevel = 0;

        if (account == null || startDate == null || endDate == null || file == null) {
            throw new RuntimeException();
        }

        // force a correct file extension
        final String fileName = FileUtils.stripFileExtension(file.getAbsolutePath()) + ".ofx";

        try (IndentedPrintWriter writer = new IndentedPrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), Charset.forName("windows-1252"))))) {

            // write the required header
            for (String line : OFXHEADER) {
                writer.println(line, indentLevel);
            }
            writer.println();

            // start of data
            writer.println(wrapOpen(OFX), indentLevel++);

            // write sign-on response
            writer.println(wrapOpen(SIGNONMSGSRSV1), indentLevel++);
            writer.println(wrapOpen(SONRS), indentLevel++);
            writer.println(wrapOpen(STATUS), indentLevel++);
            writer.println(wrapOpen(CODE) + "0", indentLevel);
            writer.println(wrapOpen(SEVERITY) + "INFO", indentLevel);
            writer.println(wrapClose(STATUS), --indentLevel);
            writer.println(wrapOpen(DTSERVER) + encodeDate(exportDate), indentLevel);
            writer.println(wrapOpen(LANGUAGE) + "ENG", indentLevel);
            writer.println(wrapClose(SONRS), --indentLevel);
            writer.println(wrapClose(SIGNONMSGSRSV1), --indentLevel);

            writer.println(wrapOpen(getBankingMessageSetAggregate(account)), indentLevel++);
            writer.println(wrapOpen(getResponse(account)), indentLevel++);
            writer.println(wrapOpen(TRNUID) + "1", indentLevel);
            writer.println(wrapOpen(STATUS), indentLevel++);
            writer.println(wrapOpen(CODE) + "0", indentLevel);
            writer.println(wrapOpen(SEVERITY) + "INFO", indentLevel);
            writer.println(wrapClose(STATUS), --indentLevel);

            // begin start of statement response
            writer.println(wrapOpen(getStatementResponse(account)), indentLevel++);
            writer.println(wrapOpen(CURDEF) + account.getCurrencyNode().getSymbol(), indentLevel);

            // write account identification
            writer.println(wrapOpen(getAccountFromAggregate(account)), indentLevel++);

            switch (account.getAccountType()) {
                case INVEST:
                case MUTUAL:
                    writer.println(wrapOpen(BROKERID), indentLevel);  //  required for investment accounts, but jGnash does not manage a broker ID, normally a web URL
                    break;
                default:
                    writer.println(wrapOpen(BANKID) + account.getBankId(), indentLevel);  // savings and checking only
                    break;
            }

            writer.println(wrapOpen(ACCTID) + account.getAccountNumber(), indentLevel);

            // write the required account type
            switch (account.getAccountType()) {
                case CHECKING:
                    writer.println(wrapOpen(ACCTTYPE) + CHECKING, indentLevel);
                    break;
                case ASSET:
                case BANK:
                case CASH:
                    writer.println(wrapOpen(ACCTTYPE) + SAVINGS, indentLevel);
                    break;
                case CREDIT:
                case LIABILITY:
                    writer.println(wrapOpen(ACCTTYPE) + CREDITLINE, indentLevel);
                    break;
                case SIMPLEINVEST:
                    writer.println(wrapOpen(ACCTTYPE) + MONEYMRKT, indentLevel);
                    break;
                default:
                    break;
            }

            writer.println(wrapClose(getAccountFromAggregate(account)), --indentLevel);

            // begin start of bank transaction list
            writer.println(wrapOpen(getTransactionList(account)), indentLevel++);
            writer.println(wrapOpen(DTSTART) + encodeDate(startDate), indentLevel);
            writer.println(wrapOpen(DTEND) + encodeDate(endDate), indentLevel);

            // write bank transactions
            for (Transaction transaction : account.getTransactions(startDate, endDate)) {
                writer.println(wrapOpen(STMTTRN), indentLevel++);
                writer.println(wrapOpen(TRNTYPE) + (transaction.getAmount(account).compareTo(BigDecimal.ZERO) == 1 ? CREDIT : DEBIT), indentLevel);

                writer.println(wrapOpen(DTPOSTED) + encodeDate(transaction.getDate()), indentLevel);
                writer.println(wrapOpen(TRNAMT) + transaction.getAmount(account).toPlainString(), indentLevel);
                writer.println(wrapOpen(REFNUM) + transaction.getUuid(), indentLevel);
                writer.println(wrapOpen(NAME) + transaction.getPayee(), indentLevel);
                writer.println(wrapOpen(MEMO) + transaction.getMemo(), indentLevel);

                // write the check number if applicable
                if (account.getAccountType() == AccountType.CHECKING && !transaction.getNumber().isEmpty()) {
                    writer.println(wrapOpen(CHECKNUM) + transaction.getNumber(), indentLevel);
                }

                // write out the banks transaction id if previously imported
                if (transaction.getFitid() != null && !transaction.getFitid().isEmpty()) {
                    writer.println(wrapOpen(FITID) + transaction.getFitid(), indentLevel);
                }

                writer.println(wrapClose(STMTTRN), --indentLevel);
            }

            // end of bank transaction list
            writer.println(wrapClose(getTransactionList(account)), --indentLevel);

            // write ledger balance
            writer.println(wrapOpen(LEDGERBAL), indentLevel++);
            writer.println(wrapOpen(BALAMT) + account.getBalance(endDate).toPlainString(), indentLevel);
            writer.println(wrapOpen(DTASOF) + encodeDate(exportDate), indentLevel);
            writer.println(wrapClose(LEDGERBAL), --indentLevel);

            // end of statement response
            writer.println(wrapClose(getStatementResponse(account)), --indentLevel);
            writer.println(wrapClose(getResponse(account)), --indentLevel);
            writer.println(wrapClose(getBankingMessageSetAggregate(account)), --indentLevel);

            // finished
            writer.println(wrapClose(OFX), --indentLevel);
        } catch (IOException e) {
            Logger.getLogger(OfxExport.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private static void writeBankTransactions() {

    }

    private static String encodeDate(final Date date) {
        return dateFormat.format(date);
    }

    private static String wrapOpen(final String string) {
        return "<" + string + ">";
    }

    private static String wrapClose(final String string) {
        return "</" + string + ">";
    }

    private static String getBankingMessageSetAggregate(final Account account) {
        switch (account.getAccountType()) {
            case ASSET:
            case BANK:
            case CASH:
            case CHECKING:
            case SIMPLEINVEST:
                return BANKMSGSRSV1;
            case CREDIT:
            case LIABILITY:
                return CREDITCARDMSGSRSV1;
            case INVEST:
            case MUTUAL:
                return INVSTMTMSGSRSV1;
            default:
                return "";
        }
    }

    private static String getResponse(final Account account) {
        switch (account.getAccountType()) {
            case ASSET:
            case BANK:
            case CASH:
            case CHECKING:
            case SIMPLEINVEST:
                return STMTTRNRS;
            case CREDIT:
            case LIABILITY:
                return CCSTMTTRNRS;
            case INVEST:
            case MUTUAL:
                return INVSTMTTRNRS;
            default:
                return "";
        }
    }

    private static String getStatementResponse(final Account account) {
        switch (account.getAccountType()) {
            case ASSET:
            case BANK:
            case CASH:
            case CHECKING:
            case SIMPLEINVEST:
                return STMTRS;
            case CREDIT:
            case LIABILITY:
                return CCSTMTRS;
            case INVEST:
            case MUTUAL:
                return INVSTMTRS;
            default:
                return "";
        }
    }

    private static String getAccountFromAggregate(final Account account) {
        switch (account.getAccountType()) {
            case ASSET:
            case BANK:
            case CASH:
            case CHECKING:
            case SIMPLEINVEST:
                return BANKACCTFROM;
            case CREDIT:
            case LIABILITY:
                return CCACCTFROM;
            case INVEST:
            case MUTUAL:
                return INVACCTFROM;
            default:
                return "";
        }
    }

    private static String getTransactionList(final Account account) {
        switch (account.getAccountType()) {
            case INVEST:
            case MUTUAL:
                return INVTRANLIST;
            default:
                return BANKTRANLIST;
        }
    }

    /**
     * Support class to make writing indented SGML easier
     */
    private static class IndentedPrintWriter extends PrintWriter {

        private static String INDENT = "  ";

        public IndentedPrintWriter(final Writer out) {
            super(out);
        }

        public void println(final String x, final int indentLevel) {
            for (int i = 0; i < indentLevel; i++) {
                write(INDENT);
            }
            println(x);
        }
    }
}
