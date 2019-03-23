/*
 * Copyright (C) 2008 Arnout Engelen
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
package net.bzzt.swift.mt940.exporter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jgnash.convert.importat.ImportBank;
import jgnash.convert.importat.ImportTransaction;

import net.bzzt.swift.mt940.Mt940Entry;
import net.bzzt.swift.mt940.Mt940Entry.SollHabenKennung;
import net.bzzt.swift.mt940.Mt940File;
import net.bzzt.swift.mt940.Mt940Record;

/**
 * The Mt940 Exporter converts a parsed Mt940File to jGnash-specific
 * Double-entry Transaction objects.
 *
 * @author arnouten
 */
public class Mt940Exporter {
    private Mt940Exporter() {
    }

    public static ImportBank<ImportTransaction> convert(Mt940File file) {
        final ImportBank<ImportTransaction> importBank = new ImportBank<>();

        importBank.setTransactions(convertTransactions(file));

        return importBank;
    }

    /**
     * Convert an entire Mt940File to Transactions
     *
     * @param file file to import
     * @return list of import transactions
     */
    private static List<ImportTransaction> convertTransactions(final Mt940File file) {
        List<ImportTransaction> retVal = new ArrayList<>();
        for (Mt940Record record : file.getRecords()) {
            retVal.addAll(record.getEntries().stream().map(Mt940Exporter::convert).collect(Collectors.toList()));
        }
        return retVal;
    }

    /**
     * Convert a single Mt940-entry to a jGnash-Transaction
     *
     * @param entry Mt940Entry to convert
     * @return new import transaction
     */
    private static ImportTransaction convert(Mt940Entry entry) {
        BigDecimal amount;

        if (entry.getSollHabenKennung() == SollHabenKennung.CREDIT) {
            // The bank account is credited, so we gained income
            amount = entry.getBetrag();
        } else if (entry.getSollHabenKennung() == SollHabenKennung.DEBIT) {
            // The bank account is debited, so we made expenses
            // withdrawals have a negative 'amount'
            amount = BigDecimal.valueOf(0L).subtract(entry.getBetrag());
        } else {
            throw new UnsupportedOperationException("SollHabenKennung " + entry.getSollHabenKennung() + " not supported");
        }
        
        ImportTransaction tran = new ImportTransaction();
        tran.setAmount(amount);
        tran.setDatePosted(entry.getValutaDatum());
        tran.setMemo(entry.getMehrzweckfeld());
        tran.setAccount(null);

        return tran;
    }
}