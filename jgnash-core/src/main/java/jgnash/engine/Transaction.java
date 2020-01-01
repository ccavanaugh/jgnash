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
package jgnash.engine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import jgnash.util.NotNull;
import jgnash.util.Nullable;

/**
 * Base class for transactions.  Transaction should be treated as immutable as in not modified if they have
 * been persisted within the database.
 *
 * @author Craig Cavanaugh
 */
@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "TRANSACT") // cannot use "Transaction" as the table name or it causes an SQL error!!!!
public class Transaction extends StoredObject implements Comparable<Transaction> {

    private static final transient String EMPTY = "";

    /**
     * If the memo consists of only the summation symbol, memos from the TransactionEntries are concatenated.
     */
    public static final transient String CONCATENATE = "Ʃ";

    /**
     * Date of entry from form entry, used for sort order.
     */
    LocalDate date = LocalDate.now();

    /**
     * Timestamp for transaction creation.
     */
    @Column(name = "timestamp", nullable = false, columnDefinition = "BIGINT default 0")
    long timestamp = System.currentTimeMillis();

    /**
     * Transaction number.
     */
    private String number;

    /**
     * Transaction payee.
     */
    private String payee;

    /**
     * Financial Institute Transaction ID. Typically used for OFX import FITID.  If this field is not null
     * then it is an indicator of an imported transaction
     */
    @Basic
    private String fitid;

    /**
     * File name for the attachment, should not contain any preceding paths.
     */
    @Column(columnDefinition = "VARCHAR(256)")
    private String attachment;

    /**
     * Transaction memo.
     */
    @Column(columnDefinition = "VARCHAR(1024)")
    private String memo;

    /**
     * Cache the concatenated memo
     */
    private transient String concatMemo;

    /**
     * Cache the generated LocalDateTime
     */
    private transient LocalDateTime timeStampDate;

    /**
     * Transaction entries.
     */
    @JoinTable
    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
    Set<TransactionEntry> transactionEntries = new HashSet<>();

    /**
     * Public constructor.
     */
    public Transaction() {
        // zero arg constructor required for persistence
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
        final Set<Account> accounts = new TreeSet<>();

        for (final TransactionEntry e : transactionEntries) {
            accounts.add(e.getCreditAccount());
            accounts.add(e.getDebitAccount());
        }

        return accounts;
    }

    /**
     * Determines if any of the transaction's accounts are hidden.
     *
     * @return true if any of the accounts are hidden
     */
    public boolean areAccountsHidden() {
        boolean accountsHidden = false;

        for (final Account account : getAccounts()) {
            if (!account.isVisible()) {
                accountsHidden = true;
                break;
            }
        }

        return accountsHidden;
    }

    /**
     * Determines if any of the transaction's accounts are locked against editing (cloning and then changing accounts).
     *
     * @return true if any of the accounts are locked
     */
    public boolean areAccountsLocked() {
        boolean accountsLocked = false;

        for (final Account account : getAccounts()) {
            if (account.isLocked()) {
                accountsLocked = true;
                break;
            }
        }

        return accountsLocked;
    }

