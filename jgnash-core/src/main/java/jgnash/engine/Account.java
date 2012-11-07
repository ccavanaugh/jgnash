/*
 * jGnash, account personal finance application
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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jgnash.util.DateUtils;

/**
 * Abstract Account class
 *
 * @author Craig Cavanaugh
 * @author Jeff Prickett prickett@users.sourceforge.net
 */
public class Account extends StoredObject implements Comparable<Account> {

    private static final long serialVersionUID = 6886735664760113291L;

    private String accountType;

    private transient AccountType cachedAccountType;

    private boolean placeHolder = false;

    private boolean locked = false;

    Account parentAccount;

    private boolean visible = true;

    private boolean excludedFromBudget = false;

    private String name = "";

    private String description = "";

    private String notes = "";

    /**
     * CurrencyNode for this account
     */
    private CurrencyNode currencyNode;

    private final List<Account> children = new ArrayList<>();

    final List<Transaction> transactions = new ArrayList<>();

    List<SecurityNode> securities = new ArrayList<>();

    /**
     * Balance of the account
     */
    private BigDecimal accountBalance;

    /**
     * Reconciled balance of the account
     */
    private BigDecimal reconciledBalance;

    /**
     * User definable account number
     */
    private String accountNumber = "";

    /**
     * User definable bank id. Useful for OFX import
     */
    private String bankId;

    private final Map<String, Object> propertyMap = new HashMap<>();

    private transient ReadWriteLock transactionLock;

    private transient ReadWriteLock childLock;

    private transient AccountProxy proxy;

    /**
     * String delimiter for reported account structure
     */
    private static String accountSeparator = ":";

    private static final Pattern numberPattern = Pattern.compile("\\d+");

    private static final Logger logger = Logger.getLogger(Account.class.getName());

    /**
     * No argument public constructor for reflection purposes. <b>Do not use to
     * create account new instance</b>
     */
    public Account() {
        transactionLock = new ReentrantReadWriteLock(true);
        childLock = new ReentrantReadWriteLock(true);
    }

    public Account(final AccountType type, final CurrencyNode node) {
        this();

        if (type == null) {
            throw new IllegalArgumentException("Null AccountType assigned");
        }

        if (node == null) {
            throw new IllegalArgumentException("Null CurrencyNode assigned");
        }

        setAccountType(type);
        setCurrencyNode(node);
    }

    ReadWriteLock getTransactionLock() {
        return transactionLock;
    }

    AccountProxy getProxy() {
        if (proxy == null) {
            proxy = getAccountType().getProxy(this);
        }
        return proxy;
    }

    final void setAccountType(final AccountType type) {
        if (type == null) {
            throw new RuntimeException("Null account type");
        }

        if (cachedAccountType != null && !cachedAccountType.isMutable()) {
            throw new RuntimeException("Immutable account type");
        }

        accountType = type.name();
        cachedAccountType = type;

        proxy = null; // proxy will need to change
    }

    /**
     * Sets / Adds an AccountProperty
     *
     * @param key   AccountProperty
     * @param value actual object to add or set
     */
    public void setProperty(final AccountProperty key, final Object value) {
        propertyMap.put(key.name(), value);
    }

    /**
     * Remove an AccountProperty from this account
     *
     * @param key AccountProperty to remove
     * @return true if this account contained the AccountProperty
     */
    boolean removeProperty(final AccountProperty key) {
        return propertyMap.remove(key.name()) != null;
    }

    /**
     * Gets an account property
     *
     * @param key AccountProperty to get
     * @return not null if the account contained the property
     */
    public Object getProperty(final AccountProperty key) {
        return propertyMap.get(key.name());
    }

    /**
     * Returns account Set of AccountProperties added to this account.
     *
     * @return Set of AccountProperties. An empty Set will be returned in none
     *         exist.
     */
    public Set<AccountProperty> getProperties() {
        Set<AccountProperty> properties = EnumSet.noneOf(AccountProperty.class);

        for (String propertyKey : propertyMap.keySet()) {
            properties.add(AccountProperty.valueOf(propertyKey));
        }

        return properties;
    }

