/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
package jgnash.convert.exportantur.csv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.Comparators;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.resource.util.ResourceUtils;
import jgnash.time.DateUtils;
import jgnash.util.FileUtils;
import jgnash.util.NotNull;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

/**
 * Primary class for CSV export
 *
 * @author Craig Cavanaugh
 */
public class CsvExport {
    private static final char BYTE_ORDER_MARK = '\ufeff';

    private static final String SPACE = " ";

    private static final int INDENT = 4;

    private CsvExport() {
    }

    public static void exportAccountTree(@NotNull final Engine engine, @NotNull final Path path) {
        Objects.requireNonNull(engine);
        Objects.requireNonNull(path);

        // force a correct file extension
        final String fileName = FileUtils.stripFileExtension(path.toString()) + ".csv";

        final CSVFormat csvFormat = CSVFormat.EXCEL.withQuoteMode(QuoteMode.ALL);

        try (final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(Paths.get(fileName)),
                StandardCharsets.UTF_8);
             final CSVPrinter writer = new CSVPrinter(new BufferedWriter(outputStreamWriter), csvFormat)) {

            outputStreamWriter.write(BYTE_ORDER_MARK); // write UTF-8 byte order mark to the file for easier imports

            writer.printRecord(ResourceUtils.getString("Column.Account"), ResourceUtils.getString("Column.Code"),
                    ResourceUtils.getString("Column.Entries"), ResourceUtils.getString("Column.Balance"),
                    ResourceUtils.getString("Column.ReconciledBalance"), ResourceUtils.getString("Column.Currency"),
                    ResourceUtils.getString("Column.Type"));

            // Create a list sorted by depth and account code and then name if code is not specified
            final List<Account> accountList = engine.getAccountList();
            accountList.sort(Comparators.getAccountByTreePosition(Comparators.getAccountByCode()));

            final CurrencyNode currencyNode = engine.getDefaultCurrency();
            final LocalDate today = LocalDate.now();

            for (final Account account : accountList) {
                final String indentedName = SPACE.repeat(account.getDepth() * INDENT) + account.getName();
                final String balance = account.getTreeBalance(today, currencyNode).toPlainString();
                final String reconcileBalance = account.getReconciledTreeBalance().toPlainString();

                writer.printRecord(indentedName, String.valueOf(account.getAccountCode()), String.valueOf(account.getTransactionCount()),
                        balance, reconcileBalance, account.getCurrencyNode().getSymbol(), account.getAccountType().toString());
            }
        } catch (final IOException e) {
            Logger.getLogger(CsvExport.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    public static void exportAccount(@NotNull final Account account, @NotNull final LocalDate startDate,
                                     @NotNull final LocalDate endDate, @NotNull final Path path) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(startDate);
        Objects.requireNonNull(endDate);
        Objects.requireNonNull(path);

        // force a correct file extension
        final String fileName = FileUtils.stripFileExtension(path.toString()) + ".csv";

        final CSVFormat csvFormat = CSVFormat.EXCEL.withQuoteMode(QuoteMode.ALL);

        try (final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(Paths.get(fileName)),
                StandardCharsets.UTF_8);
             final CSVPrinter writer = new CSVPrinter(new BufferedWriter(outputStreamWriter), csvFormat)) {

            outputStreamWriter.write(BYTE_ORDER_MARK); // write UTF-8 byte order mark to the file for easier imports

            writer.printRecord(ResourceUtils.getString("Column.Account"), ResourceUtils.getString("Column.Num"),
                    ResourceUtils.getString("Column.Debit"), ResourceUtils.getString("Column.Credit"),
                    ResourceUtils.getString("Column.Balance"), ResourceUtils.getString("Column.Date"),
                    ResourceUtils.getString("Column.Timestamp"), ResourceUtils.getString("Column.Memo"),
                    ResourceUtils.getString("Column.Payee"), ResourceUtils.getString("Column.Clr"));

            // write the transactions
            final List<Transaction> transactions = account.getTransactions(startDate, endDate);

            final DateTimeFormatter dateTimeFormatter = DateUtils.getExcelDateFormatter();

            final DateTimeFormatter timestampFormatter = DateUtils.getExcelTimestampFormatter();

            for (final Transaction transaction : transactions) {
                final String date = dateTimeFormatter.format(transaction.getLocalDate());

                final String timeStamp = timestampFormatter.format(transaction.getTimestamp());

                final String credit = transaction.getAmount(account).compareTo(BigDecimal.ZERO) < 0 ? ""
                                              : transaction.getAmount(account).abs().toPlainString();

                final String debit = transaction.getAmount(account).compareTo(BigDecimal.ZERO) > 0 ? ""
                                             : transaction.getAmount(account).abs().toPlainString();

                final String balance = account.getBalanceAt(transaction).toPlainString();

                final String reconciled = transaction.getReconciled(account) == ReconciledState.NOT_RECONCILED
                                                  ? Boolean.FALSE.toString() : Boolean.TRUE.toString();

                writer.printRecord(account.getName(), transaction.getNumber(), debit, credit, balance, date, timeStamp,
                        transaction.getMemo(), transaction.getPayee(), reconciled);
            }
        } catch (final IOException e) {
            Logger.getLogger(CsvExport.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
