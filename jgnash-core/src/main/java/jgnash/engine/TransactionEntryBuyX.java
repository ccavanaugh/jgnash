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
 * Buy shares and reduce the (cash) balance of an account
 * <p/>
 * The investment account is always assigned to the debit account.
 * <p/>
 * If an account other than the investment account is assigned to the credit
 * account, then the balance of the credit account is reduced because the cost
 * of the buy is made against it.
 *
 * @author Craig Cavanaugh
 * @version $Id: TransactionEntryBuyX.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class TransactionEntryBuyX extends AbstractInvestmentTransactionEntry {

    /**
     * No argument constructor for reflection purposes.
     * <b>Do not use to create a new instance</b>
     *
     * @deprecated
     */
    @Deprecated
    public TransactionEntryBuyX() {
    }

    /**
     * Constructor
     *
     * @param account           Credit account
     * @param investmentAccount Debit / Investment  account
     * @param securityNode      Security for the transaction
     * @param price             Price of shares
     * @param quantity          Number of shares
     * @param exchangeRate      Exchange rate for the credit account (May be ONE, but may not be null and must be greater than ZERO)
     */
    protected TransactionEntryBuyX(Account account, Account investmentAccount, SecurityNode securityNode, BigDecimal price, BigDecimal quantity, BigDecimal exchangeRate) {

        assert investmentAccount.memberOf(AccountGroup.INVEST);
        assert exchangeRate != null && exchangeRate.signum() == 1;

        setSecurityNode(securityNode);
        setPrice(price);
        setQuantity(quantity);

        setDebitAccount(investmentAccount);
        setCreditAccount(account);

        if (investmentAccount.equals(account)) { // transaction against the cash balance

            BigDecimal amount = price.multiply(quantity).setScale(investmentAccount.getCurrencyNode().getScale(), MathConstants.roundingMode).negate();

            setCreditAmount(amount);
            setDebitAmount(amount);
        } else { // transaction against a different account

            setDebitAmount(BigDecimal.ZERO);

            byte scale = getCreditAccount().getCurrencyNode().getScale();

            if (account.getCurrencyNode().equals(investmentAccount.getCurrencyNode())) {
                setCreditAmount(price.multiply(quantity.negate()).setScale(scale, MathConstants.roundingMode));
            } else { // currency exchange
                setCreditAmount(price.multiply(quantity.negate()).multiply(exchangeRate).setScale(scale, MathConstants.roundingMode));
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
        return getQuantity();
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.BUYSHARE;
    }
}
