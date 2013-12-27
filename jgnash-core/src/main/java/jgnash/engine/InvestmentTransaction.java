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

import javax.persistence.Entity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Class for investment transactions.
 *
 * All TransactionEntry(s) must be of the same security
 *
 * @author Craig Cavanaugh
 */
@Entity
public class InvestmentTransaction extends Transaction {

    public InvestmentTransaction() {
        super();
    }

    @Override
    public TransactionType getTransactionType() {
        TransactionType type = null;

        getLock().readLock().lock();

        try {
            for (TransactionEntry e : transactionEntries) {
                if (e instanceof AbstractInvestmentTransactionEntry) {
                    type = ((AbstractInvestmentTransactionEntry) e).getTransactionType();
                }
            }
        } finally {
            getLock().readLock().unlock();
        }

        return type;
    }

    public Account getInvestmentAccount() {

        Account account = null;

        getLock().readLock().lock();

        try {
            for (TransactionEntry e : transactionEntries) {
                if (e.getCreditAccount().getAccountType().getAccountGroup() == AccountGroup.INVEST) {
                    account = e.getCreditAccount();
                } else if (e.getDebitAccount().getAccountType().getAccountGroup() == AccountGroup.INVEST) {
                    account = e.getDebitAccount();
                }
            }
        } finally {
            getLock().readLock().unlock();
        }

        return account;
    }

    @Override
    public void addTransactionEntry(final TransactionEntry entry) {
        if (entry instanceof AbstractInvestmentTransactionEntry) {
            TransactionType type = getTransactionType();
            SecurityNode node = getSecurityNode();

            if (type != null) {
                assert ((AbstractInvestmentTransactionEntry) entry).getTransactionType() == type;
            }

            if (node != null) {
                assert ((AbstractInvestmentTransactionEntry) entry).getSecurityNode().equals(node);
            }
        }
        super.addTransactionEntry(entry);
    }

    /**
     * Returns the price per share
     *
     * @return the price per share
     */
    public BigDecimal getPrice() {
        BigDecimal price = BigDecimal.ZERO;

        for (TransactionEntry e : transactionEntries) {
            if (e instanceof AbstractInvestmentTransactionEntry) {
                price = ((AbstractInvestmentTransactionEntry) e).getPrice();
                break;
            }
        }
        return price;
    }

    /**
     * Returns the number of shares assigned to this transaction.
     *
     * @return the quantity of securities for this transaction
     * @see #getSignedQuantity()
     */
    public BigDecimal getQuantity() {
        BigDecimal quantity = BigDecimal.ZERO;

        for (TransactionEntry e : transactionEntries) {
            if (e instanceof AbstractInvestmentTransactionEntry) {
                quantity = quantity.add(((AbstractInvestmentTransactionEntry) e).getQuantity());
            }
        }
        return quantity;
    }

    /**
     * Returns the number of shares assigned to this transaction.
     *
     * @return the quantity of securities for this transaction
     * @see #getSignedQuantity()
     */
    BigDecimal getSignedQuantity() {
        BigDecimal quantity = BigDecimal.ZERO;

        for (TransactionEntry e : transactionEntries) {
            if (e instanceof AbstractInvestmentTransactionEntry) {
                quantity = quantity.add(((AbstractInvestmentTransactionEntry) e).getSignedQuantity());
            }
        }
        return quantity;
    }

    public SecurityNode getSecurityNode() {
        SecurityNode node = null;

        getLock().readLock().lock();

        try {
            for (TransactionEntry e : transactionEntries) {
                if (e instanceof AbstractInvestmentTransactionEntry) {
                    node = ((AbstractInvestmentTransactionEntry) e).getSecurityNode();
                }
            }
        } finally {
            getLock().readLock().unlock();
        }

        return node;
    }

    /**
     * Sum transaction fees
     *
     * @return transaction fees
     */
    public BigDecimal getFees() {
        return getFees(getInvestmentAccount());
    }

    /**
     * Sum transaction fees
     *
     * @param account account to calculate fees against
     * @return transaction fees
     */
    BigDecimal getFees(final Account account) {
        BigDecimal fees = BigDecimal.ZERO;

        getLock().readLock().lock();

        try {
            for (TransactionEntry e : transactionEntries) {
                if (e.getTransactionTag() == TransactionTag.INVESTMENT_FEE) {
                    fees = fees.add(e.getAmount(account));
                }
            }
        } finally {
            getLock().readLock().unlock();
        }

        return fees.negate();
    }

