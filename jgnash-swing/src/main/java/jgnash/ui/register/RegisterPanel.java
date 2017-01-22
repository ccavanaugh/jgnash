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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionType;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.text.CommodityFormat;
import jgnash.ui.components.ShadowBorder;
import jgnash.ui.register.invest.InvestmentTransactionDialog;
import jgnash.ui.register.table.AbstractRegisterTableModel;
import jgnash.ui.register.table.RegisterTable;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Main view for a generic account register. This displays the account's transactions and the forms for adding,
 * modifying, and removing them.
 *
 * @author Craig Cavanaugh
 * @author Don Brown
 * @author axnotizes
 */
public class RegisterPanel extends AbstractRegisterPanel implements ActionListener, RegisterListener {

    private static final String NODE_REG_TAB = "/jgnash/ui/register/tab";

    private final NumberFormat format;

    final Account account;

    private final AbstractRegisterTableModel model;

    private final RegisterTable table;

    private final TransactionPanel debitPanel;

    private final TransactionPanel creditPanel;

    private final TransferPanel transferPanel;

    private final AdjustmentPanel adjustPanel;

    JButton newButton;

    JButton deleteButton;

    JButton jumpButton;

    private JLabel accountPath;

    private JTabbedPane tabbedPane;

    private JLabel accountBalance;

    private JLabel reconciledBalance;

    private JPanel buttonPanel;

    JButton duplicateButton;

    RegisterPanel(final Account account) {
        this.account = account;
        format = CommodityFormat.getFullNumberFormat(account.getCurrencyNode());

        table = RegisterFactory.generateTable(account);
        model = (AbstractRegisterTableModel) table.getModel();

        layoutMainPanel();

        table.scrollRectToVisible(table.getCellRect(table.getRowCount() - 1, 0, true));

        deleteButton.addActionListener(this);
        duplicateButton.addActionListener(this);
        jumpButton.addActionListener(this);
        newButton.addActionListener(this);

        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT, MessageChannel.TRANSACTION);

        debitPanel = new TransactionPanel(account, PanelType.DECREASE);
        creditPanel = new TransactionPanel(account, PanelType.INCREASE);
        transferPanel = new TransferPanel(account);
        adjustPanel = new AdjustmentPanel(account);

        debitPanel.addRegisterListener(this);
        creditPanel.addRegisterListener(this);
        transferPanel.addRegisterListener(this);
        adjustPanel.addRegisterListener(this);

        String[] tabNames = RegisterFactory.getCreditDebitTabNames(account);

        tabbedPane.add(tabNames[0], creditPanel);
        tabbedPane.add(tabNames[1], debitPanel);
        tabbedPane.add(rb.getString("Tab.Transfer"), transferPanel);
        tabbedPane.add(rb.getString("Tab.Adjust"), adjustPanel);

        // set default tab to what the account is generally use for
        if (account.getAccountType() == AccountType.CHECKING || account.getAccountType() == AccountType.CREDIT) {
            tabbedPane.setSelectedComponent(debitPanel);
        } else if (account.getAccountType().getAccountGroup() == AccountGroup.INCOME) {
            tabbedPane.setSelectedComponent(debitPanel);
        }

        restoreLastTabUsed();

        tabbedPane.addChangeListener(e -> saveLastTabUsed(tabbedPane.getSelectedIndex()));

        table.addKeyListener(this);

        installPopupHandler();

