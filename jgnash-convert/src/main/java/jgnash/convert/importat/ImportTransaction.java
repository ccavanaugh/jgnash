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
package jgnash.convert.importat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jgnash.engine.Account;
import jgnash.engine.TransactionType;
import jgnash.util.NotNull;
import jgnash.util.Nullable;

/**
 * Common interface for importing transactions from OFX, QIF, and mt940
 *
 * @author Craig Cavanaugh
 * @author Arnout Engelen
 * @author Nicolas Bouillon
 */
public class ImportTransaction implements Comparable<ImportTransaction> {

    private final String uuid = UUID.randomUUID().toString();

    /**
     * The destination account
     */
    private Account account;

    /**
     * Account for dividends and gains/losses from an investment transaction
     */
    private Account gainsAccount;

    /**
     * Account for investment expenses
     */
    private Account feesAccount;

    private BigDecimal amount = BigDecimal.ZERO;

    private String checkNumber = ""; // check number (?)

    @NotNull
    private LocalDate datePosted = LocalDate.now();

    @Nullable
    private LocalDate dateUser = null;

    private String memo = ""; // memo

    @NotNull
    private String payee = "";

    // OFX
    private String payeeId;

    private ImportState state = ImportState.NEW;

    // OFX, Financial Institution transaction ID
    private String FITID;

    // OFX
    private String securityId;

    // OFX
    private String securityType;

    private BigDecimal units = BigDecimal.ZERO;

    private BigDecimal unitPrice = BigDecimal.ZERO;

    private BigDecimal commission = BigDecimal.ZERO;

    private BigDecimal fees = BigDecimal.ZERO;

    // OFX, Type of income for investment transaction
    private String incomeType;

    private boolean taxExempt = false;

    // OFX
    private TransactionType transactionType = TransactionType.SINGLENTRY;  // single entry by default

    // OFX
    private String transactionTypeDescription;

    // OFX
    private String SIC;

    // OFX
    private String refNum;

    // OFX
    private String subAccount;

    private String currency;

    // OFX, transfer account id
    private String accountTo;

    /**
     * @return returns the destination account
     */
    public Account getAccount() {
        return account;
    }

    public void setAccount(final Account account) {
        this.account = account;
    }

    /**
     * Depending on the implementation a unique ID may be provided that can be used to detect
     * duplication of prior imported transactions.
     *
     * @return transaction id
     */
    public String getFITID() {
        return FITID;
    }

    public void setFITID(String FITID) {
        this.FITID = FITID;
    }

    /**
     * Deposits get positive 'amounts', withdrawals negative
     *
     * @return transaction amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @NotNull
    public LocalDate getDatePosted() {
        return datePosted;
    }

    public void setDatePosted(@NotNull LocalDate datePosted) {
        this.datePosted = datePosted;
    }

    /**
     * Date user initiated the transaction, optional, may be null
     *
     * @return date transaction was initiated
     */
    @Nullable
    public LocalDate getDateUser() {
        return dateUser;
    }

    public void setDateUser(@Nullable LocalDate dateUser) {
        this.dateUser = dateUser;
    }

    public String getCheckNumber() {
        return checkNumber;
    }

