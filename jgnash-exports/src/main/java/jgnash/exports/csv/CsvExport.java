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
package jgnash.exports.csv;

import jgnash.engine.Account;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.text.CommodityFormat;

import java.io.*;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Primary class for CSV export
 *
 * @author Craig Cavanaugh
 */
public class CsvExport {

    private static char DELIMITER = ',';   // comma separated format

    private CsvExport() {
    }

    // TODO escape numeric data (eliminate commas), escape quotes, fix debit and credit columns
    public static void exportAccount(final Account account, final Date startDate, final Date endDate, final File file) {

        if (account == null || startDate ==  null || endDate == null || file == null) {
            throw new RuntimeException();
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {

            // write the header
            writer.write("Account,Number,Debit,Credit,Balance,Date,Memo,Payee,Reconciled");
            writer.newLine();

            // write the transactions
            List<Transaction> transactions = account.getTransactions(startDate, endDate);

            String pattern = "{1}{0}{2}{0}{3}{0}{4}{0}{5}{0}{6,date,short}{0}{7}{0}{8}{0}{9}";

            NumberFormat numberFormat = CommodityFormat.getShortNumberFormat(account.getCurrencyNode());

            for (Transaction transaction : transactions) {

                String credit = transaction.getAmount(account).compareTo(BigDecimal.ZERO) == 0 ? "" :  numberFormat.format(transaction.getAmount(account));
                String debit = transaction.getAmount(account).compareTo(BigDecimal.ZERO) == 0 ? "" :  numberFormat.format(transaction.getAmount(account));

                String balance = numberFormat.format(account.getBalanceAt(transaction));
                String reconciled = transaction.getReconciled(account) == ReconciledState.NOT_RECONCILED ? Boolean.FALSE.toString() : Boolean.TRUE.toString();

                writer.write(MessageFormat.format(pattern, DELIMITER, account.getName(), transaction.getNumber(), debit,
                        credit, balance, transaction.getDate(), transaction.getMemo(), transaction.getPayee(), reconciled));
                writer.newLine();
            }
            writer.newLine();
        } catch (IOException e) {
            Logger.getLogger(CsvExport.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
