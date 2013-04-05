/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Transaction Entry
 * <p/>
 * Each Transaction entry has an amount for the credit and debit side of the transaction. When the debit and credit
 * account has the same currency, one amount will be the negated value of the other. If the credit and debit accounts do
 * not have the same currency then the amounts will be different to represent the exchanged value at the time of the
 * transaction.
 * <p/>
 * If the entry is to be used as an "single entry / adjustment" transaction, then the credit and debit account must be
 * set to the same account and the credit and debit amounts must be set to the same value.
 * 
 * @author Craig Cavanaugh
 */
@Entity
public class TransactionEntry implements Comparable<TransactionEntry>, Cloneable, Serializable {
    
    private static final long serialVersionUID = 1L;

    private transient int hash = 0;

    @Id
    @SuppressWarnings("unused")
    @GeneratedValue(strategy=GenerationType.TABLE)
    private long id;

    @Enumerated(EnumType.STRING)
    private TransactionTag transactionTag = TransactionTag.BANK;

    /**
     * Account with balance being decreased
     */
    @ManyToOne
    private Account debitAccount;

    /**
     * Account with balance being increased
     */
    @ManyToOne
    private Account creditAccount;

    @Column(precision = 22, scale = 4)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(precision = 22, scale = 4)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    /**
     * Reconciled state of the transaction
     */
    @Enumerated(EnumType.STRING)
    private ReconciledState creditReconciled = ReconciledState.NOT_RECONCILED;

    /**
     * Reconciled state of the debit side of the transaction
     */
    @Enumerated(EnumType.STRING)
    private ReconciledState debitReconciled = ReconciledState.NOT_RECONCILED;

    /**
     * Memo for this entry
     */
    private String memo = "";

    /**
     * Public constructor
     */
    public TransactionEntry() {
    }

    /**
     * Simple constructor for a single entry transaction
     * 
     * @param account account for the transaction
     * @param amount amount for the transaction
     */
    public TransactionEntry(final Account account, final BigDecimal amount) {
        assert account != null && amount != null;

        creditAccount = account;
        debitAccount = account;

        creditAmount = amount;
        debitAmount = amount;
    }

    /**
     * Simple constructor for a double entry transaction
     * 
     * @param creditAccount credit account for the transaction
     * @param debitAccount debit account for the transaction
     * @param amount amount for the transaction
     */
    TransactionEntry(final Account creditAccount, final Account debitAccount, final BigDecimal amount) {
        assert creditAccount != null && debitAccount != null && amount != null;

        this.creditAccount = creditAccount;
        this.debitAccount = debitAccount;

        setAmount(amount.abs());
    }

    /**
     * Simple constructor for a double entry transaction with exchange rate
     * 
     * @param creditAccount credit account for the transaction
     * @param debitAccount debit account for the transaction
     * @param creditAmount amount for the transaction
     * @param debitAmount amount for the transaction
     */
    TransactionEntry(final Account creditAccount, final Account debitAccount, final BigDecimal creditAmount, final BigDecimal debitAmount) {
        assert creditAccount != null && debitAccount != null && creditAmount != null && debitAmount != null;
        assert creditAmount.signum() == 1 && debitAmount.signum() == -1;

        this.creditAccount = creditAccount;
        this.debitAccount = debitAccount;

        this.creditAmount = creditAmount;
        this.debitAmount = debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public BigDecimal getAmount(final Account account) {
        assert account != null;

        if (account.equals(creditAccount)) {
            return creditAmount;
        } else if (account.equals(debitAccount)) {
            return debitAmount;
        } else {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Shortcut method to set credit and debit amounts
     * 
     * @param amount credit amount of the transaction
     */
    public final void setAmount(final BigDecimal amount) {
        assert amount != null && amount.signum() >= 0;

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

    public void setCreditAmount(final BigDecimal amount) {
        this.creditAmount = amount;
    }

    public void setCreditAccount(final Account creditAccount) {
        this.creditAccount = creditAccount;
    }

    void setCreditReconciled(final ReconciledState creditReconciled) {
        this.creditReconciled = creditReconciled;
    }

    public void setDebitAccount(final Account debitAccount) {
        this.debitAccount = debitAccount;
    }

    void setDebitReconciled(final ReconciledState debitReconciled) {
        this.debitReconciled = debitReconciled;
    }

    /**
     * Sets the memo for the entry
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

    public void setDebitAmount(final BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    @Override
    public int compareTo(final TransactionEntry entry) {

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
     * Reconciled state is ignored
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        
        if (obj == null) {
            return false;
        }
        
        if (!(obj instanceof TransactionEntry)) {
            return false;
        }
        
        TransactionEntry entry = (TransactionEntry) obj;        

        if (!creditAccount.equals(entry.getCreditAccount())) {
            return false;
        }

        if (!debitAccount.equals(entry.getDebitAccount())) {
            return false;
        }

        if (!creditAmount.equals(entry.getCreditAmount())) {
            return false;
        }

        if (!debitAmount.equals(entry.getDebitAmount())) {
            return false;
        }

        if (creditReconciled != entry.creditReconciled) {
            return false;
        }

        if (debitReconciled != entry.debitReconciled) {
            return false;
        }

        return memo.equals(entry.getMemo());
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            h = 5;
            h = 17 * h + (transactionTag != null ? transactionTag.hashCode() : 0);
            h = 17 * h + (debitAccount != null ? debitAccount.hashCode() : 0);
            h = 17 * h + (creditAccount != null ? creditAccount.hashCode() : 0);
            h = 17 * h + (creditAmount != null ? this.creditAmount.hashCode() : 0);
            h = 17 * h + (debitAmount != null ? debitAmount.hashCode() : 0);
            h = 17 * h + (creditReconciled != null ? creditReconciled.hashCode() : 0);
            h = 17 * h + (debitReconciled != null ? debitReconciled.hashCode() : 0);
            h = 17 * h + (memo != null ? memo.hashCode() : 0);
            hash = h;
        }
        return h;
    }    

    /**
     * Check to determine is this is a single entry transaction
     * 
     * @return <code>true</code> if this is a single entry TransactionEntry
     */
    public boolean isSingleEntry() {
        return creditAccount.equals(debitAccount) && creditAmount.equals(debitAmount);
    }

    /**
     * Returns true if multiple currencies are being used for this entry.
     * <p>
     * If the credit and debit accounts have differing currencies, then unless
     * the currencies are equal in value, the credit and debit amounts should be different.
     *
     * @return <code>true</code> if this is a multi-currency transaction entry
     */
    public boolean isMultiCurrency() {
        return !creditAccount.getCurrencyNode().equals(debitAccount.getCurrencyNode());
    }

    public void setTransactionTag(final TransactionTag transactionTag) {
        this.transactionTag = transactionTag;

    }

    public TransactionTag getTransactionTag() {       
        return transactionTag;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        TransactionEntry e = null;

        try {
            e = (TransactionEntry) super.clone();
            e.id = 0; // clones id must be reset for JPA
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(TransactionEntry.class.getName()).log(Level.SEVERE, ex.toString(), ex);
        }

        return e;
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