    public void setCheckNumber(final String checkNumber) {
        this.checkNumber = checkNumber;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public ImportState getState() {
        return state;
    }

    public void setState(ImportState state) {
        this.state = state;
    }

    @NotNull
    public String getPayee() {
        return payee;
    }

    public void setPayee(@NotNull String payee) {
        Objects.requireNonNull(payee);

        this.payee = payee;
    }

    public String getSecurityId() {
        return securityId;
    }

    public void setSecurityId(String securityId) {
        this.securityId = securityId;
    }

    @Override
    public int compareTo(@NotNull final ImportTransaction importTransaction) {
        if (importTransaction == this) {
            return 0;
        }

        int result = getDatePosted().compareTo(importTransaction.getDatePosted());
        if (result != 0) {
            return result;
        }

        result = payee.compareTo(importTransaction.payee);
        if (result != 0) {
            return result;
        }

        return Integer.compare(hashCode(), importTransaction.hashCode());
    }

    @Override
    public boolean equals(final Object that) {
        if (this == that) {
            return true;
        }

        if (that == null || getClass() != that.getClass()) {
            return false;
        }

        return Objects.equals(uuid, ((ImportTransaction) that).uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    /**
     * Investment transaction units
     */
    public BigDecimal getUnits() {
        return units;
    }

    public void setUnits(final BigDecimal units) {
        this.units = units;
    }

    /**
     * Investment transaction unit price
     */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(final BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    /**
     * Investment transaction commission
     */
    public BigDecimal getCommission() {
        return commission;
    }

    public void setCommission(final BigDecimal commission) {
        this.commission = commission;
    }

    public boolean isTaxExempt() {
        return taxExempt;
    }

    public void setTaxExempt(boolean taxExempt) {
        this.taxExempt = taxExempt;
    }

    public String getSecurityType() {
        return securityType;
    }

    public void setSecurityType(final String securityType) {
        this.securityType = securityType;
    }

    public boolean isInvestmentTransaction() {
        return getSecurityId() != null;
    }

    public String getIncomeType() {
        return incomeType;
    }

    public void setIncomeType(String incomeType) {
        this.incomeType = incomeType;
    }

    @NotNull public BigDecimal getFees() {
        return fees;
    }

    public void setFees(@NotNull final BigDecimal fees) {
        this.fees = fees;
    }

    /**
     * The parser may establish a transaction type when imported.
     *
     * @return {@code TransactionType}
     */
    @NotNull
    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(@NotNull final TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * OFX defines descriptive transaction types
     */
    public String getTransactionTypeDescription() {
        return transactionTypeDescription;
    }

    public void setTransactionTypeDescription(final String transactionTypeDescription) {
        this.transactionTypeDescription = transactionTypeDescription;
    }

    /**
     * Standard Industry Code
     * <p>
     * Could be use used for automatic expense and income assignment.  Typically a 4 digit numeric, but OFX allows 6
     */
    public String getSIC() {
        return SIC;
    }

    public void setSIC(String SIC) {
        this.SIC = SIC;
    }

    /**
     * Reference number that uniquely identifies the transaction. May be used in
     * addition to or instead of a {@link #getCheckNumber()}
     */
    public String getRefNum() {
        return refNum;
    }

    public void setRefNum(String refNum) {
        this.refNum = refNum;
    }

    /**
     * Some OFX based systems will assign an ID to a Payee.
     * <p>
     * The ID would correspond to a Payee List identified by <PAYEELSTID> (not implemented)
     */
    public String getPayeeId() {
        return payeeId;
    }

    public void setPayeeId(String payeeId) {
        this.payeeId = payeeId;
    }

    /**
     * The sub-account for cash transfer, typically CASH, but could be MARGIN, SHORT, or OTHER
     * <p>
     * <SUBACCTFROM>, <SUBACCTFUND>, <SUBACCTSEC>, <SUBACCTTO>
     */
    public String getSubAccount() {
        return subAccount;
    }

    public void setSubAccount(String subAccount) {
        this.subAccount = subAccount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Account for gains or losses from an investment transaction
     */
    public Account getGainsAccount() {
        return gainsAccount;
    }

    public void setGainsAccount(final Account gainsAccount) {
        this.gainsAccount = gainsAccount;
    }

    /**
     * Account for investment expenses
     */
    public Account getFeesAccount() {
        return feesAccount;
    }

    public void setFeesAccount(Account feesAccount) {
        this.feesAccount = feesAccount;
    }

    @NotNull
    public String getToolTip() {
        if (isInvestmentTransaction()) {
            return units.toString() + " @ " + unitPrice.toString();
        }
        return "";
    }

    @Override
    public String toString() {
        return getTransactionTypeDescription() + ", " +
                getTransactionType() + ", " +
                getDatePosted() + ", " +
                getAmount() + ", " +
                getFITID() + ", " +
                getSIC() + ", " +
                getPayee() + ", " +

                getMemo() + ", " +
                getCheckNumber() + ", " +
                getRefNum() + ", " +
                getPayeeId() + ", " +
                getCurrency();
    }

    public String getAccountTo() {
        return accountTo;
    }

    public void setAccountTo(String accountTo) {
        this.accountTo = accountTo;
    }
}
