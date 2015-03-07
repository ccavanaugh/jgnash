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
package jgnash.uifx.views.register;

import jgnash.engine.ReconciledState;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionTag;

/**
 * Split Transaction Entry Controller for investment fees
 *
 * @author Craig Cavanaugh
 */
public class FeeTransactionEntrySlipController extends AbstractTransactionEntrySlipController {

    @Override
    TransactionEntry buildTransactionEntry() {
        final TransactionEntry entry = new TransactionEntry();
        entry.setMemo(memoField.getText());

        entry.setDebitAccount(accountProperty.get());
        entry.setCreditAccount(accountExchangePane.getSelectedAccount());

        entry.setDebitAmount(amountField.getDecimal().abs().negate());

        if (hasEqualCurrencies()) {
            entry.setCreditAmount(amountField.getDecimal().abs());
        } else {
            entry.setCreditAmount(accountExchangePane.exchangeAmountProperty().get().abs());
        }

        entry.setTransactionTag(TransactionTag.INVESTMENT_FEE);

        entry.setReconciled(accountProperty.get(), reconciledButton.isSelected() ? ReconciledState.CLEARED : ReconciledState.NOT_RECONCILED);

        return entry;
    }

    void modifyTransactionEntry(final TransactionEntry entry) {
        oldEntry = entry;

        memoField.setText(entry.getMemo());

        accountExchangePane.setSelectedAccount(entry.getCreditAccount());
        amountField.setDecimal(entry.getDebitAmount().abs());

        accountExchangePane.setExchangedAmount(entry.getCreditAmount());

        reconciledButton.setSelected(entry.getReconciled(accountProperty.get()) != ReconciledState.NOT_RECONCILED);
    }
}
