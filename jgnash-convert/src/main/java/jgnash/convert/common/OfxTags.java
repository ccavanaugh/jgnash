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
    String ACCTID = "ACCTID";

    /**
     * Account Type
     */
    String ACCTTYPE = "ACCTTYPE";

    /**
     * Available balance
     */
    String AVAILBAL = "AVAILBAL";

    /**
     * Balance Amount
     */
    String BALAMT = "BALAMT";

    /**
     * Bank Account info
     */
    String BANKACCTFROM = "BANKACCTFROM";

    /**
     * Bank Account info for a transfer
     */
    String BANKACCTTO = "BANKACCTTO";

    /**
     * CASH Tag
     */
    String CASH = "CASH";

    /**
     * Credit Card info
     */
    String CCACCTFROM = "CCACCTFROM";

    /**
     * Credit Account info for a transfer
     */
    String CCACCTTO = "CCACCTTO";

    /**
     * Investment Account info
     */
    String INVACCTFROM = "INVACCTFROM";

    /**
     * Investment Account info for a transfer
     */
    String INVACCTTO = "INVACCTTO";

    /**
     * Bank ID
     */
    String BANKID = "BANKID";

    /**
     * Stock purchase
     */
    String BUYSTOCK = "BUYSTOCK";

    /**
     * Purchase type, normally "BUY"
     */
    String BUYTYPE = "BUYTYPE";

    /**
     * Buy other security type
     */
    String BUYOTHER = "BUYOTHER";

    /**
     * Buy mutual fund
     */
    String BUYMF = "BUYMF";

    /**
     * Sub-account that security or cash is being transferred to: CASH, MARGIN, SHORT, OTHER
     */
    String SUBACCTTO = "SUBACCTTO";

    /**
     * Sub-account that security or cash is being transferred from: CASH, MARGIN, SHORT, OTHER
     */
    String SUBACCTFROM = "SUBACCTFROM";

    /**
     * Investment account position list
     */
    String INVPOSLIST = "INVPOSLIST";

    /**
     * Investment account balance information
     */
    String INVBAL = "INVBAL";

    /**
     * Open investment transaction orders list
     */
    String INVOOLIST = "INVOOLIST";

    /**
     * Branch identifier. May be required for some non-US banks
     */
    String BRANCHID = "BRANCHID";

    /**
     * Bank transaction list
     */
    String BANKTRANLIST = "BANKTRANLIST";

    String INVTRANLIST = "INVTRANLIST";

    String INVTRAN = "INVTRAN";

    String INVBUY = "INVBUY";

    String INVSELL = "INVSELL";

    String BROKERID = "BROKERID";

    /**
     * Check number
     */
    String CHECKNUM = "CHECKNUM";

    String CODE = "CODE";

    String CURDEF = "CURDEF";

    String CURRENCY = "CURRENCY";

    /**
     * Checking account type
     * @see #ACCTTYPE
     */
    String CHECKING = "CHECKING";

    /**
     * Credit line account type
     * @see #ACCTTYPE
     */
    String CREDITLINE = "CREDITLINE";

    /**
     * Money market account type
     * @see #ACCTTYPE
     */
    String MONEYMRKT = "MONEYMRKT";

    /**
     * Savings account type
     * @see #ACCTTYPE
     */
    String SAVINGS = "SAVINGS";

    /**
     * Credit transaction
     * @see #TRNTYPE
     */
    String CREDIT = "CREDIT";

    /**
     * Debit transaction
     * @see #TRNTYPE
     */
    String DEBIT = "DEBIT";

    /**
     * Date of balance
     * @see #LEDGERBAL
     * @see #AVAILBAL
     */
    String DTASOF = "DTASOF";

    /**
     * End date of transaction list
     */
    String DTEND = "DTEND";

    /**
     * Date posted
     */
    String DTPOSTED = "DTPOSTED";

    /**
     * Date user initiated transaction
     */
    String DTUSER = "DTUSER";

    String DTSERVER = "DTSERVER";

    /**
     * Start date of transaction list
     */
    String DTSTART = "DTSTART";

    String DTTRADE = "DTTRADE";

    String DTSETTLE = "DTSETTLE";

    /**
     * Fees applied to trade, amount
     */
    String FEES = "FEES";

    String FI = "FI";

    String FID = "FID";

    /**
     * Financial Institution transaction id
     */
    String FITID = "FITID";


    String LANGUAGE = "LANGUAGE";

    /**
     * Account balance
     */
    String LEDGERBAL = "LEDGERBAL";

    /**
     * Transaction memo
     */
    String MEMO = "MEMO";

    /**
     * Chase bank mucking up the OFX standard
     */
    String CATEGORY = "CATEGORY";

    String MESSAGE = "MESSAGE";

    /**
     * Name of payee or transaction description, may be used exclusive of {@code PAYEE}
     * @see #PAYEE
     */
    String NAME = "NAME";

    String OFX = "OFX";

    String ORG = "ORG";

    String ORIGCURRENCY = "ORIGCURRENCY";

    /**
     * Name of payee, may be used exclusive of {@code NAME}
     * @see #NAME
     */
    String PAYEE = "PAYEE";

    String PAYEEID = "PAYEEID";

    /**
     * Indicates an amount withheld due to a penalty. Amount
     */
    String PENALTY = "PENALTY";

    String REFNUM = "REFNUM";

    String SEVERITY = "SEVERITY";

    String SECID = "SECID";

    /**
     * Accounting SIC code
     */
    String SIC = "SIC";

    /**
     * Sign-on Message Set Aggregate
     */
    String SIGNONMSGSRSV1 = "SIGNONMSGSRSV1";

    String SONRS = "SONRS";

    String STATUS = "STATUS";

    /**
     * Bank statement response aggregate
     */
    String STMTRS = "STMTRS";

    /**
     * Investment account bank transaction
     */
    String INVBANKTRAN = "INVBANKTRAN";

    /**
     * Credit Card statement response aggregate
     */
    String CCSTMTRS = "CCSTMTRS";

    /**
     * Investment statement response aggregate
     */
    String INVSTMTRS = "INVSTMTRS";

    /**
     * Bank Transaction
     */
    String STMTTRN = "STMTTRN";

    String STMTTRNRS = "STMTTRNRS";

    String SUBACCTSEC = "SUBACCTSEC";

    /**
     * Where did the money for the transaction come from or go to? CASH, MARGIN, SHORT, OTHER
     */
    String SUBACCTFUND = "SUBACCTFUND";

    /**
     * Sell a mutual fund
     */
    String SELLMF = "SELLMF";

    /**
     * Sell other type of security
     */
    String SELLOTHER = "SELLOTHER";

    /**
     * Sell a stock
     */
    String SELLSTOCK = "SELLSTOCK";

    String SELLTYPE = "SELLTYPE";

    String CCSTMTTRNRS = "CCSTMTTRNRS";

    String INVSTMTTRNRS = "INVSTMTTRNRS";

    String REINVEST = "REINVEST";

    String INCOME = "INCOME";

    String INCOMETYPE = "INCOMETYPE";

    /**
     * 401k loan id
     */
    String LOANID = "LOANID";

    /**
     * 401k loan principal
     */
    String LOANPRINCIPAL = "LOANPRINCIPAL";

    /**
     * 401k loan interest
     */
    String LOANINTEREST = "LOANINTEREST";

    /**
     * Must be one of the following: PRETAX, AFTERTAX, MATCH, PROFITSHARING, ROLLOVER, OTHERVEST, OTHERNONVEST
     */
    String INV401KSOURCE = "INV401KSOURCE";

    /**
     * For 401(k)accounts, date the funds for this transaction was obtained via payroll deduction, datetime
     */
    String DTPAYROLL = "DTPAYROLL";

    /**
     * For 401(k) accounts, indicates that this Buy was made with a prior year contribution. Boolean
     */
    String PRIORYEARCONTRIB = "PRIORYEARCONTRIB";

    /**
     * For 401(k) accounts, account balance aggregate
     */
    String INV401KBAL = "INV401KBAL";

    /**
     * For 401(k) accounts, account information aggregate
     */
    String INV401K = "INV401K";

    /**
     * Tax exempt status of an investment transactions
     */
    String TAXEXEMPT = "TAXEXEMPT";

    /**
     * Transaction amount
     */
    String TRNAMT = "TRNAMT";

    /**
     * Transaction type
     */
    String TRNTYPE = "TRNTYPE";

    /**
     * Client Assigned Globally Unique Transaction ID
     */
    String TRNUID = "TRNUID";

    /**
     * Total of the investment transaction (unit * unit price + commission)
     */
    String TOTAL = "TOTAL";

    //String USERKEY = "USERKEY";

    String UNIQUEID = "UNIQUEID";

    String UNIQUEIDTYPE = "UNIQUEIDTYPE";

    String UNITS = "UNITS";

    String UNITPRICE = "UNITPRICE";

    String COMMISSION = "COMMISSION";

    /**
     * Bank Message Set Aggregate
     */
    String BANKMSGSRSV1 = "BANKMSGSRSV1";

    String CREDITCARDMSGSRSV1 = "CREDITCARDMSGSRSV1";

    String INVSTMTMSGSRSV1 = "INVSTMTMSGSRSV1";

    String SECLISTMSGSRSV1 = "SECLISTMSGSRSV1";


    /**
     * Security Info
     */
    String STOCKINFO = "STOCKINFO";

    /**
     * Mutual fund information
     */
    String MFINFO = "MFINFO";

    /**
     * Security information
     */
    String SECINFO = "SECINFO";

    /**
     * Information about an Option
     */
    String OPTINFO = "OPTINFO";

    /**
     * Option type
     */
    String OPTTYPE = "OPTTYPE";

    /**
     * Strike price
     */
    String STRIKEPRICE = "STRIKEPRICE";

    /**
     * ISO-4217 3-letter currency identifier
     */
    String CURSYM = "CURSYM";

    /**
     * Ratio of <CURDEF> currency to <CURSYM> currency, in decimal notation, rate
     */
    String CURRATE = "CURRATE";

    /**
     * Security name, maximum of 120 characters
     */
    String SECNAME = "SECNAME";

    String TICKER = "TICKER";

    String SECLIST = "SECLIST";

    /**
     * Expiration date for an Option
     */
    String DTEXPIRE = "DTEXPIRE";

    /**
     * Number of shares per contract
     */
    String SHPERCTRCT = "SHPERCTRCT";

    /**
     * Asset class of the security
     */
    String ASSETCLASS = "ASSETCLASS";

    /**
     * Yield of the security
     */
    String YIELD = "YIELD";

    /**
     * Internal security identifier for the financial institution
     */
    String FIID = "FIID";

    /**
     * Security rating, maximum of 10 characters
     */
    String RATING = "RATING";

    /**
     * Intuit mucking up the OFX standard, Bank Id, In signon message
     */
    String INTUBID = "INTU.BID";

    /**
     * Intuit mucking up the OFX standard, User Id, In signon message
     */
    String INTUUSERID = "INTU.USERID";
}
