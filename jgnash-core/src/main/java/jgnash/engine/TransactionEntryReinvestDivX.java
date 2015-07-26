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

import javax.persistence.Entity;

import jgnash.util.NotNull;
import jgnash.util.ResourceUtils;

/**
 * Reinvest dividend transaction
 * 
 * @author Craig Cavanaugh
 */
@Entity
public class TransactionEntryReinvestDivX extends TransactionEntryAbstractIncrease {

    /**
     * No argument constructor for reflection purposes.
     * <p>
     * <b>Do not use to create a new instance</b>
     */
    @SuppressWarnings("unused")
    public TransactionEntryReinvestDivX() {
    }

    public TransactionEntryReinvestDivX(final Account investmentAccount, final SecurityNode securityNode, final BigDecimal price, final BigDecimal quantity) {

        if (investmentAccount.getAccountType().getAccountGroup() != AccountGroup.INVEST) {
            throw new RuntimeException(ResourceUtils.getString("Message.Error.InvalidAccountGroup"));
        }

        setSecurityNode(securityNode);
        setPrice(price);
        setQuantity(quantity);
        setCreditAccount(investmentAccount);
        setDebitAmount(BigDecimal.ZERO);
        setCreditAmount(BigDecimal.ZERO);
    }

    /**
     * Returns {@code TransactionType.REINVESTDIV} for this type of transaction
     * 
     * @see TransactionType#REINVESTDIV
     * @return returns {@code TransactionType.REINVESTDIV)
     */
    @Override
    @NotNull
    public TransactionType getTransactionType() {
        return TransactionType.REINVESTDIV;
    }
}
