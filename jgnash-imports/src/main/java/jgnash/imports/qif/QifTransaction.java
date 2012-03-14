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
package jgnash.imports.qif;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

import jgnash.engine.Account;

/**
 * Transaction object for a QIF transaction
 *
 * @author Craig Cavanaugh
 * @version $Id: QifTransaction.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class QifTransaction {

    /**
     * Converted date
     */
    public Date date = new Date();
    /**
     * Original date before conversion
     */
    public String oDate;
    public BigDecimal amount = BigDecimal.ZERO;
    String status = null;
    String number;
    public String payee = null;
    public String memo = "";
    public String category = null;
    public Account _category = null;
    String U;
    String security;
    String price;
    String quantity;
    String type;
    String amountTrans;
    public ArrayList<QifSplitTransaction> splits = new ArrayList<>();
    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
    private ArrayList<String> address = new ArrayList<>();

    public void addSplit(QifSplitTransaction split) {
        splits.add(split);
    }

    public void addAddressLine(String line) {
        address.add(line);
    }

    public boolean hasSplits() {
        return !splits.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Payee: ").append(payee).append('\n');
        buf.append("Memo: ").append(memo).append('\n');
        buf.append("Category: ").append(category).append('\n');
        if (amount != null) {
            buf.append("Amount:").append(amount.toString()).append('\n');
        }

        buf.append("Date: ").append(date.toString()).append('\n');
        return buf.toString();
    }
}