    /**
     * Get a list of transaction entries tagged as investment fees
     *
     * @return list of investment fees
     * @see TransactionTag#INVESTMENT_FEE
     */
    public List<TransactionEntry> getInvestmentFeeEntries() {
        return getTransactionEntriesByTag(TransactionTag.INVESTMENT_FEE);
    }

    /**
     * Get a list of transaction entries tagged as gains and loss
     *
     * @return list of gains and loss entries
     * @see TransactionTag#GAIN_LOSS
     */
    public List<TransactionEntry> getInvestmentGainLossEntries() {
        return getTransactionEntriesByTag(TransactionTag.GAIN_LOSS);
    }

    /**
     * Return the market value of the transaction based on the supplied share
     * price
     *
     * @param sharePrice share price
     * @return the value of this transaction
     */
    public BigDecimal getMarketValue(final BigDecimal sharePrice) {
        return getSignedQuantity().multiply(sharePrice);
    }

    /**
     * Return the market value of the transaction based on the latest share
     * price
     *
     * @param date Date to base market value against
     * @return the value of this transaction
     */
    public BigDecimal getMarketValue(final Date date) {
        return getSignedQuantity().multiply(
                getSecurityNode().getMarketPrice(date, getInvestmentAccount().getCurrencyNode()));
    }

    /**
     * Calculates the total of the value of the shares, gains, fees, etc. as it
     * pertains to an account.
     * <p/>
     * <
     * p/>
     * <b>Not intended for use to calculate account balances</b>
     *
     * @param account The <code>Account</code> to calculate the total against
     * @return total resulting total for this transaction
     * @see AbstractInvestmentTransactionEntry#getTotal()
     */
    public BigDecimal getTotal(final Account account) {

        BigDecimal total = BigDecimal.ZERO;

        for (TransactionEntry e : transactionEntries) {
            if (e instanceof AbstractInvestmentTransactionEntry) {
                total = total.add(((AbstractInvestmentTransactionEntry) e).getTotal());
            } else {
                total = total.add(e.getAmount(account));
            }
        }
        return total;
    }

    /**
     * Calculates the total cash value of the transaction
     * <p/>
     * <b>Not intended for use to calculate account balances</b>
     *
     * @return the total cash value of the transaction
     */
    public BigDecimal getNetCashValue() {
        BigDecimal total = getQuantity().multiply(getPrice());

        switch (getTransactionType()) {
            case DIVIDEND:
                return getAmount(getInvestmentAccount());
            case REINVESTDIV:
            case SELLSHARE:
                total = total.subtract(getFees());
                break;
            case BUYSHARE:
                total = total.add(getFees());
                break;
            default:
                break;
        }

        return total;
    }

    /**
     * Compares two Transactions for ordering. Equality is checked for at the
     * reference level. If a comparison cannot be determined, the hashCode is
     * used
     *
     * @param tran the <code>Transaction</code> to be compared.
     * @return the value <code>0</code> if the argument Transaction is equal to
     * this Transaction; a value less than <code>0</code> if this Transaction is
     * before the Transaction argument; and a value greater than <code>0</code>
     * if this Transaction is after the Transaction argument.
     */
    @Override
    public int compareTo(final Transaction tran) {
        if (tran == this) {
            return 0;
        }

        int result = getDate().compareTo(tran.getDate());
        if (result != 0) {
            return result;
        }

        if (getTransactionType() == null) {
            System.out.println("null transaction type!");
        }

        result = getTransactionType().name().compareTo(tran.getTransactionType().name());
        if (result != 0) {
            return result;
        }

        result = getMemo().compareTo(tran.getMemo());
        if (result != 0) {
            return result;
        }

        if (tran instanceof InvestmentTransaction) {
            result = getSecurityNode().compareTo(((InvestmentTransaction) tran).getSecurityNode());
            if (result != 0) {
                return result;
            }
        }

        result = getDateEntered().compareTo(tran.getDateEntered());
        if (result != 0) {
            return result;
        }

        return getUuid().compareTo(tran.getUuid());
    }
}