    static void setAccountSeparator(final String separator) {
        accountSeparator = separator;
    }

    private static String getAccountSeparator() {
        return accountSeparator;
    }

    /**
     * Clear cached account balances so they will be recalculated
     */
    void clearCachedBalances() {
        accountBalance = null;
        reconciledBalance = null;
    }

    /**
     * Adds account transaction in chronological order
     *
     * @param tran the
     *             <code>Transaction</code> to be added
     * @return <tt>true</tt> the transaction was added successful <tt>false</tt>
     *         the transaction was already attached to this account
     */
    boolean addTransaction(final Transaction tran) {
        if (placeHolder) {
            logger.severe("Tried to add transaction to a place holder account");
            return false;
        }

        Lock l = transactionLock.writeLock();
        l.lock();

        try {
            boolean result = false;

            if (!contains(tran)) {
                // Search for the non-existent item
                int index = Collections.binarySearch(transactions, tran);

                if (index < 0) {
                    transactions.add(-index - 1, tran);

                    clearCachedBalances();

                    result = true;
                }

            } else {
                StringBuilder log = new StringBuilder("Account: " + getName() + '(' + hashCode() + ")" + System.lineSeparator());
                log.append("Already have transaction ID: ").append(tran.hashCode());
                logger.severe(log.toString());
            }

            return result;
        } finally {
            l.unlock();
        }
    }

    /**
     * Removes the specified transaction from this account
     *
     * @param tran the
     *             <code>Transaction</code> to be removed
     * @return <tt>true</tt> the transaction removal was successful
     *         <tt>false</tt> the transaction could not be found within this account
     */
    boolean removeTransaction(final Transaction tran) {
        Lock l = transactionLock.writeLock();
        l.lock();

        try {
            boolean result = false;

            if (contains(tran)) {
                transactions.remove(tran);
                clearCachedBalances();

                result = true;
            } else {
                final StringBuilder log = new StringBuilder("Account: " + getName() + '(' + getUuid() + ")" + System.lineSeparator());
                log.append("Did not contain transaction ID: ").append(tran.getUuid());
                Logger.getLogger(Account.class.toString()).severe(log.toString());
            }

            return result;
        } finally {
            l.unlock();
        }
    }

