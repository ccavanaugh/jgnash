/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
package jgnash.ui.account;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import jgnash.engine.Account;
import jgnash.engine.EngineFactory;
import jgnash.engine.SecurityNode;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * Displays a list for available and current securities
 *
 * @author Craig Cavanaugh
 *
 */
public class AccountSecuritiesDialog extends JDialog implements ActionListener {

    private final transient Resource rb = Resource.get();

    private boolean retValue = false;

    private AccountSecuritiesPanel panel;

    private JButton okButton;

    private JButton cancelButton;

    private final Account account;

    AccountSecuritiesDialog(final Account account, final Set<SecurityNode> list, final Component parent) {
        super(SwingUtilities.getWindowAncestor(parent), Dialog.ModalityType.APPLICATION_MODAL);

        setTitle(rb.getString("Title.AccountSecurities"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.account = account;

        layoutMainPanel();

        setSecuritiesList(list);
        pack();

        setMinimumSize(getSize());
        DialogUtils.addBoundsListener(this);
    }

    public static void showDialog(final Account account, final Component parent) {
        AccountSecuritiesDialog dlg = new AccountSecuritiesDialog(account, account.getSecurities(), parent);

        dlg.setVisible(true);

        if (dlg.getReturnValue()) {
            EngineFactory.getEngine(EngineFactory.DEFAULT).updateAccountSecurities(account, dlg.getSecuritiesList());
        }
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p:g", "f:p:g(1.0)");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.append(panel);
        builder.nextLine();

        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(ButtonBarFactory.buildOKCancelBar(okButton, cancelButton));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initComponents() {
        okButton = new JButton(rb.getString("Button.Ok"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));

        panel = new AccountSecuritiesPanel(account);

        cancelButton.addActionListener(this);
        okButton.addActionListener(this);
    }

    private void setSecuritiesList(Set<SecurityNode> list) {
        panel.setSecuritiesList(list);
    }

    public Set<SecurityNode> getSecuritiesList() {
        return panel.getSecuritiesList();
    }

    public boolean getReturnValue() {
        return retValue;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == okButton) {
            retValue = true;
        } else if (e.getSource() == cancelButton) {
            retValue = false;
        }
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }
}
