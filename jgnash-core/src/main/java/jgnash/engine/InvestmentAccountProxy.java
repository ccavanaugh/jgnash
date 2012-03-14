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
package jgnash.engine;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;

/**
 * Investment Account Proxy class
 * 
 * @author Craig Cavanaugh
 * @version $Id: InvestmentAccountProxy.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class InvestmentAccountProxy extends AccountProxy {

    public InvestmentAccountProxy(final Account account) {
        super(account);
    }

    @Override
    public BigDecimal getBalance(final Date start, final Date end) {
        return getCashBalance(start, end).add(getMarketValue(start, end));
    }

    /**
     * Returns the cash balance plus the market value of the shares
     * 
     * @return cash balance
     */
    @Override
    public BigDecimal getBalance() {
        return getCashBalance().add(getMarketValue());
    }

    @Override
    public BigDecimal getBalance(final Date date) {
        return getCashBalance(date).add(getMarketValue(date));
    }

    /**
     * Returns the cash balance of this account
     */
    @Override
    public BigDecimal getCashBalance() {
        return super.getBalance();
    }

    /**
     * Get the account's cash balance up to a specified index.
     * 
     * @param index the balance of the account at the specified index.
     * @return the balance of the account at the specified index.
     */
    private BigDecimal getCashBalanceAt(final int index) {
        return super.getBalanceAt(index);
    }

    /**
     * Returns the balance of the transactions inclusive of the start and end dates.
     * <p>
     * The balance includes the cash transactions and is based on the current market value.
     * 
     * @param start The inclusive start date
     * @param end The inclusive end date
     * @return The ending balance
     */
    @Override
    public BigDecimal getCashBalance(final Date start, final Date end) {
        return super.getBalance(start, end);
    }

    /**
     * Returns the cash account balance up to and inclusive of the supplied date.
     * 
     * @param end The inclusive ending date
     * @return The ending cash balance
     */
    @Override
    public BigDecimal getCashBalance(final Date end) {
        final Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {
            return !account.transactions.isEmpty() ? getCashBalance(account.transactions.get(0).getDate(), end) : BigDecimal.ZERO;
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns a market price for the supplied <code>SecurityNode</code> that is closest to the supplied date without
     * exceeding it. The history of the <code>SecurityNode</code> is searched as well as the account's transaction
     * history to find the closest market price without exceeding the supplied date.
     * 
     * @param node security to search against
     * @param date date to search against
     * @return market price
     */
    @Override
    public BigDecimal getMarketPrice(final SecurityNode node, final Date date) {
        return Engine.getMarketPrice(account.getReadonlyTransactionList(), node, account.getCurrencyNode(), date);
    }

    /**
     * Returns the market value of this account.
     * 
     * @return the market value of the account
     */
    @Override
    public BigDecimal getMarketValue() {
        final Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {

            BigDecimal marketValue = BigDecimal.ZERO;

            int count = account.getTransactionCount();

            if (count > 0) {
                Date lastDate = account.transactions.get(count - 1).getDate();

                /*
                 * If the user was to enter a date value greater than the current date, then
                 * "new Date()" is not sufficient to pick up the last transaction.  If the
                 * current date is greater, than it is used to force use of the latest
                 * security price.
                 */

                if (lastDate.compareTo(new Date()) >= 0) {
                    marketValue = getMarketValue(account.transactions.get(0).getDate(), lastDate);
                } else {
                    marketValue = getMarketValue(account.transactions.get(0).getDate(), new Date());
                }
            }

            return marketValue;
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns the market value of the account at a specified date. The closest market price is used and only investment
     * transactions earlier and inclusive of the specified date are considered.
     * 
     * @param date the end date to calculate the market value
     * @return the ending balance
     */
    @Override
    public BigDecimal getMarketValue(final Date date) {
        final Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {
            BigDecimal marketValue = BigDecimal.ZERO;

            if (account.getTransactionCount() > 0) {
                marketValue = getMarketValue(account.transactions.get(0).getDate(), date);
            }

            return marketValue;
        } finally {
            l.unlock();
        }
    }

    @Override
    public BigDecimal getMarketValue(final Date start, final Date end) {
        final Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {
            final HashMap<SecurityNode, BigDecimal> priceMap = new HashMap<>();

            // build lookup map for market prices
            for (SecurityNode node : account.securities) {
                priceMap.put(node, getMarketPrice(node, end));
            }

            BigDecimal balance = BigDecimal.ZERO;

            final int count = account.getTransactionCount();

            for (int i = 0; i < count; i++) {
                Transaction t = account.getTransactionAt(i);
                if (t.getDate().compareTo(start) >= 0 && t.getDate().compareTo(end) <= 0) {
                    if (t instanceof InvestmentTransaction) {
                        balance = balance.add(((InvestmentTransaction) t).getMarketValue(priceMap.get(((InvestmentTransaction) t).getSecurityNode())));
                    }
                }
            }

            return balance;
        } finally {
            l.unlock();
        }
    }

    /**
     * Calculates the accounts market value based on the latest security price
     * 
     * @param index index to calculate the balance to
     * @return market value
     */
    private BigDecimal getMarketValueAt(final int index) {
        final Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {
            final HashMap<SecurityNode, BigDecimal> priceMap = new HashMap<>();

            Date today = new Date();

            // build lookup map for market prices
            for (SecurityNode node : account.securities) {
                priceMap.put(node, getMarketPrice(node, today));
            }

            BigDecimal balance = BigDecimal.ZERO;

            for (int i = 0; i <= index; i++) {
                Transaction t = account.getTransactionAt(i);

                if (t instanceof InvestmentTransaction) {
                    balance = balance.add(((InvestmentTransaction) t).getMarketValue(priceMap.get(((InvestmentTransaction) t).getSecurityNode())));
                }
            }

            return balance;
        } finally {
            l.unlock();
        }
    }

    private BigDecimal getReconciledMarketValue() {
        final Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {
            final HashMap<SecurityNode, BigDecimal> priceMap = new HashMap<>();

            // build lookup map for market prices
            for (SecurityNode node : account.securities) {
                priceMap.put(node, getMarketPrice(node, new Date()));
            }

            BigDecimal balance = BigDecimal.ZERO;

            for (Transaction t : account.transactions) {
                if (t instanceof InvestmentTransaction && t.getReconciled(account) == ReconciledState.RECONCILED) {
                    balance = balance.add(((InvestmentTransaction) t).getMarketValue(priceMap.get(((InvestmentTransaction) t).getSecurityNode())));
                }
            }

            return balance;
        } finally {
            l.unlock();
        }
    }

    /**
     * Calculates the reconciled balance of the account
     * 
     * @return the reconciled balance of this account
     */
    @Override
    public BigDecimal getReconciledBalance() {
        return super.getReconciledBalance().add(getReconciledMarketValue());
    }

    /**
     * Get the default opening balance for reconciling the account
     * 
     * @return Opening balance for reconciling the account
     */
    @Override
    public BigDecimal getOpeningBalanceForReconcile() {

        final Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {
            final Date date = account.getFirstUnreconciledTransactionDate();

            BigDecimal balance = BigDecimal.ZERO;

            for (int i = 0; i < account.transactions.size(); i++) {
                if (account.transactions.get(i).getDate().equals(date)) {
                    if (i > 0) {
                        balance = getCashBalanceAt(i - 1).add(getMarketValueAt(i - 1));
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
