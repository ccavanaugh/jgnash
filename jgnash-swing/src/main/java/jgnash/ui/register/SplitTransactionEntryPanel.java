/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
package jgnash.ui.register;

import jgnash.engine.TransactionEntry;
import jgnash.ui.register.table.SplitsRegisterTableModel;

/**
 * Entry panel for spit transactions
 *
 * @author Craig Cavanaugh
 */
public class SplitTransactionEntryPanel extends AbstractTransactionEntryPanel {

    private final PanelType panelType;

    public SplitTransactionEntryPanel(SplitsRegisterTableModel model, PanelType type) {
        super(model);

        panelType = type;
    }

    @Override
    protected TransactionEntry buildTransactionEntry() {
        TransactionEntry entry = new TransactionEntry();
        entry.setMemo(memoField.getText());

        int signum = amountField.getDecimal().signum();

        if ((panelType == PanelType.DECREASE && signum >= 0) || (panelType == PanelType.INCREASE && signum < 0)) {
            entry.setCreditAccount(accountPanel.getSelectedAccount());
            entry.setDebitAccount(getAccount());

            if (hasEqualCurrencies()) {
                entry.setAmount(amountField.getDecimal().abs());
            } else {
                entry.setDebitAmount(amountField.getDecimal().abs().negate());
                entry.setCreditAmount(accountPanel.getExchangedAmount().abs());
            }
        } else {
            entry.setCreditAccount(getAccount());
            entry.setDebitAccount(accountPanel.getSelectedAccount());

            if (hasEqualCurrencies()) {
                entry.setAmount(amountField.getDecimal().abs());
            } else {
                entry.setCreditAmount(amountField.getDecimal().abs());
                entry.setDebitAmount(accountPanel.getExchangedAmount().abs().negate());
            }
        }

        entry.setReconciled(account, getReconciledState());

        return entry;
    }

    public void modifyTransaction(final TransactionEntry entry) {
        oldEntry = entry;

        memoField.setText(entry.getMemo());

        if (panelType == PanelType.DECREASE) {
            accountPanel.setSelectedAccount(entry.getCreditAccount());
            amountField.setDecimal(entry.getDebitAmount().abs());

            accountPanel.setExchangedAmount(entry.getCreditAmount());
        } else {
            accountPanel.setSelectedAccount(entry.getDebitAccount());
            amountField.setDecimal(entry.getCreditAmount());

            accountPanel.setExchangedAmount(entry.getDebitAmount().abs());
        }

        setReconciledState(entry.getReconciled(getAccount()));
    }
}
