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

import java.awt.event.WindowEvent;
import java.util.ResourceBundle;

import javax.swing.JDialog;
import javax.swing.WindowConstants;

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.ui.UIApplication;
import jgnash.ui.util.DialogUtils;
import jgnash.util.ResourceUtils;

/**
 * A Dialog for editing a newly created transaction.
 *
 * @author Craig Cavanaugh
 *
 */
public class EditTransactionDialog extends JDialog implements RegisterListener {

    private final TransactionPanel transPanel;

    EditTransactionDialog(final Account a, final PanelType transType) {
        super(UIApplication.getFrame(), true);
        ResourceBundle rb = ResourceUtils.getBundle();
        setTitle(rb.getString("Title.NewTrans") + " " + a.getPathName());

        transPanel = new TransactionPanel(a, transType);
        transPanel.addRegisterListener(this);

        layoutMainPanel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent evt) {
                transPanel.removeRegisterListener(EditTransactionDialog.this);
            }
        });

        DialogUtils.addBoundsListener(this);
    }   

    private void layoutMainPanel() {
        getContentPane().setLayout(new java.awt.BorderLayout());
        getContentPane().add(transPanel, java.awt.BorderLayout.CENTER);
        pack();
    }

    /**
     * Loads up the new transaction and disables auto-completion by default.
     * We do not want the newly created transaction to be cleared by an old one.
     *
     * @param t New transaction to fill the form with
     */
    void newTransaction(final Transaction t) {
        transPanel.newTransaction(t);
        transPanel.setAutoComplete(false); // disable
    }

    @Override
    public void registerEvent(final RegisterEvent e) {
        if (e.getAction() == RegisterEvent.Action.CANCEL) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } else if (e.getAction() == RegisterEvent.Action.OK) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }
}
