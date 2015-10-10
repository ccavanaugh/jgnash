/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.convert.imports.ofx;

import jgnash.convert.imports.ImportTransaction;

/**
 * OFX Transaction object
 *
 * @author Craig Cavanaugh
 *
 */
public class OfxTransaction extends ImportTransaction {

    public String transactionType;

    public String sic; // automatic category assignment

    public String refNum;

    public String payeeId;

    public String currency;

    /*
     *
     * <STMTTRN>
     *   <TRNTYPE>DIRECTDEBIT
     *   <DTPOSTED>20060612120000[0:GMT]
     *   <TRNAMT>-11.45
     *   <FITID>1014005003
     *   <SIC>000000
     *   <NAME>ACH - Debit
     *   <MEMO>WAL-MART 7 ECA PURCHASE 1021 FORTIN
     * </STMTTRN>
     *
     * <STMTTRN>
     *   <TRNTYPE>OTHER
     *   <DTPOSTED>20060721120000[0:GMT]
     *   <TRNAMT>-5.27
     *   <FITID>1023720039
     *   <SIC>000000
     *   <NAME>ATM/DEBIT WITHDRAWAL
     *   <MEMO>POS PURCHASE LOWE'S
     * </STMTTRN>
     */
    @Override
    public String toString() {
        return transactionType + ", " +
                getDatePosted() + ", " +
                getAmount() + ", " +
                getTransactionID() + ", " +
                sic + ", " +
                getPayee() + ", " +
                getMemo() + ", " +
                getCheckNumber() + ", " +
                refNum + ", " +
                payeeId + ", " +
                currency;
    }
}
