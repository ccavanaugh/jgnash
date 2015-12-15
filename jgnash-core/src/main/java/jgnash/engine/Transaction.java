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
package jgnash.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.Table;

import jgnash.util.NotNull;
import jgnash.util.Nullable;

/**
 * Base class for transactions
 *
 * @author Craig Cavanaugh
 */
@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name="TRANSACT") // cannot use "Transaction" as the table name or it causes an SQL error!!!!
public class Transaction extends StoredObject implements Comparable<Transaction> {

    private static final transient String EMPTY = "";

    /**
     * Date of entry from form entry, used for sort order
     */
    LocalDate date = LocalDate.now();

    /**
     * Date transaction was created
     *
     * TODO: Replace with LocalDateTime
     */
    LocalDate dateEntered = LocalDate.now();

    /**
     * Transaction number
     */
    private String number;

    /**
     * Transaction payee
     */
    private String payee;

    /**
     * Financial Institute Transaction ID. Typically used for OFX import FITID.  If this field is not null
     * then it is an indicator of an imported transaction
     */
    @Basic(optional = true)
    private String fitid;

    /**
     * File name for the attachment, should not contain any preceding paths
     */
    @Column(columnDefinition = "VARCHAR(256)", nullable = true)
    private String attachment;

    /**
     * Transaction memo
     */
    @Column(columnDefinition = "VARCHAR(1024)")
    private String memo;

    /**
     * Transaction entries
     */
    @JoinTable
    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
    Set<TransactionEntry> transactionEntries = new HashSet<>();

    /**
     * ReadWrite lock
     */
    private transient ReadWriteLock lock;

    /**
     * Public constructor
     */
    public Transaction() {
        lock = new ReentrantReadWriteLock(true);
    }

    ReadWriteLock getLock() {
        return lock;
    }

    /**
     * Returns a set of accounts this transaction effects.
     * The returned set may be altered without creating side effects
     *
     * @return set of accounts
     * @see Account
     */
    @NotNull
    public Set<Account> getAccounts() {
        Set<Account> accounts = new TreeSet<>();

        Lock l = getLock().readLock();
        l.lock();

        try {
            for (TransactionEntry e : transactionEntries) {
                accounts.add(e.getCreditAccount());
                accounts.add(e.getDebitAccount());
            }
        } finally {
            l.unlock();
        }

        return accounts;
    }

    /**
     * Determines if any of the transaction's accounts are hidden
     *
     * @return true if any of the accounts are hidden
     */
    public boolean areAccountsHidden() {
        boolean accountsHidden = false;

        for (Account account : getAccounts()) {
            if (!account.isVisible()) {
                accountsHidden = true;
                break;
            }
        }

        return accountsHidden;
    }

    /**
     * Determines if any of the transaction's accounts are locked against editing (cloning and then changing accounts)
     *
     * @return true if any of the accounts are locked
     */
    public boolean areAccountsLocked() {
        boolean accountsLocked = false;

        for (Account account : getAccounts()) {
            if (account.isLocked()) {
                accountsLocked = true;
                break;
            }
        }

        return accountsLocked;
    }

    /**
     * Search for a common account for all entries
     *
     * @return the common Account
     * @see Account
     */
    public Account getCommonAccount() {
        Account account = null;

        Lock l = getLock().readLock();
        l.lock();

        try {

            if (size() >= 2) {
                Set<Account> accounts = getAccounts();

                for (Account a : accounts) {
                    boolean success = true;
                    for (TransactionEntry e : transactionEntries) {
                        if (!e.getCreditAccount().equals(a) && !e.getDebitAccount().equals(a)) {
                            success = false;
                            break;
                        }
                    }
                    if (success) {
                        account = a;
                        break;
                    }
                }
            } else { // double entry transaction, return the credit account by default
                account = transactionEntries.iterator().next().getCreditAccount();
            }
        } finally {
            l.unlock();
        }

        return account;
    }

    /**
     * Returns the balance of the transaction amount with respect to the supplied account. Value will be positive or
     * negative depending if the transaction debits or credits the account.
     *
     * @param entry new TransactionEntry to add
     * @see TransactionEntry
     */
    public void addTransactionEntry(@NotNull final TransactionEntry entry) {
        Objects.requireNonNull(entry);

        assert !transactionEntries.contains(entry);

        Lock l = getLock().writeLock();
        l.lock();

        try {
            transactionEntries.add(entry);
        } finally {
            l.unlock();
        }
    }

