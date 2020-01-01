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

/**
 * Abstract Add transaction.
 *
 * @author Craig Cavanaugh
 */
@Entity
public abstract class TransactionEntryAbstractIncrease extends AbstractInvestmentTransactionEntry {

    protected TransactionEntryAbstractIncrease() {
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
        return getQuantity();
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
