/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import jgnash.engine.Account;
import jgnash.engine.ReconcileManager;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionFactory;
import jgnash.ui.util.ValidationFactory;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Handles the transfer of money between accounts.
 * 
 * @author Craig Cavanaugh
 * @author Don Brown
 * @author axnotizes
 */
public class TransferPanel extends AbstractExchangeTransactionPanel {

    TransferPanel(Account account) {
        super(account, PanelType.DECREASE);

        layoutMainPanel();
        clearForm();
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("right:d, $lcgap, 50dlu:g, 8dlu, right:d, $lcgap, max(48dlu;min)",
                "f:d, $nlgap, f:d, $nlgap, f:d, $nlgap, f:d");

        layout.setRowGroups(new int[][] { { 1, 3, 5, 7 } });
        CellConstraints cc = new CellConstraints();

        setLayout(layout);
        setBorder(Borders.DIALOG);

        add("Label.TransferTo", cc.xy(1, 1));
        add(accountPanel, cc.xy(3, 1));
        add("Label.Date", cc.xy(5, 1));
        add(datePanel, cc.xy(7, 1));

        add("Label.Memo", cc.xy(1, 3));
        add(memoField, cc.xy(3, 3));
        add("Label.Amount", cc.xy(5, 3));
        add(ValidationFactory.wrap(amountField), cc.xy(7, 3));

        add(createBottomPanel(), cc.xyw(1, 7, 7));
    }

    /**
     * TODO: The memo payee and number should be user configurable by the user
     */
    @Override
    protected Transaction buildTransaction() {
        String number = "TRAN";

        String payee = rb.getString("Tab.Transfer");

        Transaction transaction;

        final int signum = amountField.getDecimal().signum();

        if (panelType == PanelType.DECREASE && signum >= 0 || signum == -1) {
            if (hasEqualCurrencies()) {
                transaction = TransactionFactory.generateDoubleEntryTransaction(accountPanel.getSelectedAccount(),
                        getAccount(), amountField.getDecimal().abs(), datePanel.getLocalDate(), memoField.getText(),
                        payee, number);
            } else {
                transaction = TransactionFactory.generateDoubleEntryTransaction(accountPanel.getSelectedAccount(),
                        getAccount(), accountPanel.getExchangedAmount().abs(), amountField.getDecimal().abs().negate(),
                        datePanel.getLocalDate(), memoField.getText(), payee, number);
            }
        } else {
            if (hasEqualCurrencies()) {
                transaction = TransactionFactory.generateDoubleEntryTransaction(getAccount(),
                        accountPanel.getSelectedAccount(), amountField.getDecimal().abs(), datePanel.getLocalDate(),
                        memoField.getText(), payee, number);
            } else {
                transaction = TransactionFactory.generateDoubleEntryTransaction(getAccount(),
                        accountPanel.getSelectedAccount(), amountField.getDecimal().abs(),
                        accountPanel.getExchangedAmount().abs().negate(), datePanel.getLocalDate(), memoField.getText(),
                        payee, number);
            }
        }

        ReconcileManager.reconcileTransaction(getAccount(), transaction, getReconciledState());

        return transaction;
    }

    /**
     * Modifies a transaction inside this form.<br>
     * The t must be assigned to {@code modTrans} if transaction modification is allowed
     * 
     * @param t The transaction to modify
     */
    @Override
    public void modifyTransaction(Transaction t) {
        // does nothing for this form
    }

    /**
     * Always return false
     * 
     * @see jgnash.ui.register.AbstractBankTransactionPanel#canModifyTransaction(jgnash.engine.Transaction)
     */
    @Override
    protected boolean canModifyTransaction(Transaction t) {
        return false;
    }
}
