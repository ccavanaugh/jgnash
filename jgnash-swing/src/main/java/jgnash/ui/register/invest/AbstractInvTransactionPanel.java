/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
package jgnash.ui.register.invest;

import java.awt.Insets;
import java.util.Objects;

import javax.swing.SwingConstants;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.ReconcileManager;
import jgnash.engine.Transaction;
import jgnash.ui.components.IndeterminateCheckBox;
import jgnash.ui.register.AbstractTransactionPanel;

/**
 * Abstract panel for investment transaction form.
 * 
 * @author Craig Cavanaugh
 */
abstract class AbstractInvTransactionPanel extends AbstractTransactionPanel {

    private IndeterminateCheckBox reconciledButton;

    Transaction modTrans = null;

    Account account;

    AbstractInvTransactionPanel(final Account account) {

        if (!account.memberOf(AccountGroup.INVEST)) {
            throw new RuntimeException(rb.getString("Message.Error.InvalidAccountGroup"));
        }

        this.account = account;

        reconciledButton = new IndeterminateCheckBox(rb.getString("Button.Cleared"));
        reconciledButton.setHorizontalTextPosition(SwingConstants.LEADING);
        reconciledButton.setMargin(new Insets(0, 0, 0, 0));
    }

    @Override
    protected IndeterminateCheckBox getReconcileCheckBox() {
        return reconciledButton;
    }

    Account getAccount() {
        return account;
    }

    /**
     * Method that is called when the enter button is used
     */
    @Override
    public void enterAction() {
        if (validateForm()) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

            Objects.requireNonNull(engine);

            if (modTrans == null) {
                final Transaction newTrans = buildTransaction();

                // Need to set the reconciled state
                ReconcileManager.reconcileTransaction(getAccount(), newTrans, getReconciledState());

                engine.addTransaction(newTrans);
            } else {
                final Transaction newTrans = buildTransaction();

                /* Need to preserve the reconciled state of the opposite side
                 * if both sides are not automatically reconciled
                 */
                ReconcileManager.reconcileTransaction(getAccount(), newTrans, getReconciledState());

                if (engine.isTransactionValid(newTrans)) {
                    if (engine.removeTransaction(modTrans)) {
                        engine.addTransaction(newTrans);
                    }
                }
            }
            clearForm();
            focusFirstComponent();
        }
    }
}
