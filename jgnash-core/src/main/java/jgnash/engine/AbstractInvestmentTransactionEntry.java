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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import jgnash.util.NotNull;

/**
 * Investment Transaction Entry.
 *
 * @author Craig Cavanaugh
 */
@Entity
public abstract class AbstractInvestmentTransactionEntry extends TransactionEntry {

    /**
     * Security for this entry.
     */
    @ManyToOne
    private SecurityNode securityNode;

    /**
     * share price.
     */
    @Column(precision = 26, scale = 8)
    private BigDecimal price;

    /**
     * number of shares.
     */
    @Column(precision = 26, scale = 8)
    private BigDecimal quantity;

    /**
     * Creates a new instance of InvestmentTransactionEntry.
     */
    protected AbstractInvestmentTransactionEntry() {
        setTransactionTag(TransactionTag.INVESTMENT);
    }

    /**
     * Calculates the total of the value of the shares, gains, fees, etc. as it
     * pertains to an account.
     * <p>
     * <b>Not intended for use to calculate account balances</b>
     *
     * @return total resulting total for this entry
     * @see InvestmentTransaction#getTotal(jgnash.engine.Account)
     */
    public BigDecimal getTotal() {
        return getQuantity().multiply(getPrice());
    }

    public SecurityNode getSecurityNode() {
        return securityNode;
    }

    void setSecurityNode(final SecurityNode securityNode) {
        this.securityNode = securityNode;
    }

    public BigDecimal getPrice() {
        return price;
    }

    void setPrice(final BigDecimal price) {
        this.price = price;
    }

    /**
     * Assigns the number of shares for this transaction. The value should be
     * always be a positive value.
     *
     * @param quantity the quantity of securities to assign to this account
     * @see #getSignedQuantity()
     */
    void setQuantity(final BigDecimal quantity) {
        this.quantity = quantity;
    }

    /**
     * Returns the number of shares assigned to this transaction.
     *
     * @return the quantity of securities for this transaction
     * @see #getSignedQuantity()
     */
    public BigDecimal getQuantity() {
        return quantity;
    }

    /**
     * Returns the number of shares as it would impact the sum of the investment
     * accounts shares. Useful for summing share quantities
     *
     * @return the quantity of securities for this transaction
     */
    public abstract BigDecimal getSignedQuantity();

    /**
     * Returns the type of this transaction entry.
     *
     * @return the transaction type
     */
    @NotNull public abstract TransactionType getTransactionType();

    @Override
    public String toString() {

        return super.toString() + "Security:       " + getSecurityNode().getSymbol()
                + System.lineSeparator() + "Quantity:       " + getQuantity().toPlainString()
                + System.lineSeparator() + "Price:          " + getPrice().toPlainString()
                + System.lineSeparator();
    }
}