        updateAccountState();
        updateAccountInfo();
    }

    void initComponents() {
        newButton = new JButton(rb.getString("Button.New"));
        deleteButton = new JButton(rb.getString("Button.Delete"));
        duplicateButton = new JButton(rb.getString("Button.Duplicate"));
        jumpButton = new JButton(rb.getString("Button.Jump"));
        tabbedPane = new JTabbedPane();

        accountPath = new JLabel(rb.getString("Label.Path"));
        accountBalance = new JLabel("0.00");
        reconciledBalance = new JLabel("0.00");
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);
        builder.border(Borders.DIALOG);

        builder.append(createTopPanel());
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.appendRow("fill:60dlu:g");

        JScrollPane s = new JScrollPane(table);
        s.setBorder(new ShadowBorder());

        builder.append(s);

        buttonPanel = createButtonPanel();

        builder.append(buttonPanel);
        builder.append(tabbedPane);
    }

    private void saveLastTabUsed(final int index) {
        Preferences tabPreferences = Preferences.userRoot().node(NODE_REG_TAB);
        String id = getAccount().getUuid();
        tabPreferences.putInt(id, index);
    }

    private void restoreLastTabUsed() {
        if (RegisterFactory.isRestoreLastTransactionTabEnabled()) {

            Preferences tabPreferences = Preferences.userRoot().node(NODE_REG_TAB);
            String id = getAccount().getUuid();

            final int index = tabPreferences.getInt(id, tabbedPane.getSelectedIndex());

            EventQueue.invokeLater(() -> tabbedPane.setSelectedIndex(index));
        }
    }

    /**
     * Creates the top panel with account path, balance, etc
     *
     * @return top panel
     */
    private JPanel createTopPanel() {
        FormLayout layout = new FormLayout("45dlu:g, 4dlu, p, 4dlu, right:p, 12dlu, p, 4dlu, right:p", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(accountPath, new JLabel(rb.getString("Label.Balance")), accountBalance);
        builder.append(new JLabel(rb.getString("Label.ReconciledBalance")), reconciledBalance);
        return builder.getPanel();
    }

    /**
     * Margins are built into this panel so that they disappear if the panel is made invisible
     *
     * @return button command panel
     */
    JPanel createButtonPanel() {
        return new ButtonBarBuilder().addButton(newButton, duplicateButton, jumpButton, deleteButton).build();
    }

    @Override
    public AbstractRegisterTableModel getTableModel() {
        return model;
    }

    @Override
    public RegisterTable getTable() {
        return table;
    }

    @Override
    protected final void updateAccountInfo() {
        accountPath.setText(account.getName());
        accountPath.setToolTipText(getAccountPath());
        accountBalance.setText(format.format(AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), account.getBalance())));
        reconciledBalance.setText(format.format(AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), account.getReconciledBalance())));
    }

    @Override
    protected Account getAccount() {
        return account;
    }

    @Override
    protected void modifyTransaction(final int index) {
        if (index >= model.getRowCount()) {
            throw new IllegalArgumentException("RegisterPanel: should not have exceeded model size");
        }

        Transaction t = model.getTransactionAt(index);

        if (t instanceof InvestmentTransaction) {

            ((AbstractEntryFormPanel) tabbedPane.getSelectedComponent()).clearForm();

            InvestmentTransactionDialog.showDialog((InvestmentTransaction) t);
            return;
        }

        if (t.getTransactionType() == TransactionType.SINGLENTRY) {
            tabbedPane.setSelectedComponent(adjustPanel);
            adjustPanel.modifyTransaction(t);
        } else if (t.getAmount(account).signum() >= 0) {
            tabbedPane.setSelectedComponent(creditPanel);
            creditPanel.modifyTransaction(t);
        } else {
            tabbedPane.setSelectedComponent(debitPanel);
            debitPanel.modifyTransaction(t);
        }
    }

    @Override
    protected void clear() {
        debitPanel.clearForm();
        creditPanel.clearForm();
        transferPanel.clearForm();
        adjustPanel.clearForm();
        table.clearSelection();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == deleteButton) {
            deleteAction();
        } else if (e.getSource() == newButton) {
            clear();
        } else if (e.getSource() == duplicateButton) {
            duplicateAction();
        } else if (e.getSource() == jumpButton) {
            jumpAction();
        }
    }

    @Override
    public void registerEvent(final RegisterEvent e) {
        if (e.getAction() == RegisterEvent.Action.CANCEL) {
            table.clearSelection();
        }
    }

    @Override
    protected final void updateAccountState() {
        buttonPanel.setVisible(!account.isLocked());
        tabbedPane.setVisible(!account.isLocked());
    }
}
