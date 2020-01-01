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
 * Sell shares and increase the (cash) balance of an account.
 * <p>
 * The investment account is always assigned to the credit account.
 * <p>
 * If an account other than the investment account is assigned to the debit
 * account, then the balance of the debit account is increased because the
 * gains of the sell are added to it.
 *
 * @author Craig Cavanaugh
 */
@Entity
public class TransactionEntrySellX extends AbstractInvestmentTransactionEntry {

    /**
     * No argument constructor for reflection purposes.
     * <b>Do not use to create a new instance</b>
     */
    @SuppressWarnings("unused")
    public TransactionEntrySellX() {
    }

    /**
     * Constructor.
     *
     * @param account           Debit account
     * @param investmentAccount Credit / Investment  account
     * @param securityNode      Security for the transaction
     * @param price             Price of shares
     * @param quantity          Number of shares
     * @param exchangeRate      Exchange rate for the debit account (May be ONE, but may not be null and must be greater than ZERO)
     */
    TransactionEntrySellX(final Account account, final Account investmentAccount, final SecurityNode securityNode,
                          final BigDecimal price, final BigDecimal quantity, final BigDecimal exchangeRate) {

        assert investmentAccount.memberOf(AccountGroup.INVEST);
        assert exchangeRate != null && exchangeRate.signum() == 1;

        setSecurityNode(securityNode);
        setPrice(price);
        setQuantity(quantity);

        setCreditAccount(investmentAccount);
        setDebitAccount(account);

        if (investmentAccount.equals(account)) { // transaction against the cash balance

            BigDecimal amount = price.multiply(quantity).setScale(investmentAccount.getCurrencyNode().getScale(), MathConstants.roundingMode);

            setCreditAmount(amount);
            setDebitAmount(amount);
        } else { // transaction against a different account

            setCreditAmount(BigDecimal.ZERO);

            byte scale = getCreditAccount().getCurrencyNode().getScale();

            if (account.getCurrencyNode().equals(investmentAccount.getCurrencyNode())) {
                setDebitAmount(price.multiply(quantity).setScale(scale, MathConstants.roundingMode));
            } else { // currency exchange
                setDebitAmount(price.multiply(quantity).multiply(exchangeRate).setScale(scale, MathConstants.roundingMode));
            }
        }
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
    @NotNull
    public TransactionType getTransactionType() {
        return TransactionType.SELLSHARE;
    }
}
