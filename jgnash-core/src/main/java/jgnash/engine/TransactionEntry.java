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
package jgnash.engine;

import jgnash.util.EncodeDecode;
import jgnash.util.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;

/**
 * Transaction Entry
 * <p>
 * Each Transaction entry has an amount for the credit and debit side of the transaction. When the debit and credit
 * account has the same currency, one amount will be the negated value of the other. If the credit and debit accounts do
 * not have the same currency then the amounts will be different to represent the exchanged value at the time of the
 * transaction.
 * <p>
 * If the entry is to be used as an "single entry / adjustment" transaction, then the credit and debit account must be
 * set to the same account and the credit and debit amounts must be set to the same value.
 *
 * @author Craig Cavanaugh
 */
@Entity
public class TransactionEntry implements Comparable<TransactionEntry>, Cloneable, Serializable {

    /**
     * Cached hashcode.
     */
    private volatile transient int hashCode = 0;

    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private long id;

    @Enumerated(EnumType.STRING)
    private TransactionTag transactionTag = TransactionTag.BANK;

    /**
     * Account with balance being decreased.
     */
    @ManyToOne
    private Account debitAccount;

    /**
     * Account with balance being increased.
     */
    @ManyToOne
    private Account creditAccount;

    @Column(precision = 22, scale = 4)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(precision = 22, scale = 4)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    /**
     * Reconciled state of the transaction.
     */
    @Enumerated(EnumType.STRING)
    private ReconciledState creditReconciled = ReconciledState.NOT_RECONCILED;

    /**
     * Reconciled state of the debit side of the transaction.
     */
    @Enumerated(EnumType.STRING)
    private ReconciledState debitReconciled = ReconciledState.NOT_RECONCILED;

    /**
     * Memo for this entry.
     */
    @Column(columnDefinition = "VARCHAR(1024)")
    private String memo = "";

    /**
     * Field for user defined tags.
     */
    @Column(columnDefinition = "VARCHAR(2048)")
    private String customTags;

    /**
     * Public constructor.
     */
    public TransactionEntry() {
    }

    /**
     * Simple constructor for a single entry transaction.
     *
     * @param account account for the transaction
     * @param amount  amount for the transaction
     */
    public TransactionEntry(final Account account, final BigDecimal amount) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(amount);

        creditAccount = account;
        debitAccount = account;

