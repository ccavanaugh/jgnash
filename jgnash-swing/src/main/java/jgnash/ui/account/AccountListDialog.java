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
package jgnash.ui.account;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

import jgnash.engine.Account;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * Displays a tree view of the accounts
 *
 * @author Craig Cavanaugh
 */
public class AccountListDialog extends JDialog implements ActionListener {
    private final transient Resource rb = Resource.get();

    private JButton okButton;

    private JButton cancelButton;

    private final AccountListTreePane list;

    private Account account;

    private boolean returnStatus = false;

    public AccountListDialog() {
        this(null, false);
    }

    AccountListDialog(final Account a) {
        this(a, false);
    }

    AccountListDialog(final Account a, final boolean disablePlaceHolders) {
        super(UIApplication.getFrame(), true);
        setTitle(rb.getString("Title.SelAccount"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        list = new AccountListTreePane("list", true);
        
        // force visibility
        list.setExpenseVisible(true);
        list.setIncomeVisible(true);
        list.setAccountVisible(true);

        if (disablePlaceHolders) {
            list.disablePlaceHolders();
        }

        if (a != null) {
            list.setSelectedAccount(a);
        }

        layoutMainPanel();       
    }

    public void disableAccount(final Account a) {
        list.disableAccount(a);
    }

    public void disablePlaceHolders() {
        list.disablePlaceHolders();
    }

    private void initComponents() {
        cancelButton = new JButton(rb.getString("Button.Cancel"));
        okButton = new JButton(rb.getString("Button.Ok"));

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("max(200dlu;p):g", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(Borders.DIALOG);

        builder.appendRow("f:100dlu:g");
        builder.append(list);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(StaticUIMethods.buildOKCancelBar(okButton, cancelButton));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        pack();
        setMinimumSize(getSize());

        DialogUtils.addBoundsListener(this);
    }

    public boolean getReturnStatus() {
        return returnStatus;
    }

    public Account getAccount() {
        return account;
    }

    private void cancelAction() {
        returnStatus = false;
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void okAction() {
        account = list.getSelectedAccount();
        returnStatus = true;
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == okButton) {
            okAction();
        } else if (e.getSource() == cancelButton) {
            cancelAction();
        }
    }
}
