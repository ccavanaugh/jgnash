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

import javax.persistence.Entity;

import jgnash.util.NotNull;
import jgnash.resource.util.ResourceUtils;

/**
 * Remove shares without impacting the cash balance. This is a single entry transaction
 * 
 * @author Craig Cavanaugh
 */
@Entity
public class TransactionEntryMergeX extends AbstractInvestmentTransactionEntry {

    /**
     * No argument constructor for reflection purposes.
     * <p>
     * <b>Do not use to create a new instance</b>
     */
    @SuppressWarnings("unused")
    public TransactionEntryMergeX() {
    }

    public TransactionEntryMergeX(final Account investmentAccount, final SecurityNode securityNode, final BigDecimal price, final BigDecimal quantity) {

        if (investmentAccount.getAccountType().getAccountGroup() != AccountGroup.INVEST) {
            throw new RuntimeException(ResourceUtils.getString("Message.Error.InvalidAccountGroup"));
        }

        setCreditAccount(investmentAccount);
        setDebitAccount(investmentAccount);

        setPrice(price);
        setQuantity(quantity);
        setSecurityNode(securityNode);
    }

    /**
     * Returns the number of shares as it would impact the sum of the investment accounts shares. Useful for summing
     * share quantities
     * 
     * @return the quantity of securities for this transaction
     */
    @Override
    public BigDecimal getSignedQuantity() {
        return getQuantity().negate();
    }

    @Override
    @NotNull
    public TransactionType getTransactionType() {
        return TransactionType.MERGESHARE;
    }

    @Override
    public void setCreditAccount(final Account creditAccount) {
        super.setCreditAccount(creditAccount);
        super.setDebitAccount(creditAccount);
    }

    @Override
    public void setDebitAccount(final Account debitAccount) {
        super.setDebitAccount(debitAccount);
        super.setCreditAccount(debitAccount);
    }
}
