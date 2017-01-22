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
package jgnash.ui.register.invest;

import java.time.LocalDate;
import java.util.logging.Logger;

import javax.swing.JTextField;

import jgnash.engine.Account;
import jgnash.engine.MathConstants;
import jgnash.engine.ReconciledState;
import jgnash.ui.UIApplication;
import jgnash.ui.components.AccountSecurityComboBox;
import jgnash.ui.components.AutoCompleteFactory;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.JFloatField;

/**
 * Abstract class for investment transaction entry.  This is to be used for any entry
 * that requires date, price, quantity, and security fields
 *
 * @author Craig Cavanaugh
 */
public abstract class AbstractPriceQtyInvTransactionPanel extends AbstractInvTransactionPanel {

    final DatePanel datePanel;

    final JTextField memoField;

    final JFloatField priceField;

    final JFloatField quantityField;

    final AccountSecurityComboBox securityCombo;

    final JFloatField totalField;

    private static final Logger logger = UIApplication.getLogger();

    AbstractPriceQtyInvTransactionPanel(final Account account) {
        super(account);

        datePanel = new DatePanel();
        memoField = AutoCompleteFactory.getMemoField();
        priceField = new JFloatField(0, MathConstants.SECURITY_PRICE_ACCURACY, account.getCurrencyNode().getScale());
        quantityField = new JFloatField(0, MathConstants.SECURITY_QUANTITY_ACCURACY, 2);
        securityCombo = new AccountSecurityComboBox(account);

        totalField = new JFloatField(account.getCurrencyNode());
        totalField.setEditable(false);
        totalField.setFocusable(false);
    }

    /**
     * @see jgnash.ui.register.AbstractEntryFormPanel#clearForm()
     */
    @Override
    public void clearForm() {
        modTrans = null;

        if (!getRememberLastDate()) {
            datePanel.setDate(LocalDate.now());
        }

        memoField.setText(null);
        priceField.setDecimal(null);
        quantityField.setDecimal(null);
        setReconciledState(ReconciledState.NOT_RECONCILED);
        totalField.setDecimal(null);
    }

    /**
     * Performs from validation
     *
     * @return {@code true} if all form requirements are satisfied
     * @see jgnash.ui.register.AbstractEntryFormPanel#validateForm()
     */
    @Override
    public boolean validateForm() {
        if (securityCombo.getSelectedNode() == null) {
            logger.warning(rb.getString("Message.Error.SecuritySelection"));
            showValidationError(rb.getString("Message.Error.SecuritySelection"), securityCombo);
            return false;
        }

        if (priceField.isEmpty()) {
            logger.warning(rb.getString("Message.Error.SecurityPrice"));
            showValidationError(rb.getString("Message.Error.SecurityPrice"), priceField);
            return false;
        }

        if (quantityField.isEmpty()) {
            logger.warning(rb.getString("Message.Error.SecurityQuantity"));
            showValidationError(rb.getString("Message.Error.SecurityQuantity"), quantityField);
            return false;
        }

        return true;
    }
}
