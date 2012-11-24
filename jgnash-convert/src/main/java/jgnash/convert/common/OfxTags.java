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
package jgnash.convert.common;

/**
 * Known OFX tags
 *
 * @author Craig Cavanaugh
 * @author Nicolas Bouillon
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
     * Investment Account info
     */
    static String INVACCTFROM = "INVACCTFROM";

    /**
     * Bank ID
     */
    static String BANKID = "BANKID";

    /**
     * Stock purchase
     */
    static String BUYSTOCK = "BUYSTOCK";

    static String BUYTYPE = "BUYTYPE";

    /**
     * Branch identifier. May be required for some non-US banks
     */
    static String BRANCHID = "BRANCHID";

    /**
     * Bank transaction list
     */
    static String BANKTRANLIST = "BANKTRANLIST";

    static String INVTRANLIST = "INVTRANLIST";

    static String INVTRAN = "INVTRAN";

    static String INVBUY = "INVBUY";

    static String INVSELL = "INVSELL";

    static String BROKERID = "BROKERID";

    /**
     * Check number
     */
    static String CHECKNUM = "CHECKNUM";

    static String CODE = "CODE";

    static String CURDEF = "CURDEF";

    static String CURRENCY = "CURRENCY";

    /**
     * Checking account type
     * @see #ACCTTYPE
     */
    static String CHECKING = "CHECKING";

    /**
     * Credit line account type
     * @see #ACCTTYPE
     */
    static String CREDITLINE = "CREDITLINE";

    /**
     * Money market account type
     * @see #ACCTTYPE
     */
    static String MONEYMRKT = "MONEYMRKT";

    /**
     * Savings account type
     * @see #ACCTTYPE
     */
    static String SAVINGS = "SAVINGS";

    /**
     * Credit transaction
     * @see #TRNTYPE
     */
    static String CREDIT = "CREDIT";

    /**
     * Debit transaction
     * @see #TRNTYPE
     */
    static String DEBIT = "DEBIT";

    /**
     * Date of balance
     * @see #LEDGERBAL
     * @see #AVAILBAL
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

    static String DTTRADE = "DTTRADE";

    static String DTSETTLE = "DTSETTLE";

    static String FI = "FI";

    static String FID = "FID";

    /**
     * Financial Institution transaction id
     */
    static String FITID = "FITID";


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
     * Name of payee or transaction description, may be used exclusive of <code>PAYEE</code>
     * @see #PAYEE
     */
    static String NAME = "NAME";

    static String OFX = "OFX";

    static String ORG = "ORG";

    static String ORIGCURRENCY = "ORIGCURRENCY";

    /**
     * Name of payee, may be used exclusive of <code>NAME</code>
     * @see #NAME
     */
    static String PAYEE = "PAYEE";

    static String PAYEEID = "PAYEEID";

    static String REFNUM = "REFNUM";

    static String SEVERITY = "SEVERITY";

    static String SECID = "SECID";

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
     * Investment account bank transaction
     */
    static String INVBANKTRAN = "INVBANKTRAN";

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

    static String SUBACCTSEC = "SUBACCTSEC";

    static String SUBACCTFUND = "SUBACCTFUND";

    static String SELLSTOCK = "SELLSTOCK";

    static String SELLTYPE = "SELLTYPE";

    static String CCSTMTTRNRS = "CCSTMTTRNRS";

    static String INVSTMTTRNRS = "INVSTMTTRNRS";

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

    static String TOTAL = "TOTAL";

    static String USERKEY = "USERKEY";

    static String UNIQUEID = "UNIQUEID";

    static String UNIQUEIDTYPE = "UNIQUEIDTYPE";

    static String UNITS = "UNITS";

    static String UNITPRICE = "UNITPRICE";

    static String COMMISSION = "COMMISSION";

    /**
     * Bank Message Set Aggregate
     */
    static String BANKMSGSRSV1 = "BANKMSGSRSV1";

    static String CREDITCARDMSGSRSV1 = "CREDITCARDMSGSRSV1";

    static String INVSTMTMSGSRSV1 = "INVSTMTMSGSRSV1";

    /**
     * Intuit mucking up the OFX standard, Bank Id, In signon message
     */
    static String INTUBID = "INTU.BID";

    /**
     * Intuit mucking up the OFX standard, User Id, In signon message
     */
    static String INTUUSERID = "INTU.USERID";
}
