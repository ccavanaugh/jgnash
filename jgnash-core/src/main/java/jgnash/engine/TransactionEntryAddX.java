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

/**
 * Add shares without impacting the cash balance.  This is a single
 * entry transaction
 *
 * @author Craig Cavanaugh
 */
@Entity
public class TransactionEntryAddX extends TransactionEntryAbstractIncrease {

    /**
     * No argument constructor for reflection purposes.
     * <b>Do not use to create a new instance</b>
     */
    @SuppressWarnings("unused")
    public TransactionEntryAddX() {
    }

    public TransactionEntryAddX(final Account account, final SecurityNode securityNode, final BigDecimal price, final BigDecimal quantity) {
        setCreditAccount(account);
        setDebitAccount(account);

        setPrice(price);
        setQuantity(quantity);
        setSecurityNode(securityNode);
    }

    @Override
    @NotNull
    public TransactionType getTransactionType() {
        return TransactionType.ADDSHARE;
    }
}