    /**
     * Search for a common account for all entries.
     *
     * @return the common Account
     * @see Account
     */
    public Account getCommonAccount() {
        Account account = null;

        if (size() >= 2) {
            for (final Account a : getAccounts()) {
                boolean success = true;
                for (final TransactionEntry e : transactionEntries) {
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

        if (transactionEntries.contains(entry)) {
            throw new IllegalArgumentException("Duplicate entry is not allowed");
        }

        transactionEntries.add(entry);
    }

    public void removeTransactionEntry(@NotNull final TransactionEntry entry) {
        Objects.requireNonNull(entry);

        transactionEntries.remove(entry);
    }

    /**
     * Returns the number of {@code TransactionEntry(s)} this transaction contains. A read lock is obtained before
     * determining the size.
     *
     * @return the number of {@code TransactionEntry(s)}
     * @see TransactionEntry
     */
    public int size() {
        return transactionEntries.size();
    }

    public void setDate(@NotNull final LocalDate localDate) {
        this.date = localDate;
    }

    public LocalDate getLocalDate() {
        return date;
    }

    /**
     * Sets the payee for this transaction.
     *
     * @param payee the transaction payee
     */
    public void setPayee(@Nullable final String payee) {
        this.payee = payee;
    }

    /**
     * Return the payee for this transaction.
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
     * Sets the number for this transaction.
     *
     * @param number the transaction number
     */
    public void setNumber(@Nullable final String number) {
        this.number = number;
    }

    /**
     * Return the number for this transaction.
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
     * Calculates the amount of the transaction relative to the supplied account.
     * <p>
     * This method is synchronized to protect against concurrency issues
     *
     * @param account reference account
     * @return Amount of this transaction relative to the supplied account
     */
    public synchronized BigDecimal getAmount(final Account account) {
        return transactionEntries.stream().map(transactionEntry
                -> transactionEntry.getAmount(account)).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Compares two Transactions for ordering. Equality is checked for at the reference level. If a comparison cannot be
     * determined, the hashCode is used
     *
     * @param tran the {@code Transaction} to be compared.
     * @return the value {@code 0} if the argument Transaction is equal to this Transaction; a value less than
     * {@code 0} if this Transaction is before the Transaction argument; and a value greater than
     * {@code 0} if this Transaction is after the Transaction argument.
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

        result = Long.compareUnsigned(timestamp, tran.timestamp);
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
     * Returns a sorted defensive copy of the transaction entries.
     *
     * @return list of transaction entries
     */
    public List<TransactionEntry> getTransactionEntries() {

        // protect against write through by creating a new ArrayList
        final List<TransactionEntry> list = new ArrayList<>(transactionEntries);
        Collections.sort(list);

        return list;
    }

    private List<TransactionEntry> getTransactionEntries(final Account account) {
        return transactionEntries.stream()
                .filter(transactionEntry -> transactionEntry.getCreditAccount().equals(account)
                        || transactionEntry.getDebitAccount().equals(account)).collect(Collectors.toList());
    }


    /**
     * Return a list of transaction entries with the given tag.
     *
     * @param tag TransactionTag to filter for
     * @return {@code List<TransactionEntry>} of entries with the given tag. An empty list will be
     * returned if none are found
     */
    List<TransactionEntry> getTransactionEntriesByTag(final TransactionTag tag) {
        return transactionEntries.stream().filter(e -> e.getTransactionTag() == tag).collect(Collectors.toList());
    }

    /**
     * Adds a collection of transaction entries.
     *
     * @param entries collection of TransactionEntry(s)
     */
    public void addTransactionEntries(final Collection<TransactionEntry> entries) {
        entries.forEach(this::addTransactionEntry);
    }

    /**
     * Clears all transaction entries.
     */
    public void clearTransactionEntries() {
        transactionEntries.clear();
    }

    public LocalDateTime getTimestamp() {
        if (timeStampDate == null) {
            timeStampDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        }

        return timeStampDate;
    }

    @NotNull
    public TransactionType getTransactionType() {
        if (size() == 1) {
            final TransactionEntry entry = transactionEntries.iterator().next();

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

    /**
     * Returns the memo for the {@code Transaction}.  If memo was set to be equal to {@value #CONCATENATE}, then a
     * concatenated version of the {@code TransactionEntry} memos will be returned.  If the {@code Transaction} level
     * memo is null, that the memo for the first {@code TransactionEntry} is returned
     *
     * @return resultant memo
     */
    @NotNull
    public synchronized String getMemo() {
        if (memo != null) {
            if (isMemoConcatenated()) {
                if (concatMemo == null) {
                    concatMemo = getMemo(getTransactionEntries());
                }
                return concatMemo;
            }

            return memo;
        }
        return getTransactionEntries().get(0).getMemo();
    }

    /**
     * Returns the {@code Transaction} level memo
     *
     * @return the Transaction memo
     */
    @Nullable
    public String getTransactionMemo() {
        return memo;
    }

    /**
     * Returns the concatenated memo given an Account.
     *
     * @param account base account to generate a memo for
     * @return Concatenated string of split entry memos
     */
    @NotNull
    public synchronized String getMemo(@NotNull final Account account) {
        return getMemo(getTransactionEntries(account));
    }

    /**
     * Builds a concatenated memo given a list of TransactionEntries.
     *
     * @param transEntries List of {@code TransactionEntry}
     * @return concatenated memo
     */
    public static String getMemo(final List<TransactionEntry> transEntries) {
        final List<String> memoList = new ArrayList<>();

        // Create an ordered list of unique memos that are not empty
        transEntries.stream().filter(transactionEntry
                -> !transactionEntry.getMemo().isEmpty() && !memoList.contains(transactionEntry.getMemo()))
                .forEachOrdered(transactionEntry -> memoList.add(transactionEntry.getMemo()));

        return String.join(", ", memoList);
    }

    public boolean isMemoConcatenated() {
        return CONCATENATE.equals(memo);
    }

    /**
     * Set the memo for the {@code Transaction}.  If set to be equal to {@value #CONCATENATE}, then a concatenated
     * version will be reported.  If set to null or an empty string, the memo of the first {@code TransactionEntry}
     * will be reported.  Otherwise, the supplied String will be reported.
     *
     * @param memo sets the {@code Transaction} level memo
     */
    public synchronized void setMemo(final String memo) {

        // force to null if empty to conserve memory.
        if (memo != null && memo.isEmpty()) {
            this.memo = null;
        } else {
            this.memo = memo;
        }
    }

    /**
     * Returns a set of all tags associated with the transaction
     *
     * @return Set of all Tags
     */
    public Set<Tag> getTags() {
        final Set<Tag> tags = new HashSet<>();

        for (final TransactionEntry entry : transactionEntries) {
            tags.addAll(entry.getTags());
        }

        return tags;
    }

    /**
     * Returns a set of all tags associated with the specified Account in common.
     *
     * @param account common Account
     * @return Set of all Tags
     */
    public Set<Tag> getTags(final Account account) {
        final Set<Tag> tags = new HashSet<>();

        for (final TransactionEntry entry : getTransactionEntries(account)) {
            tags.addAll(entry.getTags());
        }

        return tags;
    }

    /**
     * Assigns the specified Tag(s) to all entries
     *
     * @param tags Set of tags to assign
     * @see TransactionEntry#setTags(Collection)
     */
    public void setTags(final Collection<Tag> tags) {
        for (final TransactionEntry entry : transactionEntries) {
            entry.setTags(tags);
        }
    }

    /**
     * Assigns the specified Tag(s) to all entries of the specified class
     * @param clazz class to filter by
     * @param tags Tags to assign
     * @see TransactionEntry#setTags(Collection)
     */
    public void setTags(@NotNull final Class<? extends TransactionEntry> clazz, @NotNull final Collection<Tag> tags) {
        transactionEntries.stream().filter(clazz::isInstance).forEach(e-> e.setTags(tags));
    }

    /**
     * Returns all tags associated with with entries of the specified class
     * @param clazz class to filter by
     * @return Set of Tags
     */
    public Set<Tag> getTags(@NotNull final Class<? extends TransactionEntry> clazz) {
        final Set<Tag> tags = new HashSet<>();
        transactionEntries.stream().filter(clazz::isInstance)
                          .forEach(transactionEntry -> tags.addAll(transactionEntry.getTags()));
        return tags;
    }

    @Nullable
    public String getFitid() {
        return fitid;
    }

    public void setFitid(final String fitid) {
        this.fitid = fitid;
    }

    @SuppressWarnings("WeakerAccess")
    public void setReconciled(@NotNull final Account account, @NotNull final ReconciledState state) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(state);

        for (final TransactionEntry e : transactionEntries) {
            e.setReconciled(account, state);
        }
    }

    public void setReconciled(@NotNull final ReconciledState state) {
        Objects.requireNonNull(state);

        for (final TransactionEntry e : transactionEntries) {
            e.setCreditReconciled(state);
            e.setDebitReconciled(state);
        }
    }

    @NotNull
    public ReconciledState getReconciled(final Account account) {
        ReconciledState state = ReconciledState.NOT_RECONCILED; // default is not reconciled

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

        final Transaction tran = (Transaction) super.clone();

        tran.concatMemo = null; // force a reset of the concatenated memo, the entries of the clone may change

        tran.timestamp = System.currentTimeMillis();    // force the clone to have a new timestamp
        tran.timeStampDate = null;                      // clear the cached value

        // deep clone
        tran.transactionEntries = new HashSet<>(); // deep clone

        for (final TransactionEntry entry : transactionEntries) {
            tran.addTransactionEntry((TransactionEntry) entry.clone());
        }

        return tran;
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();

        final String lineSep = System.getProperty("line.SEPARATOR");

        b.append("Transaction UUID: ").append(getUuid()).append(lineSep);
        b.append("Number:           ").append(getNumber()).append(lineSep);
        b.append("Payee:            ").append(getPayee()).append(lineSep);
        b.append("Memo:             ").append(getMemo()).append(lineSep);

        b.append(lineSep);

        for (final TransactionEntry entry : getTransactionEntries()) {
            b.append(entry).append(lineSep);
        }

        return b.toString();
    }
}
