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
import java.util.Objects;

import javax.persistence.Entity;

import jgnash.util.NotNull;

/**
 * Investment dividend.
 * <p>
 * The creditAccount and creditAmount fields are used for the investment account.
 * The debitAccount and debitAmount fields are used for the capital gains (income) account.
 * creditAccount is assumed to be the investment account.
 *
 * @author Craig Cavanaugh
 */
@Entity
public class TransactionEntryDividendX extends AbstractInvestmentTransactionEntry {

    /**
     * No argument constructor for reflection purposes only.
     * <b>Do not use to create a new instance</b>
     */
    @SuppressWarnings("unused")
    public TransactionEntryDividendX() {

    }

    /**
     * Constructor.
     *
     * @param incomeAccount         Debit account
     * @param investmentAccount     Credit/Investment  account
     * @param securityNode          Security for the transaction
     * @param dividend              Dividend received
     * @param incomeExchangedAmount Exchanged amount for the debit account
     */
    TransactionEntryDividendX(final Account incomeAccount, final Account investmentAccount, final SecurityNode securityNode, final BigDecimal dividend, final BigDecimal incomeExchangedAmount) {
        Objects.requireNonNull(incomeAccount);
        Objects.requireNonNull(investmentAccount);

        assert investmentAccount.memberOf(AccountGroup.INVEST);
        assert dividend.signum() >= 0 && incomeExchangedAmount.signum() <= 0;

        setSecurityNode(securityNode);

        /* Dividends do not involve exchange of shares */
        setPrice(BigDecimal.ZERO);
        setQuantity(BigDecimal.ZERO);

        setCreditAccount(investmentAccount);
        setDebitAccount(incomeAccount);

        /* Transaction can be treated as single entry, but double entry is being forced by UI
         * Single entry support is required for jGnash 1.x in. */
        if (investmentAccount.equals(incomeAccount)) { // transaction against the cash balance
            setCreditAmount(dividend); // treat as a single entry transaction
            setDebitAmount(dividend);
        } else { // double entry dividend
            setCreditAmount(dividend); // account balance of investment account not impacted
            setDebitAmount(incomeExchangedAmount);
        }
    }

    @Override
    public BigDecimal getTotal() {
        return getCreditAmount();
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
        return BigDecimal.ZERO;
    }

    @Override
    @NotNull
    public TransactionType getTransactionType() {
        return TransactionType.DIVIDEND;
    }
}