    /**
     * Determines if the specified transaction is attach to this account
     *
     * @param tran the
     *             <code>Transaction</code> to look for
     * @return <tt>true</tt> the transaction is attached to this account
     *         <tt>false</tt> the transaction is not attached to this account
     */
    public boolean contains(final Transaction tran) {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            return transactions.contains(tran);
        } finally {
            l.unlock();
        }
    }

    /**
     * Determine if the supplied account is a child of this account
     *
     * @param account to check
     * @return true if the supplied account is a child of this account
     */
    public boolean contains(final Account account) {
        Lock l = childLock.readLock();
        l.lock();

        try {
            return children.contains(account);
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns a shallow copy of the transaction array for this account
     *
     * @return List of transactions
     * @see #getReadonlyTransactionList()
     */
    public List<Transaction> getTransactions() {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            return new ArrayList<>(transactions);
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns a readonly list that is a wrapper around the internal list. <p>
     * Use of this method over
     * <code>getTransactions()</code> will reduce GC and perform better. The
     * List should not be held because internal updates will cause indeterminate
     * changes
     *
     * @return List of transactions
     * @see #getTransactions()
     */
    public List<Transaction> getReadonlyTransactionList() {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            return Collections.unmodifiableList(transactions);
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns the transaction at the specified index
     *
     * @param index the index of the transaction to return.
     * @return the transaction at the specified index.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public Transaction getTransactionAt(final int index) throws IndexOutOfBoundsException {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            return transactions.get(index);
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns the number of transactions attached to this account.
     *
     * @return the number of transactions attached to this account.
     */
    public int getTransactionCount() {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            return transactions.size();
        } finally {
            l.unlock();
        }
    }

    /**
     * Searches through the transactions and determines the next largest
     * transaction number.
     *
     * @return The next check number; and empty String if numbers are not found
     */
    public String getNextTransactionNumber() {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            int number = 0;

            for (Transaction tran : transactions) {
                if (numberPattern.matcher(tran.getNumber()).matches()) {
                    try {
                        number = Math.max(number, Integer.parseInt(tran.getNumber()));
                    } catch (NumberFormatException e) {
                        logger.log(Level.INFO, "Number regex failed", e);
                    }
                }
            }

            if (number == 0) {
                return "";
            }

            return Integer.toString(number + 1);
        } finally {
            l.unlock();
        }
    }

    /**
     * Add account child account given it's reference
     *
     * @param child The child account to add to this account.
     * @return true if the account was added successfully, false otherwise.
     */
    protected boolean addChild(final Account child) {
        Lock l = childLock.writeLock();
        l.lock();

        try {
            boolean result = false;

            if (!children.contains(child) && child != this) {
                if (child.setParent(this)) {
                    children.add(child);
                    sortChildren();
                    result = true;
                }
            }

            return result;
        } finally {
            l.unlock();
        }
    }

    /**
     * Removes account child account. The reference to the parent(this) is left
     * so that the parent can be discovered.
     *
     * @param child The child account to remove.
     * @return true if the specific account was account child of this account,
     *         false otherwise.
     */
    boolean removeChild(final Account child) {
        Lock l = childLock.writeLock();
        l.lock();

        try {
            boolean result = false;

            if (children.remove(child)) {
                result = true;
            }
            return result;
        } finally {
            l.unlock();
        }
    }

    private void sortChildren() {
        Lock l = childLock.writeLock();
        l.lock();

        try {
            Collections.sort(children);
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns account list of the children
     *
     * @return List of children
     */
    public List<Account> getChildren() {
        Lock l = childLock.readLock();
        l.lock();

        try {
            // return account defensive copy
            return new ArrayList<>(children);
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns the index of the specified <tt>Transaction</tt> within this
     * <tt>Account</tt>.
     *
     * @param tran the <tt>Transaction</tt> to look for
     * @return The index of the <tt>Transaction</tt>, -1 if this
     *         <tt>Account</tt> does not contain the <tt>Transaction</tt>.
     */
    public int indexOf(final Transaction tran) {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            return transactions.indexOf(tran);
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns the number of children this account has
     *
     * @return the number of children this account has.
     */
    public int getChildCount() {
        Lock l = childLock.readLock();
        l.lock();

        try {
            return children.size();
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns the parent account.
     *
     * @return the parent of this account, null is this account is not account
     *         child
     */
    public Account getParent() {
        Lock l = childLock.readLock();
        l.lock();

        try {
            return parentAccount;
        } finally {
            l.unlock();
        }
    }

    /**
     * Sets the parent of this account
     *
     * @param account The new parent account
     * @return true is successful
     */
    public boolean setParent(final Account account) {
        Lock l = childLock.writeLock();
        l.lock();

        try {

            boolean result = false;

            if (account != this) {
                parentAccount = account;
                result = true;
            }

            return result;
        } finally {
            l.unlock();
        }
    }

    /**
     * Determines is this account has any child accounts.
     *
     * @return true is this account has children, false otherwise.
     */
    public boolean isParent() {
        Lock l = childLock.readLock();
        l.lock();

        try {
            return !children.isEmpty();
        } finally {
            l.unlock();
        }
    }

    /**
     * The account balance is cached to improve performance and reduce thrashing
     * of the GC system. The accountBalance is rest when transactions are added
     * and removed and lazily recalculated.
     *
     * @return the balance of this account
     */
    public BigDecimal getBalance() {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            if (accountBalance != null) {
                return accountBalance;
            }
            return accountBalance = getProxy().getBalance();
        } finally {
            l.unlock();
        }
    }

    /**
     * The account balance is cached to improve performance and reduce thrashing
     * of the GC system. The accountBalance is rest when transactions are added
     * and removed and lazily recalculated.
     *
     * @param node CurrencyNode to get balance against
     * @return the balance of this account
     */
    private BigDecimal getBalance(final CurrencyNode node) {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            return adjustForExchangeRate(getBalance(), node);
        } finally {
            l.unlock();
        }
    }

    /**
     * Get the account balance up to the specified index using the natural
     * transaction sort order
     *
     * @param index the balance of this account at the specified index.
     * @return the balance of this account at the specified index.
     */
    public BigDecimal getBalanceAt(final int index) {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            return getProxy().getBalanceAt(index);
        } finally {
            l.unlock();
        }
    }

    /**
     * Get the account balance up to the specified transaction using the natural
     * transaction sort order
     *
     * @param transaction reference transaction for running balance.  Must be contained within the account
     * @return  the balance of this account at the specified transaction
     */
    public BigDecimal getBalanceAt(final Transaction transaction) {
        Lock l = transactionLock.readLock();
        l.lock();

        BigDecimal balance = BigDecimal.ZERO;

        try {
            int index = transactions.indexOf(transaction);

            if (index >= 0) {
                balance = getBalanceAt(index);
            }

            return balance;
        } finally {
            l.unlock();
        }
    }

    /**
     * The reconciled balance is cached to improve performance and reduce
     * thrashing of the GC system. The reconciledBalance is reset when
     * transactions are added and removed and lazily recalculated.
     *
     * @return the reconciled balance of this account
     */
    public BigDecimal getReconciledBalance() {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            if (reconciledBalance != null) {
                return reconciledBalance;
            }

            return reconciledBalance = getProxy().getReconciledBalance();
        } finally {
            l.unlock();
        }
    }

    private BigDecimal getReconciledBalance(final CurrencyNode node) {
        return adjustForExchangeRate(getReconciledBalance(), node);
    }

    private BigDecimal adjustForExchangeRate(final BigDecimal amount, final CurrencyNode node) {
        if (node.equals(getCurrencyNode())) { // child has the same commodity type
            return amount;
        }

        // the account has a different currency, use the last known exchange rate
        return amount.multiply(getCurrencyNode().getExchangeRate(node));
    }

    /**
     * Returns the date of the first unreconciled transaction
     *
     * @return Date of first unreconciled transaction
     */
    public Date getFirstUnreconciledTransactionDate() {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            Date date = null;

            for (Transaction transaction : transactions) {
                if (transaction.getReconciled(this) == ReconciledState.NOT_RECONCILED) {
                    date = transaction.getDate();
                    break;
                }
            }

            if (date == null) {
                date = transactions.get(transactions.size() - 1).getDate();
            }

            return date;
        } finally {
            l.unlock();
        }
    }

    /**
     * Get the default opening balance for reconciling the account
     *
     * @return Opening balance for reconciling the account
     * @see AccountProxy#getOpeningBalanceForReconcile()
     */
    public BigDecimal getOpeningBalanceForReconcile() {
        return getProxy().getOpeningBalanceForReconcile();
    }

    /**
     * Returns the balance of the account plus any child accounts
     *
     * @return the balance of this account including the balance of any child
     *         accounts.
     */
    public BigDecimal getTreeBalance() {
        Lock l = transactionLock.readLock();
        l.lock();

        Lock cLock = childLock.readLock();
        cLock.lock();

        try {
            BigDecimal balance = getBalance();

            for (Account child : children) {
                balance = balance.add(child.getTreeBalance(getCurrencyNode()));
            }

            return balance;
        } finally {
            l.unlock();
            cLock.unlock();
        }
    }

    /**
     * Returns the balance of the account plus any child accounts. The balance
     * is adjusted to the current exchange rate of the supplied commodity if
     * needed.
     *
     * @param node The commodity to convert balance to
     * @return the balance of this account including the balance of any child
     *         accounts.
     */
    private BigDecimal getTreeBalance(final CurrencyNode node) {
        Lock l = transactionLock.readLock();
        l.lock();

        Lock cLock = childLock.readLock();
        cLock.lock();

        try {
            BigDecimal balance = getBalance(node);

            for (Account child : children) {
                balance = balance.add(child.getTreeBalance(node));
            }
            return balance;
        } finally {
            l.unlock();
            cLock.unlock();
        }
    }

    /**
     * Returns the reconciled balance of the account plus any child accounts.
     * The balance is adjusted to the current exchange rate of the supplied
     * commodity if needed.
     *
     * @param node The commodity to convert balance to
     * @return the balance of this account including the balance of any child
     *         accounts.
     */
    private BigDecimal getReconciledTreeBalance(final CurrencyNode node) {
        Lock l = transactionLock.readLock();
        l.lock();

        Lock cLock = childLock.readLock();
        cLock.lock();

        try {
            BigDecimal balance = getReconciledBalance(node);

            for (Account child : children) {
                balance = balance.add(child.getReconciledTreeBalance(node));
            }
            return balance;
        } finally {
            l.unlock();
            cLock.unlock();
        }
    }

    /**
     * Returns the reconciled balance of the account plus any child accounts.
     *
     * @return the balance of this account including the balance of any child
     *         accounts.
     */
    public BigDecimal getReconciledTreeBalance() {
        Lock l = transactionLock.readLock();
        l.lock();

        Lock cLock = childLock.readLock();
        cLock.lock();

        try {
            BigDecimal balance = getReconciledBalance();

            for (Account child : children) {
                balance = balance.add(child.getReconciledTreeBalance(getCurrencyNode()));
            }
            return balance;
        } finally {
            l.unlock();
            cLock.unlock();
        }
    }

    /**
     * Returns the balance of the transactions inclusive of the start and end
     * dates.
     *
     * @param start The inclusive start date
     * @param end   The inclusive end date
     * @return The ending balance
     */
    public BigDecimal getBalance(final Date start, final Date end) {
        assert start != null && end != null;

        Lock l = transactionLock.readLock();
        l.lock();

        try {
            return getProxy().getBalance(start, end);
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns the account balance up to and inclusive of the supplied date. The
     * returned balance is converted to the specified commodity.
     *
     * @param startDate start date
     * @param endDate   end date
     * @param node      The commodity to convert balance to
     * @return the account balance
     */
    public BigDecimal getBalance(final Date startDate, final Date endDate, final CurrencyNode node) {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            return adjustForExchangeRate(getBalance(startDate, endDate), node);
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns the the balance of the account plus any child accounts inclusive
     * of the start and end dates.
     *
     * @param start Start date inclusive
     * @param end   End date inclusive
     * @return recursive account balance
     */
    public BigDecimal getTreeBalance(final Date start, final Date end) {
        assert start != null && end != null;

        Lock l = transactionLock.readLock();
        l.lock();

        Lock cLock = childLock.readLock();
        cLock.lock();

        try {
            BigDecimal balance = getBalance(start, end);

            for (Account child : children) {
                balance = balance.add(child.getTreeBalance(start, end, getCurrencyNode()));
            }
            return balance;
        } finally {
            l.unlock();
            cLock.unlock();
        }
    }

    /**
     * Returns the full inclusive ancestry of this
     * <code>Account</code>.
     *
     * @return <code>List</code> of accounts
     */
    public List<Account> getAncestors() {
        List<Account> list = new ArrayList<>();
        list.add(this);

        Account parent = getParent();

        while (parent != null) {
            list.add(parent);
            parent = parent.getParent();
        }

        return list;
    }

    /**
     * Returns the the balance of the account plus any child accounts inclusive
     * of the start and end dates.
     *
     * @param start start date
     * @param end   end date
     * @param node  CurrencyNode to use for balance
     * @return account balance
     */
    public BigDecimal getTreeBalance(final Date start, final Date end, final CurrencyNode node) {
        assert start != null && end != null;

        Lock l = transactionLock.readLock();
        l.lock();

        Lock cLock = childLock.readLock();
        cLock.lock();

        try {
            BigDecimal returnValue = getBalance(start, end, node);

            for (Account child : children) {
                returnValue = returnValue.add(child.getTreeBalance(start, end, node));
            }
            return returnValue;
        } finally {
            l.unlock();
            cLock.unlock();
        }
    }

    /**
     * Returns the account balance up to and inclusive of the supplied date
     *
     * @param date The inclusive ending date
     * @return The ending balance
     */
    public BigDecimal getBalance(final Date date) {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            return getProxy().getBalance(date);
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns the account balance up to and inclusive of the supplied date. The
     * returned balance is converted to the specified commodity.
     *
     * @param node The commodity to convert balance to
     * @param date The inclusive ending date
     * @return The ending balance
     */
    public BigDecimal getBalance(final Date date, final CurrencyNode node) {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            return adjustForExchangeRate(getBalance(date), node);
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns an array of transactions that occur after account specified cut
     * off date. The returned array is inclusive of the specified dates.
     *
     * @param startDate starting cut off date
     * @param endDate   ending cut off date
     * @return the array of transactions that occurred between the specified
     *         dates
     */
    public List<Transaction> getTransactions(final Date startDate, final Date endDate) {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            final ArrayList<Transaction> list = new ArrayList<>();

            for (Transaction transaction : transactions) {
                if (DateUtils.after(transaction.getDate(), startDate, true) && DateUtils.before(transaction.getDate(), endDate, true)) {
                    list.add(transaction);
                }
            }

            return list;
        } finally {
            l.unlock();
        }
    }

    /**
     * Sets the commodity node for this account.
     *
     * @param node The new commodity node for this account.
     */
    final void setCurrencyNode(final CurrencyNode node) {
        if (node == null) {
            throw new IllegalArgumentException();
        }

        currencyNode = node;
    }

    /**
     * Returns the commodity node for this account.
     *
     * @return the commodity node for this account.
     */
    public final CurrencyNode getCurrencyNode() {
        return currencyNode;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(final boolean locked) {
        this.locked = locked;
    }

    public boolean isPlaceHolder() {
        return placeHolder;
    }

    public void setPlaceHolder(final boolean placeHolder) {
        this.placeHolder = placeHolder;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String desc) {
        description = desc;
    }

    public synchronized String getName() {
        return name;
    }

    public void setName(final String newName) {
        if (!newName.equals(name)) {
            name = newName;
            if (this.getParent() != null) { // tell the parent to resort

                getParent().sortChildren();
            }
        }
    }

    public synchronized String getPathName() {
        final Account parent = getParent();
        if (parent != null && parent.getAccountType() != AccountType.ROOT) {
            return parent.getPathName() + getAccountSeparator() + getName();
        }
        return getName(); // this account is at the root level
    }

    public AccountType getAccountType() {
        if (cachedAccountType == null) {
            cachedAccountType = AccountType.valueOf(accountType);
        }

        return cachedAccountType;
    }

    /**
     * Returns the visibility of the account
     *
     * @return boolean is this account is visible, false otherwise
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Changes the visibility of the account
     *
     * @param visible the new account visibility
     */
    public void setVisible(final boolean visible) {
        this.visible = visible;
    }

    /**
     * Sets the notes for this account
     *
     * @param notes the notes for this account
     */
    public void setNotes(final String notes) {
        this.notes = notes;
    }

    /**
     * Returns the notes for this account
     *
     * @return the notes for this account
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Compares two Account for ordering. The ID of the account is used as
     * account comparison if the accounts have equal names. It would be bad if
     * compareTo would tag the Accounts as equal when they really are not
     *
     * @param acc the
     *            <code>Account</code> to be compared.
     * @return the value
     *         <code>0</code> if the argument Account is equal to this Account; account
     *         value less than
     *         <code>0</code> if this Account is before the Account argument; and
     *         account value greater than
     *         <code>0</code> if this Account is after the Account argument.
     */
    @Override
    public int compareTo(final Account acc) {
        // sort on the full pathname, improves order for some cases.
        final int result = getPathName().compareToIgnoreCase(acc.getPathName());

        if (result == 0) {
            return getUuid().compareTo(acc.getUuid());
        }
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(final Object other) {
        return this == other || other instanceof Account && getUuid().equals(((Account) other).getUuid());
    }

    @Override
    public int hashCode() {
        return getUuid().hashCode();
    }

    public void setAccountNumber(final String account) {
        accountNumber = account;
    }

    /**
     * Returns the account number. A non-null value is guaranteed
     *
     * @return the account number
     */
    public String getAccountNumber() {
        return accountNumber;
    }

    /**
     * Adds account commodity to the list and ensures duplicates are not added.
     * The list is sorted according to numeric code
     *
     * @param node SecurityNode to add
     * @return true if successful
     */
    public boolean addSecurity(final SecurityNode node) {

        boolean result = false;

        if (node != null && memberOf(AccountGroup.INVEST) && !containsSecurity(node)) {
            securities.add(node);
            Collections.sort(securities);

            result = true;
        }

        return result;
    }

    /**
     * Remove a security
     *
     * @param node SecurityNode to remove
     * @return true if successful, false if used by a transaction
     */
    boolean removeSecurity(final SecurityNode node) {

        if (getUsedSecurities().contains(node)) {
            return false;
        }

        securities.remove(node);

        return true;
    }

    public boolean containsSecurity(final SecurityNode node) {
        return securities.contains(node);
    }

    /**
     * Returns the market value of this account
     *
     * @return market value of the account
     */
    public BigDecimal getMarketValue() {
        return getProxy().getMarketValue();
    }

    public Set<SecurityNode> getSecurities() {
        Set<SecurityNode> set = new TreeSet<>();

        for (SecurityNode node : securities) {
            set.add(node);
        }

        return set;
    }

    /**
     * Returns a set of used SecurityNodes
     *
     * @return a set of used SecurityNodes
     */
    public Set<SecurityNode> getUsedSecurities() {
        Set<SecurityNode> set = new TreeSet<>();

        Lock l = transactionLock.readLock();
        l.lock();

        try {
            for (Transaction t : transactions) {
                if (t instanceof InvestmentTransaction) {
                    set.add(((InvestmentTransaction) t).getSecurityNode());
                }
            }
        } finally {
            l.unlock();
        }
        return set;
    }

    /**
     * Returns the cash balance of this account
     *
     * @return Cash balance of the account
     */
    public BigDecimal getCashBalance() {
        Lock l = transactionLock.readLock();
        l.lock();

        try {
            return getProxy().getCashBalance();
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns the depth of the account relative to the
     * <code>RootAccount</code>
     *
     * @return depth relative to the root
     */
    public int getDepth() {
        int depth = 0;

        Account parent = getParent();

        while (parent != null) {
            depth++;
            parent = parent.getParent();
        }

        return depth;
    }

    /**
     * Shortcut method to check account type
     *
     * @param type AccountType to compare against
     * @return true if supplied AccountType match
     */
    public final boolean instanceOf(final AccountType type) {
        return getAccountType() == type;
    }

    /**
     * Shortcut method to check account group membership
     *
     * @param group AccountGroup to compare against
     * @return true if this account belongs to the supplied group
     */
    public final boolean memberOf(final AccountGroup group) {
        return getAccountType().getAccountGroup() == group;
    }

    public String getBankId() {
        return bankId;
    }

    public void setBankId(final String bankId) {
        this.bankId = bankId;
    }

    public boolean isExcludedFromBudget() {
        return excludedFromBudget;
    }

    public void setExcludedFromBudget(boolean excludeFromBudget) {
        this.excludedFromBudget = excludeFromBudget;
    }

    protected Object readResolve() {
        transactionLock = new ReentrantReadWriteLock(true);
        childLock = new ReentrantReadWriteLock(true);

        return this;
    }

    /**
     * When overridden, this should return account shallow copy only. Does not
     * include transactions or child accounts
     *
     * @return clone of this account
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        Account a = (Account) super.clone();
        a.securities = new ArrayList<>(securities);
        a.children.clear();
        a.transactions.clear();

        return a;
    }
}
