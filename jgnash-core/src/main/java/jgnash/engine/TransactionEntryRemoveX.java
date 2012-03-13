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

/**
 * Remove shares without impacting the cash balance.  This is a single
 * entry transaction
 *
 * @author Craig Cavanaugh
 * @version $Id: TransactionEntryRemoveX.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public final class TransactionEntryRemoveX extends AbstractInvestmentTransactionEntry {

    /**
     * No argument constructor for reflection purposes.
     * <b>Do not use to create a new instance</b>
     *
     * @deprecated
     */
    @Deprecated
    public TransactionEntryRemoveX() {
    }

    public TransactionEntryRemoveX(Account account, SecurityNode securityNode, BigDecimal price, BigDecimal quantity) {
        setCreditAccount(account);
        setDebitAccount(account);

        setPrice(price);
        setQuantity(quantity);
        setSecurityNode(securityNode);
    }

    /**
     * Returns the number of shares as it would impact
     * the sum of the investment accounts shares. Useful
     * for summing share quantities
     *
     * @return the quantity of securities for this transaction
     */
    @Override
    public BigDecimal getSignedQuantity() {
        return getQuantity().negate();
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.REMOVESHARE;
    }

    @Override
    public void setCreditAccount(Account creditAccount) {
        super.setCreditAccount(creditAccount);
        super.setDebitAccount(creditAccount);
    }

    @Override
    public void setDebitAccount(Account debitAccount) {
        super.setDebitAccount(debitAccount);
        super.setCreditAccount(debitAccount);
    }
}