    public void removeTransactionEntry(@NotNull final TransactionEntry entry) {
        Objects.requireNonNull(entry);

        Lock l = getLock().writeLock();
        l.lock();

        try {
            transactionEntries.remove(entry);
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns the number of {@code TransactionEntry(s)} this transaction contains. A read lock is obtained before
     * determining the size.
     *
     * @return the number of {@code TransactionEntry(s)}
     * @see TransactionEntry
     */
    public int size() {
        Lock l = getLock().readLock();
        l.lock();

        try {
            return transactionEntries.size();
        } finally {
            l.unlock();
        }
    }

    public void setDate(@NotNull final LocalDate localDate) {
        this.date = localDate;
    }

    public LocalDate getLocalDate() {
        return date;
    }

    /**
     * Sets the payee for this transaction
     *
     * @param payee the transaction payee
     */
    public void setPayee(@Nullable final String payee) {
        this.payee = payee;
    }

    /**
     * Return the payee for this transaction
     *
     * @return the transaction payee. Guaranteed to not return null
     */
    @NotNull
    public String getPayee() {
        String result = EMPTY;

        if (payee != null) {
            result = payee;
        }
        return result;
    }

    /**
     * Sets the number for this transaction
     *
     * @param number the transaction number
     */
    public void setNumber(@Nullable final String number) {
        this.number = number;
    }

    /**
     * Return the number for this transaction
     *
     * @return the transaction number. Guaranteed to not return null
     */
    @NotNull
    public String getNumber() {
        String result = EMPTY;

        if (number != null) {
            result = number;
        }
        return result;
    }

    /**
     * Calculates the amount of the transaction relative to the supplied account
     *
     * @param account reference account
     * @return Amount of this transaction relative to the supplied account
     */
    public BigDecimal getAmount(final Account account) {
        BigDecimal balance = BigDecimal.ZERO;

        Lock l = getLock().readLock();
        l.lock();

        try {
            for (TransactionEntry entry : transactionEntries) {
                balance = balance.add(entry.getAmount(account));
            }
        } finally {
            l.unlock();
        }

        return balance;
    }

    /**
     * Compares two Transactions for ordering. Equality is checked for at the reference level. If a comparison cannot be
     * determined, the hashCode is used
     *
     * @param tran the {@code Transaction} to be compared.
     * @return the value {@code 0} if the argument Transaction is equal to this Transaction; a value less than
     *         {@code 0} if this Transaction is before the Transaction argument; and a value greater than
     *         {@code 0} if this Transaction is after the Transaction argument.
     */
    @Override
    public int compareTo(final @NotNull Transaction tran) {
        if (tran == this) {
            return 0;
        }

        int result = date.compareTo(tran.date);
        if (result != 0) {
            return result;
        }

        result = getNumber().compareTo(tran.getNumber());
        if (result != 0) {
            return result;
        }

        result = dateEntered.compareTo(tran.dateEntered);
        if (result != 0) {
            return result;
        }

        result = getAmount(getCommonAccount()).compareTo(tran.getAmount(tran.getCommonAccount()));
        if (result != 0) {
            return result;
        }

        return getUuid().compareTo(tran.getUuid());
    }

    /**
     * Compares this transaction against another for equality. The date the transaction is created is ignored. The
     * voucher date is still tested.
     *
     * @param tran Transaction to compare against
     * @return {@code true} if Transactions are equal
     */
    public boolean equalsIgnoreDate(final Transaction tran) {
        if (tran == this) {
            return true;
        }

        if (getTransactionType() != tran.getTransactionType()) {
            return false;
        }

        if (!date.equals(tran.date)) {
            return false;
        }

        if (!getPayee().equalsIgnoreCase(tran.getPayee())) {
            return false;
        }

        if (!getAmount(getCommonAccount()).equals(tran.getAmount(tran.getCommonAccount()))) {
             return false;
        }

        return getNumber().equalsIgnoreCase(tran.getNumber());

    }

    public List<TransactionEntry> getTransactionEntries() {

        List<TransactionEntry> list = null;

        Lock l = getLock().readLock();
        l.lock();

        try {
            // protect against write through by creating a new ArrayList
            list = new ArrayList<>(transactionEntries);
            Collections.sort(list);
        } finally {
            l.unlock();
        }

        return list;
    }

    /**
     * Return a list of transaction entries with the given tag
     *
     * @param tag TransactionTag to filter for
     * @return {@code List<TransactionEntry>} of entries with the given tag. An empty list will be
     * returned if none are found
     */
    List<TransactionEntry> getTransactionEntriesByTag(final TransactionTag tag) {
        List<TransactionEntry> list = new ArrayList<>();

        Lock l = getLock().readLock();
        l.lock();

        try {
            list.addAll(transactionEntries.stream()
                    .filter(e -> e.getTransactionTag() == tag).collect(Collectors.toList()));
        } finally {
            l.unlock();
        }

        return list;
    }

    /**
     * Adds a collection of transaction entries
     *
     * @param entries collection of TransactionEntry(s)
     */
    public void addTransactionEntries(final Collection<TransactionEntry> entries) {
        entries.forEach(this::addTransactionEntry);
    }

    /**
     * Clears all transaction entries
     */
    public void clearTransactionEntries() {
        transactionEntries.clear();
    }

    public LocalDate getDateEntered() {
        return dateEntered;
    }

    public void setDateEntered(@NotNull final LocalDate localDate) {
        Objects.requireNonNull(localDate);

        dateEntered = localDate;
    }

    @NotNull
    public TransactionType getTransactionType() {
        if (size() == 1) {
            TransactionEntry entry = transactionEntries.iterator().next();

            if (entry.isSingleEntry()) {
                return TransactionType.SINGLENTRY;
            }
            return TransactionType.DOUBLEENTRY;
        }

        if (size() > 1) {
            return TransactionType.SPLITENTRY;
        }

        return TransactionType.INVALID;
    }

    @NotNull
    public String getMemo() {
        if (memo != null) {
            return memo;
        }
        return getTransactionEntries().get(0).getMemo();
    }

    public void setMemo(final String memo) {
        this.memo = memo;
    }

    @Nullable
    public String getFitid() {
        return fitid;
    }

    public void setFitid(final String fitid) {
        this.fitid = fitid;
    }

    public void setReconciled(@NotNull final Account account, @NotNull final ReconciledState state) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(state);

        final Lock l = getLock().writeLock();
        l.lock();

        try {
            for (final TransactionEntry e : transactionEntries) {
                e.setReconciled(account, state);
            }
        } finally {
            l.unlock();
        }
    }

    public void setReconciled(@NotNull final ReconciledState state) {
        Objects.requireNonNull(state);

        final Lock l = getLock().writeLock();
        l.lock();

        try {
            for (final TransactionEntry e : transactionEntries) {
                e.setCreditReconciled(state);
                e.setDebitReconciled(state);
            }
        } finally {
            l.unlock();
        }
    }

    @NotNull
    public ReconciledState getReconciled(final Account account) {
        ReconciledState state = ReconciledState.NOT_RECONCILED; // default is not reconciled

        final Lock l = getLock().readLock();
        l.lock();

        try {

            for (final TransactionEntry e : transactionEntries) {
                if (e.getCreditAccount().equals(account)) {
                    state = e.getCreditReconciled();
                    break;
                }

                if (e.getDebitAccount().equals(account)) {
                    state = e.getDebitReconciled();
                    break;
                }
            }

        } finally {
            l.unlock();
        }

        return state;
    }

    /**
     * Returns an external link to a file.
     *
     * @return external path, null if not set
     */
    @Nullable
    public String getAttachment() {
        return attachment;
    }

    /**
     * Sets an external link to a file, the path should be relative to the data file for portability.
     * May be set to null.
     *
     * @param attachment attachment path
     */
    public void setAttachment(@Nullable final String attachment) {
        this.attachment = attachment;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Lock l = getLock().readLock();
        l.lock();

        Transaction tran;

        try {

            tran = (Transaction) super.clone();

            // deep clone
            tran.transactionEntries = new HashSet<>(); // deep clone
            tran.lock = new ReentrantReadWriteLock(true);

            for (TransactionEntry entry : transactionEntries) {
                tran.addTransactionEntry((TransactionEntry) entry.clone());
            }

        } finally {
            l.unlock();
        }

        return tran;
    }

    /**
     * Required by XStream for proper initialization
     *
     * @return Properly initialized Transaction
     */
    @SuppressWarnings("unused")
    protected Object readResolve() {
        postLoad();
        return this;
    }

    @PostLoad
    private void postLoad() {
        lock = new ReentrantReadWriteLock(true);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        final String lineSep = System.getProperty("line.separator");

        b.append("Transaction UUID: ").append(getUuid()).append(lineSep);
        b.append("Number:           ").append(getNumber()).append(lineSep);
        b.append("Payee:            ").append(getPayee()).append(lineSep);
        b.append("Memo:             ").append(getMemo()).append(lineSep);

        b.append(lineSep);

        for (TransactionEntry entry : getTransactionEntries()) {
            b.append(entry).append(lineSep);
        }

        return b.toString();
    }
}
