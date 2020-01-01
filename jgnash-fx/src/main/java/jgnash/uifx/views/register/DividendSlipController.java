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
package jgnash.uifx.views.register;

import java.math.BigDecimal;

import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntryDividendX;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionType;
import jgnash.util.NotNull;

/**
 * Dividend entry controller.
 *
 * @author Craig Cavanaugh
 */
public class DividendSlipController extends AbstractInvIncomeSlipController {

    @NotNull
    @Override
    public Transaction buildTransaction() {
        BigDecimal incomeExchangedAmount = decimalTextField.getDecimal().negate();

        BigDecimal accountExchangedAmount = decimalTextField.getDecimal();

        if (!incomeExchangePane.getSelectedAccount().getCurrencyNode().equals(accountProperty().get().getCurrencyNode())) {
            incomeExchangedAmount = incomeExchangePane.exchangeAmountProperty().get().negate();
        }

        if (!accountExchangePane.getSelectedAccount().getCurrencyNode().equals(accountProperty().get().getCurrencyNode())) {
            accountExchangedAmount = accountExchangePane.exchangeAmountProperty().get();
        }

        final Transaction transaction = TransactionFactory.generateDividendXTransaction(incomeExchangePane.getSelectedAccount(),
                accountProperty().get(), accountExchangePane.getSelectedAccount(), securityComboBox.getValue(),
                decimalTextField.getDecimal(), incomeExchangedAmount, accountExchangedAmount, datePicker.getValue(),
                memoTextField.getText());

        transaction.setNumber(numberComboBox.getValue());

        transaction.setTags(TransactionEntryDividendX.class, tagPane.getSelectedTags());

        return transaction;
    }

    @Override
    public void modifyTransaction(@NotNull Transaction transaction) {
        super.modifyTransaction(transaction);

        tagPane.setSelectedTags(transaction.getTags(TransactionEntryDividendX.class));
    }

    @Override
    TransactionType getTransactionType() {
        return TransactionType.DIVIDEND;
    }
}
