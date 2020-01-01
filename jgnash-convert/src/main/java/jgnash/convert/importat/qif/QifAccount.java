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

import java.util.List;
import java.util.Objects;

import jgnash.convert.importat.DateFormat;
import jgnash.convert.importat.ImportBank;

/**
 * @author Craig Cavanaugh
 */
public class QifAccount extends ImportBank<QifTransaction> {

    public String name;

    public String type;

    public String description = "";

    private DateFormat dateFormat = null;

    @Override
    public List<QifTransaction> getTransactions() {

        if (dateFormat == null) {
            setDateFormat(QifTransaction.determineDateFormat(super.getTransactions()));
        }

        reparseDates(getDateFormat());  // reparse the dates before returning

        return super.getTransactions();
    }

    public QifTransaction get(final int index) {
        return super.getTransactions().get(index);
    }

    @Override
    public String toString() {
        return "Name: " + name + '\n' + "Type: " + type + '\n' + "Description: " + description + '\n';
    }

    public void reparseDates(final DateFormat dateFormat) {
        Objects.requireNonNull(dateFormat);

        setDateFormat(dateFormat);

        for (final QifTransaction transaction: super.getTransactions()) {
            transaction.setDatePosted(QifTransaction.parseDate(transaction.oDate, dateFormat));
        }
    }

    public DateFormat getDateFormat() {
        if (dateFormat == null) {
            return DateFormat.US;
        }

        return dateFormat;
    }

    public void setDateFormat(final DateFormat dateFormat) {
        Objects.requireNonNull(dateFormat);

        this.dateFormat = dateFormat;
    }
}
