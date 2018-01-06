/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import jgnash.engine.Account;
import jgnash.engine.AmortizeObject;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.account.AmortizeDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Register panel for liability accounts.
 *
 * @author Craig Cavanaugh
 */
public class LiabilityRegisterPanel extends RegisterPanel {

    private JButton amortizeButton;

    private JButton paymentButton;

    private final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

    /**
     * Creates a new instance of LiabilityRegisterPanel
     *
     * @param account account to create panel for
     */
    LiabilityRegisterPanel(final Account account) {
        super(account);
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        paymentButton = new JButton(rb.getString("Button.NewPayment"));
        amortizeButton = new JButton(rb.getString("Button.Amortize"));

        amortizeButton.addActionListener(this);
        paymentButton.addActionListener(this);
    }

    /**
     * Overrides createButtonPanel in GenericRegisterPanel to add extra buttons
     */
    @Override
    protected JPanel createButtonPanel() {
        FormLayout layout = new FormLayout("d, 4dlu:g, d", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(StaticUIMethods.buildLeftAlignedBar(newButton, duplicateButton, jumpButton, deleteButton));
        builder.append(StaticUIMethods.buildRightAlignedBar(paymentButton, amortizeButton));
        return builder.getPanel();
    }

    /**
     * Displays the Amortize dialog
     */
    private void amortizeAction() {
        AmortizeObject ao = account.getAmortizeObject();

        AmortizeDialog d = new AmortizeDialog(ao);
        d.setVisible(true);

        if (d.getResult()) {
            if (!engine.setAmortizeObject(account, d.getAmortizeObject())) {
                StaticUIMethods.displayError(rb.getString("Message.Error.AmortizationSave"));
            }
        }
    }

    /* creates the payment transaction relative to the debit account */
    private void paymentActionDebit() {
        AmortizeObject ao = account.getAmortizeObject();

        if (ao != null) {

            DateChkNumberDialog d = new DateChkNumberDialog(ao.getBankAccount(), rb.getString("Title.NewTrans"));
            d.setVisible(true);

            if (!d.getResult()) {
                return;
            }

            final Transaction tran = ao.generateTransaction(account, d.getDate(), d.getNumber());

            if (tran != null) {// display the transaction in the register
                EditTransactionDialog dlg = new EditTransactionDialog(ao.getBankAccount(), PanelType.DECREASE);
                dlg.newTransaction(tran);
                dlg.setVisible(true);
            } else {
                StaticUIMethods.displayWarning(rb.getString("Message.Warn.ConfigAmortization"));
            }
        } else { // could not generate the transaction
            StaticUIMethods.displayWarning(rb.getString("Message.Warn.ConfigAmortization"));
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        super.actionPerformed(e);

        if (e.getSource() == amortizeButton) {
            amortizeAction();
        } else if (e.getSource() == paymentButton) {
            paymentActionDebit();
        }
    }
}
