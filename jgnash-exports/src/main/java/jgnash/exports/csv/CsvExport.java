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
import jgnash.engine.Transaction;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

    private static char delimiter = ',';   // comma separated format

    private CsvExport() {
    }

    public static void exportAccount(final Account account, final Date startDate, final Date endDate, final String fileName) {

        if (account == null || startDate ==  null || endDate == null || fileName == null) {
            throw new RuntimeException();
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName)))) {

            // write the header
            writer.write("Account,Number,Debit,Credit,Balance,Date,Memo,Payee,Reconciled");
            writer.newLine();

            // write the transactions
            List<Transaction> transactions = account.getTransactions(startDate, endDate);

            for (Transaction transaction : transactions) {

                //writer.write(aPl.toString());
                writer.newLine();
            }
            writer.newLine();
        } catch (IOException e) {
            Logger.getLogger(CsvExport.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
