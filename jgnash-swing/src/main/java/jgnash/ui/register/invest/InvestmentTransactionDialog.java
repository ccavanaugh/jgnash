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
package jgnash.ui.register.invest;

import java.awt.event.WindowEvent;
import java.util.ResourceBundle;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import jgnash.engine.Account;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.ui.UIApplication;
import jgnash.ui.register.RegisterEvent;
import jgnash.ui.register.RegisterListener;
import jgnash.ui.util.DialogUtils;
import jgnash.resource.util.ResourceUtils;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A Dialog for creating and editing new transactions
 *
 * @author Craig Cavanaugh
 */
public class InvestmentTransactionDialog extends JDialog implements RegisterListener {

    private final Account account;

    private final InvestmentTransactionPanel transactionPanel;

    public static void showDialog(InvestmentTransaction t) {
        ResourceBundle rb = ResourceUtils.getBundle();

        InvestmentTransactionDialog d = new InvestmentTransactionDialog(t.getInvestmentAccount());

        d.setTitle(rb.getString("Title.ModifyTransaction"));
        d.setTransaction(t);
        d.setVisible(true);
    }

    private InvestmentTransactionDialog(Account account) {
        super(UIApplication.getFrame(), true);

        this.account = account;

        transactionPanel = new InvestmentTransactionPanel(this.account);
        transactionPanel.addRegisterListener(this);

        layoutMainPanel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        setMinimumSize(getSize());

        DialogUtils.addBoundsListener(this);
    }

    private void setTransaction(Transaction t) {
        transactionPanel.modifyTransaction(t);
    }

    private void layoutMainPanel() {
        ResourceBundle rb = ResourceUtils.getBundle();

        FormLayout layout = new FormLayout("right:d, 4dlu, f:d:g", "f:d, 3dlu, f:d, 8dlu, f:d");
        CellConstraints cc = new CellConstraints();

        JPanel p = new JPanel(layout);

        p.add(new JLabel(rb.getString("Label.BaseAccount")), cc.xy(1, 1));
        p.add(new JLabel(account.getPathName()), cc.xy(3, 1));
        p.add(transactionPanel, cc.xyw(1, 3, 3));

        p.setBorder(Borders.DIALOG);

        getContentPane().setLayout(new java.awt.BorderLayout());
        getContentPane().add(p, java.awt.BorderLayout.CENTER);
        pack();
    }

    @Override
    public void registerEvent(RegisterEvent e) {
        if (e.getAction() == RegisterEvent.Action.CANCEL) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } else if (e.getAction() == RegisterEvent.Action.OK) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }
}