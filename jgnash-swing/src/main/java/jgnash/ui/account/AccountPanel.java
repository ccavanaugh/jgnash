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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.SecurityNode;
import jgnash.ui.components.CurrencyComboBox;
import jgnash.ui.components.JIntegerField;
import jgnash.ui.components.JTextFieldEx;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Panel for entering / modifying account information.
 *
 * @author Craig Cavanaugh
 */
final class AccountPanel extends JPanel implements ActionListener {

    private final transient ResourceBundle rb = ResourceUtils.getBundle();

    private Account parentAccount;

    private Set<SecurityNode> commodityList = new TreeSet<>();

    private CurrencyComboBox currencyCombo;

    private JTextFieldEx accountNumberField;

    JCheckBox placeholderCheckBox;

    private JTextFieldEx nameField;

    private JCheckBox lockedCheckBox;

    private JButton parentButton;

    private JTextFieldEx descriptionField;

    private JButton securityButton;

    private JTextArea notesArea;

    private JTextField bankIdField;

    private JIntegerField accountCodeField;

    private JCheckBox hideCheckBox;

    private JCheckBox excludeBudgetCheckBox;

    private JComboBox<AccountType> accountTypeCombo;

    private DefaultComboBoxModel<AccountType> accountTypeModel;

    private Account modifyingAccount;

    public AccountPanel() {
        layoutMainPanel();

        setAccountType(AccountType.BANK);
    }