        creditAmount = amount;
        debitAmount = amount;
    }

    /**
     * Simple constructor for a double entry transaction.
     *
     * @param creditAccount credit account for the transaction
     * @param debitAccount  debit account for the transaction
     * @param amount        amount for the transaction
     */
    TransactionEntry(final Account creditAccount, final Account debitAccount, final BigDecimal amount) {
        Objects.requireNonNull(creditAccount);
        Objects.requireNonNull(debitAccount);
        Objects.requireNonNull(amount);

        this.creditAccount = creditAccount;
        this.debitAccount = debitAccount;

        setAmount(amount.abs());
    }

    /**
     * Simple constructor for a double entry transaction with exchange rate.
     *
     * @param creditAccount credit account for the transaction
     * @param debitAccount  debit account for the transaction
     * @param creditAmount  amount for the transaction
     * @param debitAmount   amount for the transaction
     */
    TransactionEntry(@NotNull final Account creditAccount, @NotNull final Account debitAccount,
                     @NotNull final BigDecimal creditAmount, @NotNull final BigDecimal debitAmount) {
        Objects.requireNonNull(creditAccount);
        Objects.requireNonNull(debitAccount);
        Objects.requireNonNull(creditAmount);
        Objects.requireNonNull(debitAmount);

        assert creditAmount.signum() == 1 && debitAmount.signum() == -1;

        this.creditAccount = creditAccount;
        this.debitAccount = debitAccount;

        this.creditAmount = creditAmount;
        this.debitAmount = debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public BigDecimal getAmount(@NotNull final Account account) {
        Objects.requireNonNull(account);

        if (account.equals(creditAccount)) {
            return creditAmount;
        } else if (account.equals(debitAccount)) {
            return debitAmount;
        } else {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Shortcut method to set credit and debit amounts.
     *
     * @param amount credit amount of the transaction
     */
    public final void setAmount(@NotNull final BigDecimal amount) {
        Objects.requireNonNull(amount);

        assert amount.signum() >= 0;

        creditAmount = amount;
        debitAmount = amount.negate();
    }

    public Account getCreditAccount() {
        return creditAccount;
    }

    ReconciledState getCreditReconciled() {
        return creditReconciled;
    }

    public Account getDebitAccount() {
        return debitAccount;
    }

    ReconciledState getDebitReconciled() {
        return debitReconciled;
    }

    @NotNull
    public String getMemo() {
        return memo;
    }

    public ReconciledState getReconciled(final Account account) {
        if (account == getDebitAccount()) {
            return debitReconciled;
        } else if (account == getCreditAccount()) {
            return creditReconciled;
        }
        //log.warning("Invalid account!: " + account.getPathName());
        return ReconciledState.NOT_RECONCILED;
    }

    public void setCreditAmount(final BigDecimal creditAmount) {
        Objects.requireNonNull(creditAmount);

        this.creditAmount = creditAmount;
    }

    public void setCreditAccount(final Account creditAccount) {
        this.creditAccount = creditAccount;
    }

    void setCreditReconciled(@NotNull final ReconciledState creditReconciled) {
        Objects.requireNonNull(creditReconciled);

        this.creditReconciled = creditReconciled;
    }

    public void setDebitAccount(final Account debitAccount) {
        this.debitAccount = debitAccount;
    }

    void setDebitReconciled(@NotNull final ReconciledState debitReconciled) {
        Objects.requireNonNull(debitReconciled);

        this.debitReconciled = debitReconciled;
    }

    /**
     * Sets the memo for the entry.
     *
     * @param memo new memo
     */
    public void setMemo(final String memo) {
        if (memo != null) {
            this.memo = memo;
        }
    }

    public void setReconciled(final Account account, final ReconciledState reconciled) {
        if (account.equals(getCreditAccount())) {
            setCreditReconciled(reconciled);
        } else if (account.equals(getDebitAccount())) {
            setDebitReconciled(reconciled);
        }
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(@NotNull final BigDecimal debitAmount) {
        Objects.requireNonNull(debitAmount);

        this.debitAmount = debitAmount;
    }

    @Override
    public int compareTo(@NotNull final TransactionEntry entry) {

        if (this == entry) {
            return 0;
        }

        int result = memo.compareTo(entry.getMemo());

        if (result != 0) {
            return result;
        }

        result = transactionTag.compareTo(entry.transactionTag);

        if (result != 0) {
            return result;
        }

        if (hashCode() > entry.hashCode()) {
            return 1;
        }
        return -1;
    }

    /**
     * Check to determine is this is a single entry transaction.
     *
     * @return {@code true} if this is a single entry TransactionEntry
     */
    boolean isSingleEntry() {
        return creditAccount.equals(debitAccount) && creditAmount.equals(debitAmount);
    }

    /**
     * Returns true if multiple currencies are being used for this entry.
     * <p>
     * If the credit and debit accounts have differing currencies, then unless
     * the currencies are equal in value, the credit and debit amounts should be different.
     *
     * @return {@code true} if this is a multi-currency transaction entry
     */
    public boolean isMultiCurrency() {
        return !creditAccount.getCurrencyNode().equals(debitAccount.getCurrencyNode());
    }

    public void setTransactionTag(final TransactionTag transactionTag) {
        Objects.requireNonNull(transactionTag);

        this.transactionTag = transactionTag;

    }

    public TransactionTag getTransactionTag() {
        return transactionTag;
    }

    /**
     * Returns a delimited string of user defined customTags applied to the transaction.
     *
     * @return custom tags or an empty collection if not set
     */
    @SuppressWarnings("unused")
    public Collection<String> getCustomTags() {
        return EncodeDecode.decodeStringCollection(customTags);
    }

    @SuppressWarnings("unused")
    public void setCustomTags(final Collection<String> strings) {
        customTags = EncodeDecode.encodeStringCollection(strings);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        final TransactionEntry e = (TransactionEntry) super.clone();
        e.id = 0; // clones id must be reset

        return e;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactionEntry that = (TransactionEntry) o;
        return Objects.equals(transactionTag, that.transactionTag) &&
                Objects.equals(debitAccount, that.debitAccount) &&
                Objects.equals(creditAccount, that.creditAccount) &&
                Objects.equals(creditAmount, that.creditAmount) &&
                Objects.equals(debitAmount, that.debitAmount) &&
                Objects.equals(creditReconciled, that.creditReconciled) &&
                Objects.equals(debitReconciled, that.debitReconciled) &&
                Objects.equals(memo, that.memo) &&
                Objects.equals(customTags, that.customTags);
    }

    @Override
    public int hashCode() {
        int hashResult = hashCode;
        if (hashResult == 0) {
            hashResult = Objects.hash(transactionTag, debitAccount, creditAccount, creditAmount, debitAmount,
                    creditReconciled, debitReconciled, memo, customTags);
            hashCode = hashResult;
        }
        return hashResult;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        final String lineSep = System.lineSeparator();

        b.append("TransactionEntry hashCode: ").append(hashCode()).append(lineSep);
        b.append("Tag:            ").append(getTransactionTag().name()).append(lineSep);
        b.append("Memo:           ").append(getMemo()).append(lineSep);
        b.append("Debit Account:  ").append(getDebitAccount().getName()).append(lineSep);
        b.append("Credit Account: ").append(getCreditAccount().getName()).append(lineSep);
        b.append("Debit Amount:   ").append(getDebitAmount().toPlainString()).append(lineSep);
        b.append("Credit Amount:  ").append(getCreditAmount().toPlainString()).append(lineSep);

        return b.toString();
    }
}
