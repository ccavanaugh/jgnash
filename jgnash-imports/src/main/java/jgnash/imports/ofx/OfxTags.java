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
package jgnash.imports.ofx;

/**
 * Known OFX tags
 *
 * @author Craig Cavanaugh
 * @author Nicolas Bouillon
 * @version $Id: OfxTags.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public interface OfxTags {

    /**
     * Account ID
     */
    static String ACCTID = "ACCTID";

    /**
     * Account Type
     */
    static String ACCTTYPE = "ACCTTYPE";

    /**
     * Available balance
     */
    static String AVAILBAL = "AVAILBAL";

    /**
     * Balance Amount
     */
    static String BALAMT = "BALAMT";

    /**
     * Bank Account info
     */
    static String BANKACCTFROM = "BANKACCTFROM";

    /**
     * Credit Card info
     */
    static String CCACCTFROM = "CCACCTFROM";

    /**
     * Bank ID
     */
    static String BANKID = "BANKID";

    /**
     * Branch identifier. May be required for some non-US banks
     */
    static String BRANCHID = "BRANCHID";

    /**
     * Bank transaction list
     */
    static String BANKTRANLIST = "BANKTRANLIST";

    static String BROKERID = "BROKERID";

    /**
     * Check number
     */
    static String CHECKNUM = "CHECKNUM";

    static String CODE = "CODE";

    static String CURDEF = "CURDEF";

    static String CURRENCY = "CURRENCY";

    /**
     * Date of balance (LEDGERBA & AVAILBAL
     */
    static String DTASOF = "DTASOF";

    /**
     * End date of transaction list
     */
    static String DTEND = "DTEND";

    /**
     * Date posted
     */
    static String DTPOSTED = "DTPOSTED";

    /**
     * Date user initiated transaction
     */
    static String DTUSER = "DTUSER";

    static String DTSERVER = "DTSERVER";

    /**
     * Start date of transaction list
     */
    static String DTSTART = "DTSTART";

    static String FI = "FI";

    static String FID = "FID";

    /**
     * Financial Institution transaction id
     */
    static String FITID = "FITID";

    /**
     * Investment Account info
     */
    static String INVACCTFROM = "INVACCTFROM";

    static String LANGUAGE = "LANGUAGE";

    /**
     * Account balance
     */
    static String LEDGERBAL = "LEDGERBAL";

    /**
     * Transaction memo
     */
    static String MEMO = "MEMO";

    static String MESSAGE = "MESSAGE";

    /**
     * Transaction name
     */
    static String NAME = "NAME";

    static String OFX = "OFX";

    static String ORG = "ORG";

    static String ORIGCURRENCY = "ORIGCURRENCY";

    static String PAYEEID = "PAYEEID";

    static String REFNUM = "REFNUM";

    static String SEVERITY = "SEVERITY";

    /**
     * Accounting SIC code
     */
    static String SIC = "SIC";

    /**
     * Sign-on Message Set Aggregate
     */
    static String SIGNONMSGSRSV1 = "SIGNONMSGSRSV1";

    static String SONRS = "SONRS";

    static String STATUS = "STATUS";

    /**
     * Bank statement response aggregate
     */
    static String STMTRS = "STMTRS";

    /**
     * Credit Card statement response aggregate
     */
    static String CCSTMTRS = "CCSTMTRS";

    /**
     * Investment statement response aggregate
     */
    static String INVSTMTRS = "INVSTMTRS";

    /**
     * Bank Transaction
     */
    static String STMTTRN = "STMTTRN";

    static String STMTTRNRS = "STMTTRNRS";

    /**
     * Transaction amount
     */
    static String TRNAMT = "TRNAMT";

    /**
     * Transaction type
     */
    static String TRNTYPE = "TRNTYPE";

    /**
     * Client Assigned Globally Unique Transaction ID
     */
    static String TRNUID = "TRNUID";

    static String USERKEY = "USERKEY";

    /**
     * Bank Message Set Aggregate
     */
    static String BANKMSGSRSV1 = "BANKMSGSRSV1";

    static String CREDITCARDMSGSRSV1 = "CREDITCARDMSGSRSV1";

    /**
     * Intuit mucking up the OFX standard, Bank Id, In signon message
     */
    static String INTUBID = "INTU.BID";

    /**
     * Intuit mucking up the OFX standard, User Id, In signon message
     */
    static String INTUUSERID = "INTU.USERID";
}
