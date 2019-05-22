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
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.time.DateUtils;
import jgnash.util.FileUtils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

/**
 * Primary class for CSV export
 *
 * @author Craig Cavanaugh
 */
public class CsvExport {

    private CsvExport() {
    }

    public static void exportAccount(final Account account, final LocalDate startDate, final LocalDate endDate, final File file) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(startDate);
        Objects.requireNonNull(endDate);
        Objects.requireNonNull(file);

        // force a correct file extension
        final String fileName = FileUtils.stripFileExtension(file.getAbsolutePath()) + ".csv";

        final CSVFormat csvFormat = CSVFormat.EXCEL.withQuoteMode(QuoteMode.ALL);

        try (final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(Paths.get(fileName)),
                StandardCharsets.UTF_8);
             final CSVPrinter writer = new CSVPrinter(new BufferedWriter(outputStreamWriter), csvFormat)) {

            outputStreamWriter.write('\ufeff'); // write UTF-8 byte order mark to the file for easier imports

            writer.printRecord("Account", "Number", "Debit", "Credit", "Balance", "Date", "Timestamp",
                    "Memo", "Payee", "Reconciled");

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
