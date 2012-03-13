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

import jgnash.util.Resource;

/**
 * Add shares without impacting the cash balance. This is a single entry transaction
 * 
 * @author Craig Cavanaugh
 * @version $Id: TransactionEntrySplitX.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class TransactionEntrySplitX extends TransactionEntryAbstractIncrease {

    /**
     * No argument constructor for reflection purposes.
     * <p>
     * <b>Do not use to create a new instance</b>
     * 
     * @deprecated
     */
    @Deprecated
    public TransactionEntrySplitX() {
    }

    public TransactionEntrySplitX(final Account investmentAccount, final SecurityNode securityNode, final BigDecimal price, final BigDecimal quantity) {

        if (investmentAccount.getAccountType().getAccountGroup() != AccountGroup.INVEST) {
            throw new RuntimeException(Resource.get().getString("Message.ErrorInvalidAccountGroup"));
        }

        setCreditAccount(investmentAccount);
        setDebitAccount(investmentAccount);

        setPrice(price);
        setQuantity(quantity);
        setSecurityNode(securityNode);
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.SPLITSHARE;
    }
}
