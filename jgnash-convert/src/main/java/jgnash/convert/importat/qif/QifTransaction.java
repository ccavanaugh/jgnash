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
package jgnash.convert.importat.qif;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jgnash.convert.importat.DateFormat;
import jgnash.convert.importat.ImportTransaction;

/**
 * Transaction object for a QIF transaction
 *
 * @author Craig Cavanaugh
 */
public class QifTransaction extends ImportTransaction {

    private static final Pattern DATE_DELIMITER_PATTERN = Pattern.compile("[/'.-]");

    /**
     * Original date before conversion
     */
    String oDate;

    String status = null;

    public String category = null;

    String security;
    String price;
    String quantity;
    String amountTrans;

    public final ArrayList<QifSplitTransaction> splits = new ArrayList<>();

    void addSplit(QifSplitTransaction split) {
        splits.add(split);
    }

    boolean hasSplits() {
        return !splits.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Payee: ").append(getPayee()).append('\n');
        buf.append("Memo: ").append(getMemo()).append('\n');
        buf.append("Category: ").append(category).append('\n');
        if (getAmount() != null) {
            buf.append("Amount:").append(getAmount()).append('\n');
        }

        buf.append("Date: ").append(getDatePosted()).append('\n');
        return buf.toString();
    }

    static DateFormat determineDateFormat(final Collection<QifTransaction> transactions) {
        Objects.requireNonNull(transactions);

        DateFormat dateFormat = DateFormat.US;   // US date is assumed

        for (final QifTransaction transaction : transactions) {

            // protect against a transaction missing a date Github issue #30
            if (transaction.oDate != null) {
                int zero;
                int one;
                //int two;

                final String[] chunks = QifTransaction.DATE_DELIMITER_PATTERN.split(transaction.oDate);

                zero = Integer.parseInt(chunks[0].trim());
                one = Integer.parseInt(chunks[1].trim());
                //two = Integer.parseInt(chunks[2].trim());

                if (zero > 12 && one <= 12) {   // must have a EU date format
                    dateFormat = DateFormat.EU;
                    break;
                }
            }
        }

        return dateFormat;
    }

    /**
     * Converts a string into a data object
     * <p>
     * {@code
     * "6/21' 1" -> 6/21/2001
     * "6/21'01" -> 6/21/2001
     * "9/18'2001 -> 9/18/2001
     * "06/21/2001" -> "06/21/01"
     * "3.26.03" -> German version of quicken format "03-26-2003"
     * MSMoney format "1.1.2005"
     * kmymoney2 20.1.94
     * European dd/mm/yyyy
     * 21/2/07 -> 02/21/2007 UK
     * Quicken 2007 D15/2/07
     * }``
     *
     * @param sDate  String QIF date to parse
     * @param format String identifier of format to parse
     * @return Returns parsed date and current date if an error occurs
     */
    static LocalDate parseDate(final String sDate, final DateFormat format) throws java.time.DateTimeException {

        int month = 0;
        int day = 0;
        int year = 0;

        final String[] chunks = DATE_DELIMITER_PATTERN.split(sDate);

        try {
            switch (format) {
                case US:
                    month = Integer.parseInt(chunks[0].trim());
                    day = Integer.parseInt(chunks[1].trim());
                    year = Integer.parseInt(chunks[2].trim());
                    break;
                case EU:
                    day = Integer.parseInt(chunks[0].trim());
                    month = Integer.parseInt(chunks[1].trim());
                    year = Integer.parseInt(chunks[2].trim());
                    break;
            }
        } catch (final NumberFormatException e) {
            Logger.getLogger(QifUtils.class.getName()).severe(e.toString());
        }

        if (year < 100) {
            if (year < 29) {
                year += 2000;
            } else {
                year += 1900;
            }
        }

        try {
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            Logger.getLogger(QifUtils.class.getName()).severe("Invalid date format specified");
            return LocalDate.now();
        }
    }
}
