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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * Investment Account Proxy class.
 *
 * @author Craig Cavanaugh
 */
class InvestmentAccountProxy extends AccountProxy {

    public InvestmentAccountProxy(final Account account) {
        super(account);
    }

    @Override
    public BigDecimal getBalance(final LocalDate start, final LocalDate end) {
        return getCashBalance(start, end).add(getMarketValue(start, end));
    }

    /**
     * Returns the cash balance plus the market value of the shares.
     *
     * @return cash balance
     */
    @Override
    public BigDecimal getBalance() {
        return getCashBalance().add(getMarketValue());
    }

    @Override
    public BigDecimal getBalance(final LocalDate date) {
        return getCashBalance(date).add(getMarketValue(date));
    }

    /**
     * Returns the cash balance of this account.  Cash balance may be referred to as the "sweep" account where
     * the money market fund (cash) does not have it's own account number and the user see's it as a cash balance
     * in their account statements.
     * 
     * @return cash balance of the account
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
     * @param end   The inclusive end date
     * @return The ending balance
     */
    private BigDecimal getCashBalance(final LocalDate start, final LocalDate end) {
        return super.getBalance(start, end);
    }

    /**
     * Returns the cash account balance up to and inclusive of the supplied date.
     *
     * @param end The inclusive ending date
     * @return The ending cash balance
     */
    private BigDecimal getCashBalance(final LocalDate end) {
        final Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {
            return !account.transactions.isEmpty()
                    ? getCashBalance(account.getSortedTransactionList().get(0).getLocalDate(), end) : BigDecimal.ZERO;
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns a market price for the supplied {@code SecurityNode} that is closest to the supplied date without
     * exceeding it. The history of the {@code SecurityNode} is searched as well as the account's transaction
     * history to find the closest market price without exceeding the supplied date.
     *
     * @param node security to search against
     * @param date date to search against
     * @return market price
     */
    private BigDecimal getMarketPrice(final SecurityNode node, final LocalDate date) {
        account.getTransactionLock().readLock().lock();

        try {
            return Engine.getMarketPrice(account.getSortedTransactionList(), node, account.getCurrencyNode(), date);
        } finally {
            account.getTransactionLock().readLock().unlock();
        }
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
                LocalDate lastDate = account.getSortedTransactionList().get(count - 1).getLocalDate();

                /*
                 * If the user was to enter a date value greater than the current date, then
                 * "new Date()" is not sufficient to pick up the last transaction.  If the
                 * current date is greater, than it is used to force use of the latest
                 * security price.
                 */

                final LocalDate startDate = account.getSortedTransactionList().get(0).getLocalDate();

                if (lastDate.compareTo(LocalDate.now()) >= 0) {
                    marketValue = getMarketValue(startDate, lastDate);
                } else {
                    marketValue = getMarketValue(startDate, LocalDate.now());
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
    private BigDecimal getMarketValue(final LocalDate date) {
        final Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {
            BigDecimal marketValue = BigDecimal.ZERO;

            if (account.getTransactionCount() > 0) {
                marketValue = getMarketValue(account.getSortedTransactionList().get(0).getLocalDate(), date);
            }

            return marketValue;
        } finally {
            l.unlock();
        }
    }

    /**
     * Returns the market value for an account.
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @return market value
     */
    private BigDecimal getMarketValue(final LocalDate start, final LocalDate end) {
        final Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {
            final HashMap<SecurityNode, BigDecimal> priceMap = new HashMap<>();

            // build lookup map for market prices
            for (final SecurityNode node : account.getSecurities()) {
                priceMap.put(node, getMarketPrice(node, end));
            }

            BigDecimal balance = BigDecimal.ZERO;

            // Get a defensive copy, JPA lazy updates can have side effects
            List<Transaction> transactions = account.getSortedTransactionList();

            for (final Transaction t : transactions) {
                if (t.getLocalDate().compareTo(start) >= 0 && t.getLocalDate().compareTo(end) <= 0) {
                    if (t instanceof InvestmentTransaction) {
                        balance = balance.add(((InvestmentTransaction) t).getMarketValue(priceMap.get(((InvestmentTransaction) t).getSecurityNode())));
                    }
                }
            }

            return round(balance);
        } finally {
            l.unlock();
        }
    }

    /**
     * Calculates the accounts market value based on the latest security price.
     *
     * @param index index to calculate the balance to
     * @return market value
     */
    private BigDecimal getMarketValueAt(final int index) {
        final Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {
            final HashMap<SecurityNode, BigDecimal> priceMap = new HashMap<>();

            LocalDate today = LocalDate.now();

            // build lookup map for market prices
            for (final SecurityNode node : account.getSecurities()) {
                priceMap.put(node, getMarketPrice(node, today));
            }

            BigDecimal balance = BigDecimal.ZERO;

            for (int i = 0; i <= index; i++) {
                Transaction t = account.getTransactionAt(i);

                if (t instanceof InvestmentTransaction) {
                    balance = balance.add(((InvestmentTransaction) t).getMarketValue(priceMap.get(((InvestmentTransaction) t).getSecurityNode())));
                }
            }

            return round(balance);
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
            for (final SecurityNode node :account.getSecurities()) {
                priceMap.put(node, getMarketPrice(node, LocalDate.now()));
            }

            BigDecimal balance = BigDecimal.ZERO;

            // Get a defensive copy, JPA lazy updates can have side effects
            List<Transaction> transactions = account.getSortedTransactionList();

            for (final Transaction t : transactions) {
                if (t instanceof InvestmentTransaction && t.getReconciled(account) == ReconciledState.RECONCILED) {
                    balance = balance.add(((InvestmentTransaction) t).getMarketValue(priceMap.get(((InvestmentTransaction) t).getSecurityNode())));
                }
            }

            return round(balance);
        } finally {
            l.unlock();
        }
    }

    /**
     * Calculates the reconciled balance of the account.
     *
     * @return the reconciled balance of this account
     */
    @Override
    public BigDecimal getReconciledBalance() {
        return super.getReconciledBalance().add(getReconciledMarketValue());
    }

    /**
     * Get the default opening balance for reconciling the account.
     *
     * @return Opening balance for reconciling the account
     */
    @Override
    public BigDecimal getOpeningBalanceForReconcile() {

        final Lock l = account.getTransactionLock().readLock();
        l.lock();

        try {
            final LocalDate date = account.getFirstUnreconciledTransactionDate();

            BigDecimal balance = BigDecimal.ZERO;

            final List<Transaction> transactions = account.getSortedTransactionList();

            for (int i = 0; i < transactions.size(); i++) {
                if (transactions.get(i).getLocalDate().equals(date)) {
                    if (i > 0) {
                        balance = getCashBalanceAt(i - 1).add(getMarketValueAt(i - 1));
                    }
                    break;
                }
            }

            return round(balance);
        } finally {
            l.unlock();
        }
    }

    /**
     * Scales / Rounds a given value to the scale of the accounts currency.  Calculating Market value will result
     * in minor discrepancies.  Use this before returning values for consistent calculations.
     *
     * @param value value to round
     * @return rounded value
     */
    private BigDecimal round(final BigDecimal value) {
        return value.setScale(account.getCurrencyNode().getScale(), MathConstants.roundingMode);
    }
}
