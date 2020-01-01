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
package jgnash.convert.exportantur.ofx;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.convert.common.OfxTags;
import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.SecurityNode;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionType;
import jgnash.util.FileUtils;
import jgnash.util.NotNull;

/**
 * Primary class for OFX export. The SGML format is used instead of the newer
 * XML to offer the best compatibility with older importers
 *
 * @author Craig Cavanaugh
 */
public class OfxExport implements OfxTags {

    private static final String[] OFXHEADER = new String[]{"OFXHEADER:100", "DATA:OFXSGML", "VERSION:102",
            "SECURITY:NONE", "ENCODING:USASCII", "CHARSET:1252", "COMPRESSION:NONE", "OLDFILEUID:NONE",
            "NEWFILEUID:NONE"};

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Account account;

    private final LocalDate startDate;

    private final LocalDate endDate;

    private final File file;

    private IndentedPrintWriter indentedWriter;

    private int indentLevel = 0;

    public OfxExport(final Account account, final LocalDate startDate, final LocalDate endDate, final File file) {
        this.account = account;
        this.startDate = startDate;
        this.endDate = endDate;
        this.file = file;
    }

    public void exportAccount() {
        Objects.requireNonNull(account);
        Objects.requireNonNull(startDate);
        Objects.requireNonNull(endDate);
        Objects.requireNonNull(file);

        final LocalDate exportDate = LocalDate.now();

        // force a correct file extension
        final String fileName = FileUtils.stripFileExtension(file.getAbsolutePath()) + ".ofx";

        try (final IndentedPrintWriter writer = new IndentedPrintWriter(Files.newBufferedWriter(Paths.get(fileName),
                Charset.forName("windows-1252")))) {

            indentedWriter = writer;

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
                    writer.println(wrapOpen(BROKERID), indentLevel); //  required for investment accounts, but jGnash does not manage a broker ID, normally a web URL
                    break;
                default:
                    writer.println(wrapOpen(BANKID) + account.getBankId(), indentLevel); // savings and checking only
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
                case MONEYMKRT:
                    writer.println(wrapOpen(ACCTTYPE) + MONEYMRKT, indentLevel);
                    break;
                default:
                    break;
            }

            writer.println(wrapClose(getAccountFromAggregate(account)), --indentLevel);

            // begin start of transaction list
            writer.println(wrapOpen(getTransactionList(account)), indentLevel++);
            writer.println(wrapOpen(DTSTART) + encodeDate(startDate), indentLevel);
            writer.println(wrapOpen(DTEND) + encodeDate(endDate), indentLevel);

            // write the transaction list
            if (account.getAccountType() == AccountType.INVEST || account.getAccountType() == AccountType.MUTUAL) {
                writeInvestmentTransactions();
            } else {
                writeBankTransactions();
            }

            // end of transaction list
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

    /**
     * Writes all bank account transactions within the date range
     */
    private void writeBankTransactions() {
        account.getTransactions(startDate, endDate).forEach(this::writeBankTransaction);
    }

    /**
     * Writes all investment account transactions within the date range
     */
    private void writeInvestmentTransactions() {
        for (final Transaction transaction : account.getTransactions(startDate, endDate)) {
            if (transaction instanceof InvestmentTransaction) {
                final InvestmentTransaction invTransaction = (InvestmentTransaction) transaction;

                switch (invTransaction.getTransactionType()) {
                    case ADDSHARE:
                    case BUYSHARE:
                        writeBuyStockTransaction(invTransaction);
                        break;
                    case REMOVESHARE:
                    case SELLSHARE:
                        writeSellStockTransaction(invTransaction);
                        break;
                    case DIVIDEND:
                        writeDividendTransaction(invTransaction);
                        break;
                    case REINVESTDIV:
                        writeReinvestStockTransaction(invTransaction);
                        break;
                    default:
                        break;
                }
            } else {    // bank transaction, write it
                indentedWriter.println(wrapOpen(INVBANKTRAN), indentLevel++);
                writeBankTransaction(transaction);
                indentedWriter.println(wrapClose(INVBANKTRAN), --indentLevel);
            }
        }
    }

    /**
     * Writes one bank transaction
     *
     * @param transaction {@code Transaction} to write
     */
    private void writeBankTransaction(final Transaction transaction) {
        indentedWriter.println(wrapOpen(STMTTRN), indentLevel++);
        indentedWriter.println(wrapOpen(TRNTYPE)
                + (transaction.getAmount(account).compareTo(BigDecimal.ZERO) >= 1 ? CREDIT : DEBIT), indentLevel);

        indentedWriter.println(wrapOpen(DTPOSTED) + encodeDate(transaction.getLocalDate()), indentLevel);
        indentedWriter.println(wrapOpen(TRNAMT) + transaction.getAmount(account).toPlainString(), indentLevel);
        indentedWriter.println(wrapOpen(REFNUM) + transaction.getUuid(), indentLevel);
        indentedWriter.println(wrapOpen(NAME) + transaction.getPayee(), indentLevel);
        indentedWriter.println(wrapOpen(MEMO) + transaction.getMemo(), indentLevel);

        // write the check number if applicable
        if (account.getAccountType() == AccountType.CHECKING && !transaction.getNumber().isEmpty()) {
            indentedWriter.println(wrapOpen(CHECKNUM) + transaction.getNumber(), indentLevel);
        }

        // write out the banks transaction id if previously imported
        writeFitID(transaction);

        // write out the account to
        if (transaction.getTransactionType() == TransactionType.DOUBLEENTRY) {
            final Account other = transaction.getTransactionEntries().get(0).getCreditAccount() != account
                    ? transaction.getTransactionEntries().get(0).getCreditAccount()
                    : transaction.getTransactionEntries().get(0).getDebitAccount();

            if (other != null && other.getAccountNumber() != null && other.getAccountNumber().length() > 0) {
                if (other.getAccountType() != AccountType.EXPENSE && other.getAccountType() != AccountType.INCOME) {
                    writeAccountTo(other);
                }
            }
        }

        indentedWriter.println(wrapClose(STMTTRN), --indentLevel);
    }

    private void writeFitID(@NotNull final Transaction transaction) {
        // write out the banks transaction id if previously imported
        if (transaction.getFitid() != null && !transaction.getFitid().isEmpty()) {
            indentedWriter.println(wrap(FITID, transaction.getFitid()), indentLevel);
        } else {
            indentedWriter.println(wrap(FITID, transaction.getUuid().toString()), indentLevel);
        }
    }

    private void writeSecID(final SecurityNode node) {

        // write security information
        indentedWriter.println(wrapOpen(SECID), indentLevel++);

        if (!node.getISIN().isEmpty()) {
            indentedWriter.println(wrap(UNIQUEID, node.getISIN()), indentLevel);
        } else {
            indentedWriter.println(wrap(UNIQUEID, node.getSymbol()), indentLevel);
        }

        indentedWriter.println(wrap(UNIQUEIDTYPE, "CUSIP"), indentLevel);
        indentedWriter.println(wrapClose(SECID), --indentLevel);
    }

    private void writeBuyStockTransaction(final InvestmentTransaction transaction) {
        indentedWriter.println(wrapOpen(BUYSTOCK), indentLevel++);
        indentedWriter.println(wrapOpen(INVBUY), indentLevel++);

        indentedWriter.println(wrapOpen(INVTRAN), indentLevel++);

        // write the FITID
        writeFitID(transaction);

        indentedWriter.println(wrap(DTTRADE, encodeDate(transaction.getLocalDate())), indentLevel);
        indentedWriter.println(wrap(DTSETTLE, encodeDate(transaction.getLocalDate())), indentLevel);

        indentedWriter.println(wrapClose(INVTRAN), --indentLevel);

        // write security information
        writeSecID(transaction.getSecurityNode());

        indentedWriter.println(wrap(UNITS, transaction.getQuantity().toPlainString()), indentLevel);
        indentedWriter.println(wrap(UNITPRICE, transaction.getPrice().toPlainString()), indentLevel);
        indentedWriter.println(wrap(COMMISSION, transaction.getFees().toPlainString()), indentLevel);
        indentedWriter.println(wrap(TOTAL, transaction.getTotal(account).toPlainString()), indentLevel);
        indentedWriter.println(wrap(SUBACCTSEC, "CASH"), indentLevel);
        indentedWriter.println(wrap(SUBACCTFUND, "CASH"), indentLevel);

        indentedWriter.println(wrapClose(INVBUY), --indentLevel);
        indentedWriter.println(wrap(BUYTYPE, "BUY"), indentLevel);
        indentedWriter.println(wrapClose(BUYSTOCK), --indentLevel);
    }

    private void writeSellStockTransaction(final InvestmentTransaction transaction) {
        indentedWriter.println(wrapOpen(SELLSTOCK), indentLevel++);
        indentedWriter.println(wrapOpen(INVSELL), indentLevel++);

        indentedWriter.println(wrapOpen(INVTRAN), indentLevel++);

        // write the FITID
        writeFitID(transaction);

        indentedWriter.println(wrap(DTTRADE, encodeDate(transaction.getLocalDate())), indentLevel);
        indentedWriter.println(wrap(DTSETTLE, encodeDate(transaction.getLocalDate())), indentLevel);
        indentedWriter.println(wrapClose(INVTRAN), --indentLevel);

        // write security information
        writeSecID(transaction.getSecurityNode());

        indentedWriter.println(wrap(UNITS, transaction.getQuantity().toPlainString()), indentLevel);
        indentedWriter.println(wrap(UNITPRICE, transaction.getPrice().toPlainString()), indentLevel);
        indentedWriter.println(wrap(COMMISSION, transaction.getFees().toPlainString()), indentLevel);
        indentedWriter.println(wrap(TOTAL, transaction.getTotal(account).toPlainString()), indentLevel);
        indentedWriter.println(wrap(SUBACCTSEC, "CASH"), indentLevel);
        indentedWriter.println(wrap(SUBACCTFUND, "CASH"), indentLevel);

        indentedWriter.println(wrapClose(INVSELL), --indentLevel);
        indentedWriter.println(wrap(SELLTYPE, "SELL"), indentLevel);
        indentedWriter.println(wrapClose(SELLSTOCK), --indentLevel);
    }

    /**
     * Reinvested transaction is a two part process.
     * Need to show Income into cash and then the reinvestment from cash
     *
     * @param transaction transaction to write
     */
    private void writeReinvestStockTransaction(final InvestmentTransaction transaction) {

        // Part one, show dividend income to cash
        writeDividendTransaction(transaction);

        // Part two, show reinvest from cash
        indentedWriter.println(wrapOpen(REINVEST), indentLevel++);

        indentedWriter.println(wrapOpen(INVTRAN), indentLevel++);

        // write the FITID
        writeFitID(transaction);

        indentedWriter.println(wrap(DTTRADE, encodeDate(transaction.getLocalDate())), indentLevel);
        indentedWriter.println(wrap(DTSETTLE, encodeDate(transaction.getLocalDate())), indentLevel);
        indentedWriter.println(wrap(MEMO, "Distribution reinvestment: " + transaction.getSecurityNode().getSymbol()), indentLevel);
        indentedWriter.println(wrapClose(INVTRAN), --indentLevel);

        // write security information
        writeSecID(transaction.getSecurityNode());
        indentedWriter.println(wrap(INCOMETYPE, "DIV"), indentLevel);
        indentedWriter.println(wrap(TOTAL, transaction.getTotal(account).abs().negate().toPlainString()), indentLevel);
        indentedWriter.println(wrap(SUBACCTSEC, "CASH"), indentLevel);

        indentedWriter.println(wrap(UNITS, transaction.getQuantity().toPlainString()), indentLevel);
        indentedWriter.println(wrap(UNITPRICE, transaction.getPrice().toPlainString()), indentLevel);
        indentedWriter.println(wrap(COMMISSION, transaction.getFees().toPlainString()), indentLevel);
        indentedWriter.println(wrapClose(REINVEST), --indentLevel);
    }

    private void writeDividendTransaction(final InvestmentTransaction transaction) {
        indentedWriter.println(wrapOpen(INCOME), indentLevel++);

        indentedWriter.println(wrapOpen(INVTRAN), indentLevel++);
        writeFitID(transaction);  // write the FITID

        indentedWriter.println(wrap(DTTRADE, encodeDate(transaction.getLocalDate())), indentLevel);
        indentedWriter.println(wrap(DTSETTLE, encodeDate(transaction.getLocalDate())), indentLevel);
        indentedWriter.println(wrap(MEMO, "Dividend: " + transaction.getSecurityNode().getSymbol()), indentLevel);
        indentedWriter.println(wrapClose(INVTRAN), --indentLevel);

        // write security information
        writeSecID(transaction.getSecurityNode());

        indentedWriter.println(wrap(INCOMETYPE, "DIV"), indentLevel);
        indentedWriter.println(wrap(TOTAL, transaction.getTotal(account).abs().toPlainString()), indentLevel);
        indentedWriter.println(wrap(SUBACCTSEC, "CASH"), indentLevel);
        indentedWriter.println(wrap(SUBACCTFUND, "CASH"), indentLevel);
        indentedWriter.println(wrapClose(INCOME), --indentLevel);
    }

    private String encodeDate(final LocalDate date) {
        return dateTimeFormatter.format(date) + "000000";
    }

    private static String wrapOpen(final String element) {
        return "<" + element + ">";
    }

    private static String wrapClose(final String element) {
        return "</" + element + ">";
    }

    private static String wrap(final String element, final String text) {
        return wrapOpen(element) + text + wrapClose(element);
    }

    private void writeAccountTo(Account account) {
        // write account identification
        indentedWriter.println(wrapOpen(getAccountToAggregate(account)), indentLevel++);

        switch (account.getAccountType()) {
            case INVEST:
            case MUTUAL:
                indentedWriter.println(wrapOpen(BROKERID), indentLevel); //  required for investment accounts, but jGnash does not manage a broker ID, normally a web URL
                break;
            default:
                indentedWriter.println(wrapOpen(BANKID) + account.getBankId(), indentLevel); // savings and checking only
                break;
        }

        indentedWriter.println(wrapOpen(ACCTID) + account.getAccountNumber(), indentLevel);

        // write the required account type
        switch (account.getAccountType()) {
            case CHECKING:
                indentedWriter.println(wrapOpen(ACCTTYPE) + CHECKING, indentLevel);
                break;
            case ASSET:
            case BANK:
            case CASH:
                indentedWriter.println(wrapOpen(ACCTTYPE) + SAVINGS, indentLevel);
                break;
            case CREDIT:
            case LIABILITY:
                indentedWriter.println(wrapOpen(ACCTTYPE) + CREDITLINE, indentLevel);
                break;
            case SIMPLEINVEST:
            case MONEYMKRT:
                indentedWriter.println(wrapOpen(ACCTTYPE) + MONEYMRKT, indentLevel);
                break;
            default:
                break;
        }

        indentedWriter.println(wrapClose(getAccountToAggregate(account)), --indentLevel);

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

    private static String getAccountToAggregate(final Account account) {
        switch (account.getAccountType()) {
            case ASSET:
            case BANK:
            case CASH:
            case CHECKING:
            case SIMPLEINVEST:
                return BANKACCTTO;
            case CREDIT:
            case LIABILITY:
                return CCACCTTO;
            case INVEST:
            case MUTUAL:
                return INVACCTTO;
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

        private static final String INDENT = "  ";

        IndentedPrintWriter(final Writer out) {
            super(out);
        }

        void println(final String x, final int indentLevel) {
            for (int i = 0; i < indentLevel; i++) {
                write(INDENT);
            }
            println(x);
        }
    }
}
