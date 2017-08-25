/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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
package jgnash.convert.exports.csv;

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
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.util.FileUtils;

import static java.time.temporal.ChronoField.*;

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

        try (final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(Paths.get(fileName)),
                StandardCharsets.UTF_8);

             final AutoCloseableCSVWriter writer = new AutoCloseableCSVWriter(new BufferedWriter(outputStreamWriter))) {

            outputStreamWriter.write('\ufeff'); // write UTF-8 byte order mark to the file for easier imports

            writer.writeNextRow("Account", "Number", "Debit", "Credit", "Balance", "Date", "Timestamp",
                    "Memo", "Payee", "Reconciled");

            // write the transactions
            final List<Transaction> transactions = account.getTransactions(startDate, endDate);

            final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                    .appendValue(YEAR, 4)
                    .appendValue(MONTH_OF_YEAR, 2)
                    .appendValue(DAY_OF_MONTH, 2)
                    .toFormatter();

            final DateTimeFormatter timestampFormatter = new DateTimeFormatterBuilder()
                    .appendValue(YEAR, 4).appendLiteral('-').appendValue(MONTH_OF_YEAR, 2)
                    .appendLiteral('-').appendValue(DAY_OF_MONTH, 2).appendLiteral(' ')
                    .appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2)
                    .appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2)
                    .toFormatter();

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

                writer.writeNextRow(account.getName(), transaction.getNumber(), debit, credit, balance, date, timeStamp,
                        transaction.getMemo(), transaction.getPayee(), reconciled);
            }
        } catch (final IOException e) {
            Logger.getLogger(CsvExport.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