    private void initComponents() {
        accountNumberField = new JTextFieldEx();
        accountCodeField = new JIntegerField();
        nameField = new JTextFieldEx();
        bankIdField = new JTextFieldEx();
        nameField.setText(rb.getString("Word.Name"));
        descriptionField = new JTextFieldEx();
        descriptionField.setText(rb.getString("Word.Description"));
        currencyCombo = new CurrencyComboBox();
        securityButton = new JButton(rb.getString("Word.None"));

        // for preferred width so button does not force a wide layout
        securityButton.setPreferredSize(new Dimension(20, securityButton.getPreferredSize().height));

                               
        accountTypeModel = new DefaultComboBoxModel<>(AccountType.values());
        accountTypeModel.removeElement(AccountType.ROOT);
        accountTypeCombo = new JComboBox<>(accountTypeModel);

        lockedCheckBox = new JCheckBox(rb.getString("Button.Locked"));
        placeholderCheckBox = new JCheckBox(rb.getString("Button.PlaceHolder"));
        hideCheckBox = new JCheckBox(rb.getString("Button.HideAccount"));
        excludeBudgetCheckBox = new JCheckBox(rb.getString("Button.ExcludeFromBudget"));
        parentButton = new JButton("Root");

        notesArea = new javax.swing.JTextArea();
        notesArea.setLineWrap(true);
        notesArea.setAutoscrolls(false);
        notesArea.setPreferredSize(new java.awt.Dimension(100, 80));

        accountTypeCombo.addActionListener(this);
        securityButton.addActionListener(this);
        parentButton.addActionListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("right:max(20dlu;pref), $lcgap, d, $lcgap, d:g", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.appendSeparator(rb.getString("Title.AccountInfo"));
        builder.rowGroupingEnabled(true);
        builder.append(rb.getString("Label.Name"), nameField, 3);
        builder.nextLine();
        builder.append(rb.getString("Label.Description"), descriptionField, 3);
        builder.nextLine();
        builder.append(rb.getString("Label.AccountNumber"), accountNumberField, 3);
        builder.nextLine();
        builder.append(rb.getString("Label.AccountCode"), accountCodeField, 3);
        builder.nextLine();
        builder.append(rb.getString("Label.BankID"), bankIdField, 3);
        builder.nextLine();
        builder.append(rb.getString("Label.Currency"), currencyCombo, 3);
        builder.nextLine();
        builder.append(rb.getString("Label.Securities"), securityButton, 3);
        builder.nextLine();
        builder.append(rb.getString("Label.AccountType"), accountTypeCombo, 3);
        builder.nextLine();
        builder.append(rb.getString("Label.AccountOptions"), lockedCheckBox, hideCheckBox);
        builder.nextLine();
        builder.append("", placeholderCheckBox, excludeBudgetCheckBox);
        builder.rowGroupingEnabled(false);
        builder.appendSeparator(rb.getString("Title.ParentAccount"));
        builder.append(parentButton, 5);
        builder.appendSeparator(rb.getString("Title.Notes"));

        JScrollPane pane = new JScrollPane(notesArea);
        pane.setAutoscrolls(true);
        builder.appendRow("f:60dlu:g(1.0)");
        builder.append(pane, 5);
    }

    private void showAccountListDialog() {
        AccountListDialog dlg = new AccountListDialog(parentAccount);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        if (dlg.getReturnStatus()) {
            setParentAccount(dlg.getAccount());
        }
    }

    private void accountAction() {
        updateCommodityButton();
    }

    public void setParentAccount(final Account parentAccount) {
        this.parentAccount = parentAccount;
        parentButton.setText(parentAccount.getName());
        setAccountCurrency(parentAccount.getCurrencyNode());
    }

    public Account getParentAccount() {
        return parentAccount;
    }

    public String getAccountName() {
        return nameField.getText();
    }

    public void setAccountName(final String name) {
        nameField.setText(name);
    }

    public void setBankId(final String id) {
        bankIdField.setText(id);
    }

    public void setAccountHidden(final boolean hide) {
        hideCheckBox.setSelected(hide);
    }

    public String getAccountNumber() {
        return accountNumberField.getText();
    }

    public void setAccountNumber(final String accountNumber) {
        accountNumberField.setText(accountNumber);
    }

    public int getAccountCode() {
        return accountCodeField.intValue();
    }

    public void setAccountCode(final int accountCode) {
        accountCodeField.setIntValue(accountCode);
    }

    public String getBankId() {
        return bankIdField.getText();
    }

    public void setAccountDescription(final String description) {
        descriptionField.setText(description);
    }

    public String getAccountDescription() {
        return descriptionField.getText();
    }

    public void setAccountCurrency(final CurrencyNode node) {
        currencyCombo.setSelectedNode(node);
    }

    public CurrencyNode getAccountCurrency() {
        return currencyCombo.getSelectedNode();
    }

    void disableAccountCurrency() {
        currencyCombo.setEnabled(false);
    }

    public void setAccountNotes(String notes) {
        notesArea.setText(notes);
    }

    public String getAccountNotes() {
        return notesArea.getText();
    }

    public void setAccountLocked(final boolean locked) {
        lockedCheckBox.setSelected(locked);
    }

    public boolean isAccountLocked() {
        return lockedCheckBox.isSelected();
    }

    public void setPlaceholder(final boolean selected) {
        placeholderCheckBox.setSelected(selected);
    }

    public boolean isPlaceholder() {
        return placeholderCheckBox.isSelected();
    }

    public void setAccountType(final AccountType type) {
        accountTypeCombo.setSelectedItem(type);
        updateCommodityButton();
    }

    public void setModifyingAccount(final Account account) {
        modifyingAccount = account;
    }

    public AccountType getAccountType() {
        return (AccountType) accountTypeCombo.getSelectedItem();
    }

    public boolean isAccountHidden() {
        return hideCheckBox.isSelected();
    }

    public void setExcludedFromBudget(final boolean excludeBudget) {
        excludeBudgetCheckBox.setSelected(excludeBudget);
    }

    public boolean isExcludedFromBudget() {
        return excludeBudgetCheckBox.isSelected();
    }

    public void setAccountSecurities(final Set<SecurityNode> list) {
        commodityList.clear();
        commodityList.addAll(list);
    }

    public Set<SecurityNode> getAccountSecurities() {
        return commodityList;
    }

    private void showSecuritiesDialog() {
        AccountSecuritiesDialog dlg = new AccountSecuritiesDialog(modifyingAccount, commodityList, this);
        dlg.setVisible(true);

        if (dlg.getReturnValue()) {
            commodityList = dlg.getSecuritiesList();
        }
        updateCommodityText();
    }

    private void updateCommodityButton() {
        AccountType type = getAccountType();

        if (type == AccountType.INVEST || type == AccountType.MUTUAL) {
            securityButton.setEnabled(true);
            updateCommodityText();
        } else {
            securityButton.setEnabled(false);
        }
    }

    private void updateCommodityText() {
        if (!commodityList.isEmpty()) {
            StringBuilder buf = new StringBuilder();
            Iterator<SecurityNode> it = commodityList.iterator();

            SecurityNode node = it.next();
            buf.append(node.getSymbol());
            while (it.hasNext()) {
                buf.append(", ");
                node = it.next();
                buf.append(node.getSymbol());
            }
            securityButton.setText(buf.toString());
            securityButton.setToolTipText(buf.toString());
        } else {
            securityButton.setText(rb.getString("Word.None"));
        }
    }

    void disableAccountType(final AccountType type) {
        if (type.isMutable()) {
            for (AccountType t : AccountType.values()) {
                if (!t.isMutable()) {
                    accountTypeModel.removeElement(t);
                }
            }
        } else {
            accountTypeCombo.setSelectedItem(type);
            accountTypeCombo.setEnabled(false);
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == accountTypeCombo) {
            accountAction();
        } else if (e.getSource() == securityButton) {
            showSecuritiesDialog();
        } else if (e.getSource() == parentButton) {
            showAccountListDialog();
        }
    }
}