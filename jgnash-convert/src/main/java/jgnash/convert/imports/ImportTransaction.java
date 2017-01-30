/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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
package jgnash.convert.imports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jgnash.engine.Account;
import jgnash.util.NotNull;
import jgnash.util.Nullable;

/**
 * Common interface for imported transactions from OFX and mt940
 *
 * @author Craig Cavanaugh
 * @author Arnout Engelen
 * @author Nicolas Bouillon
 */
public class ImportTransaction implements Comparable<ImportTransaction> {

    private final String uuid = UUID.randomUUID().toString();

    private Account account;

    private BigDecimal amount = BigDecimal.ZERO;

    private String checkNumber = ""; // check number (?)

    @NotNull
    private LocalDate datePosted = LocalDate.now();

    @Nullable
    private LocalDate dateUser = null;

    private String memo = ""; // memo

    @NotNull
    private String payee = ""; // previously: 'name'

    private ImportState state = ImportState.NEW;

    private String transactionID;

    private String securityId;

    private String securityType;

    private BigDecimal units;

    private BigDecimal unitPrice;

    private BigDecimal commission;

    private boolean taxExempt = false;

    /**
     * @return returns the destination account
     */
    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    /**
     * Depending on the implementation a unique ID may be provided that can be used to detect
     * duplication of prior imported transactions.
     *
     * @return transaction id
     */
    public String getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(String transactionID) {
        this.transactionID = transactionID;
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

    public void setUnits(BigDecimal units) {
        this.units = units;
    }

    /**
     * Investment transaction unit price
     */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    /**
     * Investment transaction commission
     */
    public BigDecimal getCommission() {
        return commission;
    }

    public void setCommission(BigDecimal commission) {
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

    public void setSecurityType(String securityType) {
        this.securityType = securityType;
    }
}
