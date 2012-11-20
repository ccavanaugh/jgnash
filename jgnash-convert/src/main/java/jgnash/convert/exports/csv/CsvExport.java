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
package jgnash.convert.exports.csv;

import jgnash.engine.Account;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.util.FileUtils;

import java.io.*;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Primary class for CSV export
 *
 * @author Craig Cavanaugh
 */
public class CsvExport {

    private CsvExport() {
    }

    public static void exportAccount(final Account account, final Date startDate, final Date endDate, final File file) {

        if (account == null || startDate == null || endDate == null || file == null) {
            throw new RuntimeException();
        }

        // force a correct file extension
        final String fileName = FileUtils.stripFileExtension(file.getAbsolutePath()) + ".csv";

        try (AutoCloseableCSVWriter writer = new AutoCloseableCSVWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName))))) {
            writer.writeNextRow("Account","Number","Debit","Credit","Balance","Date","Memo","Payee","Reconciled");

            // write the transactions
            final List<Transaction> transactions = account.getTransactions(startDate, endDate);

            // request locale specific date format and force to a 4 digit year format
            final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());

            if (dateFormat instanceof SimpleDateFormat) {
                String datePattern = ((SimpleDateFormat) dateFormat).toPattern().replaceAll("y+", "yyyy");
                ((SimpleDateFormat) dateFormat).applyPattern(datePattern);
            }

            for (Transaction transaction : transactions) {

                String date = dateFormat.format(transaction.getDate());
                String credit = transaction.getAmount(account).compareTo(BigDecimal.ZERO) == -1 ? "" : transaction.getAmount(account).abs().toPlainString();
                String debit = transaction.getAmount(account).compareTo(BigDecimal.ZERO) == 1 ? "" : transaction.getAmount(account).abs().toPlainString();

                String balance = account.getBalanceAt(transaction).toPlainString();
                String reconciled = transaction.getReconciled(account) == ReconciledState.NOT_RECONCILED ? Boolean.FALSE.toString() : Boolean.TRUE.toString();

                writer.writeNextRow(account.getName(), transaction.getNumber(), debit, credit, balance, date, transaction.getMemo(), transaction.getPayee(), reconciled);
            }
        } catch (IOException e) {
            Logger.getLogger(CsvExport.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
