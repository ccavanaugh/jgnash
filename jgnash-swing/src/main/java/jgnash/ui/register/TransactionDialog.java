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

import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.ResourceBundle;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.ReconcileManager;
import jgnash.engine.Transaction;
import jgnash.ui.UIApplication;
import jgnash.ui.util.DialogUtils;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A Dialog for creating and editing new transactions
 *
 * @author Craig Cavanaugh
 */
public class TransactionDialog extends JDialog implements RegisterListener {

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private final Account account;

    private final JTabbedPane tabbedPane;

    private TransactionPanelEx debitPanel;

    private TransactionPanelEx creditPanel;

    private Transaction transaction = null;

    public static Transaction showDialog(final Account account, final Transaction t) {
        TransactionDialog d = new TransactionDialog(account);

        if (t != null) {
            d.setTransaction(t);
        }

        d.setVisible(true);
        return d.getTransaction();
    }

    private TransactionDialog(final Account account) {
        super(UIApplication.getFrame(), true);
        setTitle(rb.getString("Title.NewTrans"));

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        this.account = account;
        tabbedPane = new JTabbedPane();

        buildTabbedPane();

        layoutMainPanel();

        DialogUtils.addBoundsListener(this);
    }

    /**
     * Closes the dialog
     */
    private void closeDialog() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private Transaction getTransaction() {
        return transaction;
    }

    private void setTransaction(final Transaction tran) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        if (!engine.isStored(tran)) { // must not be a persisted transaction
            if (tran.getAmount(account).signum() >= 0) {
                tabbedPane.setSelectedComponent(creditPanel);
                creditPanel.modifyTransaction(tran);
            } else {
                tabbedPane.setSelectedComponent(debitPanel);
                debitPanel.modifyTransaction(tran);
            }
        }
    }

    private void buildTabbedPane() {
        if (tabbedPane.getComponentCount() > 0) {
            if (debitPanel != null && creditPanel != null) {
                debitPanel.removeRegisterListener(this);
                creditPanel.removeRegisterListener(this);
            }
            tabbedPane.removeAll();
        }

        debitPanel = new TransactionPanelEx(account, PanelType.DECREASE);
        creditPanel = new TransactionPanelEx(account, PanelType.INCREASE);

        debitPanel.addRegisterListener(this);
        creditPanel.addRegisterListener(this);

        String[] tabNames = RegisterFactory.getCreditDebitTabNames(account);

        tabbedPane.add(tabNames[0], creditPanel);
        tabbedPane.add(tabNames[1], debitPanel);
    }

    private void layoutMainPanel() {
        final FormLayout layout = new FormLayout("right:d, 4dlu, f:d:g", "f:d, 3dlu, f:d, 8dlu, f:d");
        final CellConstraints cc = new CellConstraints();

        final JPanel p = new JPanel(layout);

        p.add(new JLabel(rb.getString("Label.BaseAccount")), cc.xy(1, 1));
        p.add(new JLabel(account.getPathName()), cc.xy(3, 1));
        p.add(tabbedPane, cc.xyw(1, 3, 3));

        p.setBorder(Borders.DIALOG);

        getContentPane().setLayout(new java.awt.BorderLayout());
        getContentPane().add(p, java.awt.BorderLayout.CENTER);
        pack();

        setMinimumSize(getSize()); // set minimum bounds
    }

    @Override
    public void registerEvent(final RegisterEvent e) {
        if (e.getAction() == RegisterEvent.Action.CANCEL) {
            transaction = null;
            closeDialog();
        } else if (e.getAction() == RegisterEvent.Action.OK) {
            transaction = ((TransactionPanelEx) tabbedPane.getSelectedComponent()).getTransaction();
            closeDialog();
        }
    }

    private static class TransactionPanelEx extends TransactionPanel {

        private Transaction t = null;

        TransactionPanelEx(final Account account, final PanelType panelType) {
            super(account, panelType);
        }

        Transaction getTransaction() {
            return t;
        }

        /**
         * Override enterAction so that the transaction is not entered into the engine
         *
         * @see jgnash.ui.register.AbstractBankTransactionPanel#enterAction()
         */
        @Override
        public void enterAction() {
            if (validateForm()) {
                t = buildTransaction();

                ReconcileManager.reconcileTransaction(getAccount(), t, getReconciledState());

                fireOkAction();
            } else {
                t = null;
            }
        }
    }
}
