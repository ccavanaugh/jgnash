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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.SecurityNode;
import jgnash.ui.UIApplication;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * Account dialog
 *
 * @author Craig Cavanaugh
 *
 */
class AccountDialog extends JDialog implements ActionListener {
    private Resource rb = Resource.get();

    private boolean returnStatus = false;

    private AccountPanel accountPanel = null;

    private JButton cancelButton;

    private JButton okButton;

    private JButton helpButton;

    public AccountDialog() {
        super(UIApplication.getFrame(), true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        layoutMainPanel();

        setMinimumSize(getSize());

        DialogUtils.addBoundsListener(this);
    }

    private void initComponents() {
        accountPanel = new AccountPanel();

        okButton = new JButton(rb.getString("Button.Ok"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));
        helpButton = new JButton(rb.getString("Button.Help"));

        UIApplication.enableHelpOnButton(helpButton, UIApplication.NEWACCOUNT_ID);

        cancelButton.addActionListener(this);
        okButton.addActionListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("pref:grow", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendRow("f:p:g");
        builder.append(accountPanel);
        builder.nextLine();

        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(ButtonBarFactory.buildHelpOKCancelBar(helpButton, okButton, cancelButton));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        pack();
    }

    private void close() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    boolean returnStatus() {
        return returnStatus;
    }

    public Account getParentAccount() {
        return accountPanel.getParentAccount();
    }

    public void setParentAccount(final Account account) {
        accountPanel.setParentAccount(account);
    }

    public String getAccountName() {
        return accountPanel.getAccountName();
    }

    public void setAccountName(final String name) {
        accountPanel.setAccountName(name);
    }

    public String getAccountDescription() {
        return accountPanel.getAccountDescription();
    }

    public void setAccountDescription(final String desc) {
        accountPanel.setAccountDescription(desc);
    }

    public String getAccountNotes() {
        return accountPanel.getAccountNotes();
    }

    public void setAccountLocked(final boolean b) {
        accountPanel.setAccountLocked(b);
    }

    public boolean isAccountLocked() {
        return accountPanel.isAccountLocked();
    }

    public void setAccountPlaceholder(final boolean b) {
        accountPanel.setPlaceholder(b);
    }

    public boolean isAccountPlaceholder() {
        return accountPanel.isPlaceholder();
    }

    public void setPlaceholderEnabled(final boolean enabled) {
        accountPanel.placeholderCheckBox.setEnabled(enabled);
    }

    public void setAccountNotes(final String notes) {
        accountPanel.setAccountNotes(notes);
    }

    public CurrencyNode getAccountCommodity() {
        return accountPanel.getAccountCurrency();
    }

    public void setAccountCommodity(final CurrencyNode currency) {
        accountPanel.setAccountCurrency(currency);
    }

    public AccountType getAccountType() {
        return accountPanel.getAccountType();
    }

    public void setAccountType(final AccountType type) {
        accountPanel.setAccountType(type);
    }

    public void setAccountCode(final String id) {
        accountPanel.setAccountCode(id);
    }

    public String getAccountCode() {
        return accountPanel.getAccountCode();
    }

    public void setBankID(final String id) {
        accountPanel.setBankId(id);
    }

    public String getBankId() {
        return accountPanel.getBankId();
    }

    public boolean isAccountVisible() {
        return !accountPanel.isAccountHidden();
    }

    public void setAccountVisible(final boolean visible) {
        accountPanel.setAccountHidden(!visible);
    }

    public boolean isExcludedFromBudget() {
        return accountPanel.isExcludedFromBudget();
    }

    public void setExcludedFromBudget(boolean excludedFromBudget) {
        accountPanel.setExcludedFromBudget(excludedFromBudget);
    }

    public Set<SecurityNode> getAccountSecurities() {
        return accountPanel.getAccountSecurities();
    }

    public void setAccountSecurities(final Set<SecurityNode> list) {
        accountPanel.setAccountSecurities(list);
    }

    void disableAccountType(final AccountType t) {
        accountPanel.disableAccountType(t);
    }

    void disableAccountCurrency() {
        accountPanel.disableAccountCurrency();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == okButton) {
            returnStatus = true;
            close();
        } else if (e.getSource() == cancelButton) {
            returnStatus = false;
            close();
        }
    }
}
