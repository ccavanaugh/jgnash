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

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import jgnash.engine.TransactionEntry;

/**
 * Split Transaction Entry Controller for Credits and Debits.
 *
 * @author Craig Cavanaugh
 */
public class SplitTransactionSlipController extends AbstractTransactionEntrySlipController {

    @FXML
    private Button enterButton;

    private SlipType slipType;

    void setSlipType(final SlipType slipType) {
        this.slipType = slipType;
    }

    @FXML
    protected void initialize() {
        super.initialize();

        enterButton.disableProperty().bind(validFormProperty().not());
    }

    @Override
    TransactionEntry buildTransactionEntry() {
        TransactionEntry entry = new TransactionEntry();
        entry.setMemo(memoField.getText());

        int signum = amountField.getDecimal().signum();

        if ((slipType == SlipType.DECREASE && signum >= 0) || (slipType == SlipType.INCREASE && signum < 0)) {
            entry.setCreditAccount(accountExchangePane.getSelectedAccount());
            entry.setDebitAccount(account.get());

            if (hasEqualCurrencies()) {
                entry.setAmount(amountField.getDecimal().abs());
            } else {
                entry.setDebitAmount(amountField.getDecimal().abs().negate());
                entry.setCreditAmount(accountExchangePane.exchangeAmountProperty().get().abs());
            }
        } else {
            entry.setCreditAccount(account.get());
            entry.setDebitAccount(accountExchangePane.getSelectedAccount());

            if (hasEqualCurrencies()) {
                entry.setAmount(amountField.getDecimal().abs());
            } else {
                entry.setCreditAmount(amountField.getDecimal().abs());
                entry.setDebitAmount(accountExchangePane.exchangeAmountProperty().get().abs().negate());
            }
        }

        entry.setReconciled(account.get(), getReconciledState());
        entry.setTags(tagPane.getSelectedTags());

        return entry;
    }

    void modifyTransactionEntry(final TransactionEntry entry) {
        oldEntry = entry;

        memoField.setText(entry.getMemo());

        if (slipType == SlipType.DECREASE) {
            accountExchangePane.setSelectedAccount(entry.getCreditAccount());
            amountField.setDecimal(entry.getDebitAmount().abs());

            accountExchangePane.setExchangedAmount(entry.getCreditAmount());
        } else {
            accountExchangePane.setSelectedAccount(entry.getDebitAccount());
            amountField.setDecimal(entry.getCreditAmount());

            accountExchangePane.setExchangedAmount(entry.getDebitAmount().abs());
        }

        setReconciledState(entry.getReconciled(account.get()));

        tagPane.setSelectedTags(entry.getTags());
    }
}
