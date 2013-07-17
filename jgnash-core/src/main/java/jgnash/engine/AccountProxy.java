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

import jgnash.util.DateUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * Proxy class to locate account balance behaviors. Depending on account type, summation of transaction types are
 * handled differently.
 *
 * @author Craig Cavanaugh
 */
class AccountProxy {

    final Account account;

    AccountProxy(final Account account) {
        this.account = account;
    }

    public BigDecimal getBalance() {
        int count = account.getTransactionCount();

        if (count > 0) {
            return getBalanceAt(count - 1);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get the account balance up to a specified index.
     *
     * @param index the balance of this account at the specified index.
     * @return the balance of this account at the specified index.
     */
    public BigDecimal getBalanceAt(final int index) {
        Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {
            BigDecimal balance = BigDecimal.ZERO;

            List<Transaction> transactions = account.getSortedTransactionList();

            for (int i = 0; i <= index; i++) {
                balance = balance.add(transactions.get(i).getAmount(account));
            }
            return balance;
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns the balance of the transactions inclusive of the start and end dates.
     *
     * @param start The inclusive start date
     * @param end   The inclusive end date
     * @return The ending balance
     */
    public BigDecimal getBalance(final Date start, final Date end) {
        Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {
            BigDecimal balance = BigDecimal.ZERO;

            for (final Transaction t : account.transactions) {
                final Date d = t.getDate();

                if (DateUtils.after(d, start) && DateUtils.before(d, end)) {
                    balance = balance.add(t.getAmount(account));
                }
            }

            return balance;
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns the account balance up to and inclusive of the supplied date
     *
     * @param date The inclusive ending date
     * @return The ending balance
     */
    public BigDecimal getBalance(final Date date) {
        Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {
            BigDecimal balance = BigDecimal.ZERO;

            if (!account.transactions.isEmpty()) {
                balance = getBalance(account.getSortedTransactionList().get(0).getDate(), date);
            }

            return balance;
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns the cash balance of this account
     *
     * @return exception thrown
     */
    public BigDecimal getCashBalance() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the cash account balance up to and inclusive of the supplied date
     *
     * @param end The inclusive ending date
     * @return The ending cash balance
     */
    public BigDecimal getCashBalance(final Date end) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the balance of the transactions inclusive of the start and end dates.
     * <p/>
     * The balance includes the cash transactions and is based on current market value.
     *
     * @param start The inclusive start date
     * @param end   The inclusive end date
     * @return The ending balance
     */
    public BigDecimal getCashBalance(final Date start, final Date end) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a market price for the commodity that is closest to the supplied date without exceeding it.
     *
     * @param node commodity to search against
     * @param date date to search against
     * @return share price
     */
    public BigDecimal getMarketPrice(final SecurityNode node, final Date date) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the market value of this account
     *
     * @return exception thrown
     */
    public BigDecimal getMarketValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the market value of the account at a specified date. The closest market price is used and only investment
     * transactions earlier and inclusive of the specified date are considered.
     *
     * @param date the end date to calculate the market value
     * @return the ending balance
     */
    public BigDecimal getMarketValue(final Date date) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the market value for an account
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @return market value
     */
    public BigDecimal getMarketValue(final Date start, final Date end) {
        throw new UnsupportedOperationException();
    }

    /**
     * Calculates the reconciled balance of the account
     *
     * @return the reconciled balance of this account
     */
    public BigDecimal getReconciledBalance() {
        Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {
            BigDecimal balance = BigDecimal.ZERO;

            for (Transaction t : account.transactions) {
                if (t.getReconciled(account) == ReconciledState.RECONCILED) {
                    balance = balance.add(t.getAmount(account));
                }
            }

            return balance;
        } finally {
            l.unlock();
        }
    }

    /**
     * Get the default opening balance for reconciling the account
     *
     * @return Opening balance for reconciling the account
     */
    public BigDecimal getOpeningBalanceForReconcile() {
        Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {
            final Date date = account.getFirstUnreconciledTransactionDate();

            final List<Transaction> transactions = account.getSortedTransactionList();

            BigDecimal balance = BigDecimal.ZERO;

            for (int i = 0; i < transactions.size(); i++) {
                if (transactions.get(i).getDate().equals(date)) {
                    if (i > 0) {
                        balance = getBalanceAt(i - 1);
                    }
                    break;
                }
            }
            return balance;
        } finally {
            l.unlock();
        }
    }
}
