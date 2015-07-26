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
package jgnash.ui.register;

import javax.swing.JPanel;

import jgnash.engine.Account;
import jgnash.ui.StaticUIMethods;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Extends AbstractBankTransactionPanel and adds fields and labels for transactions
 * across accounts with different currencies.
 * <p/>
 * When transactions with multiple currencies are modified, the transaction takes
 * on the base currency of the account register it is being modified from.  The
 * original base currency is only preserved if the transaction is modified in the
 * same account register it was created in.
 *
 * @author Craig Cavanaugh
 * @author Pranay
 *
 */
public abstract class AbstractExchangeTransactionPanel extends AbstractBankTransactionPanel {

    final PanelType panelType;

    final AccountExchangePanel accountPanel;

    AbstractExchangeTransactionPanel(Account account, PanelType panelType) {
        super(account);
        this.panelType = panelType;

        accountPanel = new AccountExchangePanel(account.getCurrencyNode(), account, amountField);
    }

    boolean hasEqualCurrencies() {
        return getAccount().getCurrencyNode().equals(accountPanel.getSelectedAccount().getCurrencyNode());
    }

    @Override
    public boolean validateForm() {
        return accountPanel.getSelectedAccount() != null && super.validateForm();
    }

    @Override
    public void clearForm() {
        accountPanel.setExchangedAmount(null);

        super.clearForm();
    }

    protected JPanel createBottomPanel() {

        FormLayout layout = new FormLayout("m, $ugap, m, $ugap, right:m:g", "f:d");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(getReconcileCheckBox(), attachmentPanel, StaticUIMethods.buildOKCancelBar(enterButton, cancelButton));
        return builder.getPanel();
    }
}
