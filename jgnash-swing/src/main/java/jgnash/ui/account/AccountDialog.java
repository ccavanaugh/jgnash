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
package jgnash.ui.account;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.SecurityNode;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.util.DialogUtils;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Account dialog
 *
 * @author Craig Cavanaugh
 */
class AccountDialog extends JDialog implements ActionListener {
    private final transient ResourceBundle rb = ResourceUtils.getBundle();

    private boolean returnStatus = false;

    private AccountPanel accountPanel = null;

    private JButton cancelButton;

    private JButton okButton;

    AccountDialog() {
        super(UIApplication.getFrame(), true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        layoutMainPanel();
    }

    private void initComponents() {
        accountPanel = new AccountPanel();

        okButton = new JButton(rb.getString("Button.Ok"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));

        cancelButton.addActionListener(this);
        okButton.addActionListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("pref:grow", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(Borders.DIALOG);

        builder.appendRow("f:p:g");
        builder.append(accountPanel);
        builder.nextLine();

        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(StaticUIMethods.buildOKCancelBar(okButton, cancelButton));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        pack();
        setMinimumSize(getSize());

        DialogUtils.addBoundsListener(this);
    }

    private void close() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    boolean returnStatus() {
        return returnStatus;
    }

    Account getParentAccount() {
        return accountPanel.getParentAccount();
    }

    void setParentAccount(final Account account) {
        accountPanel.setParentAccount(account);
    }

    String getAccountName() {
        return accountPanel.getAccountName();
    }

    void setAccountName(final String name) {
        accountPanel.setAccountName(name);
    }

    String getAccountDescription() {
        return accountPanel.getAccountDescription();
    }

    void setAccountDescription(final String desc) {
        accountPanel.setAccountDescription(desc);
    }

    String getAccountNotes() {
        return accountPanel.getAccountNotes();
    }

    void setAccountLocked(final boolean b) {
        accountPanel.setAccountLocked(b);
    }

    boolean isAccountLocked() {
        return accountPanel.isAccountLocked();
    }

    void setAccountPlaceholder(final boolean b) {
        accountPanel.setPlaceholder(b);
    }

    boolean isAccountPlaceholder() {
        return accountPanel.isPlaceholder();
    }

    void setPlaceholderEnabled(final boolean enabled) {
        accountPanel.placeholderCheckBox.setEnabled(enabled);
    }

    void setAccountNotes(final String notes) {
        accountPanel.setAccountNotes(notes);
    }

    public CurrencyNode getCurrency() {
        return accountPanel.getAccountCurrency();
    }

    public void setCurrency(final CurrencyNode currency) {
        accountPanel.setAccountCurrency(currency);
    }

    public AccountType getAccountType() {
        return accountPanel.getAccountType();
    }

    void setAccountType(final AccountType type) {
        accountPanel.setAccountType(type);
    }

    void setAccountCode(final int accountCode) {
        accountPanel.setAccountCode(accountCode);
    }

    int getAccountCode() {
        return accountPanel.getAccountCode();
    }

    void setAccountNumber(final String accountNumber) {
        accountPanel.setAccountNumber(accountNumber);
    }

    String getAccountNumber() {
        return accountPanel.getAccountNumber();
    }

    void setBankId(final String id) {
        accountPanel.setBankId(id);
    }

    String getBankId() {
        return accountPanel.getBankId();
    }

    boolean isAccountVisible() {
        return !accountPanel.isAccountHidden();
    }

    void setAccountVisible(final boolean visible) {
        accountPanel.setAccountHidden(!visible);
    }

    void setModifyingAccount(final Account account) {
        accountPanel.setModifyingAccount(account);
    }

    boolean isExcludedFromBudget() {
        return accountPanel.isExcludedFromBudget();
    }

    void setExcludedFromBudget(boolean excludedFromBudget) {
        accountPanel.setExcludedFromBudget(excludedFromBudget);
    }

    Set<SecurityNode> getAccountSecurities() {
        return accountPanel.getAccountSecurities();
    }

    void setAccountSecurities(final Set<SecurityNode> list) {
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
