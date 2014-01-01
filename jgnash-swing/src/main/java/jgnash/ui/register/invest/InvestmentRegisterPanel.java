/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Transaction;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.text.CommodityFormat;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.account.AccountSecuritiesDialog;
import jgnash.ui.components.ShadowBorder;
import jgnash.ui.register.AbstractRegisterPanel;
import jgnash.ui.register.RegisterEvent;
import jgnash.ui.register.RegisterFactory;
import jgnash.ui.register.RegisterListener;
import jgnash.ui.register.table.AbstractRegisterTableModel;
import jgnash.ui.register.table.RegisterTable;

/**
 * Main view for an investment account register. This displays the account's transactions and the forms for adding,
 * modifying, and removing them.
 * 
 * @author Craig Cavanaugh
 *
 */
public class InvestmentRegisterPanel extends AbstractRegisterPanel implements ActionListener {

    private JButton newButton;

    private JButton deleteButton;

    private JButton securitiesButton;

    private JLabel marketValue;

    private JLabel accountPath;

    private JLabel cashBalance;

    private JLabel accountBalance;

    private JPanel buttonPanel;

    private JButton duplicateButton;

    private JScrollPane jScrollPane;

    private final CommodityFormat fullFormat;

    private Account account;

    private AbstractRegisterTableModel model;

    private RegisterTable table;

    private InvestmentTransactionPanel transactionPanel;

    public InvestmentRegisterPanel(Account acc) {
        fullFormat = CommodityFormat.getFullFormat();

        if (!acc.memberOf(AccountGroup.INVEST)) {
            throw new IllegalArgumentException("Not an InvestmentAccount");
        }

        account = acc;

        table = RegisterFactory.generateTable(account);
        model = (AbstractRegisterTableModel) table.getModel();

        table.addKeyListener(this);

        transactionPanel = new InvestmentTransactionPanel(account);

        // add a listener to detect a cancel action
        transactionPanel.addRegisterListener(new RegisterListener() {

            @Override
            public void registerEvent(RegisterEvent e) {
                if (e.getAction() == RegisterEvent.Action.CANCEL) {
                    table.clearSelection();
                }
            }
        });

        deleteButton = new JButton(rb.getString("Button.Delete"));
        duplicateButton = new JButton(rb.getString("Button.Duplicate"));
        newButton = new JButton(rb.getString("Button.New"));
        securitiesButton = new JButton(rb.getString("Button.AvailSecurities"));

        accountPath = new JLabel();
        marketValue = new JLabel();
        cashBalance = new JLabel();
        accountBalance = new JLabel();
        jScrollPane = new JScrollPane(table);
        jScrollPane.setBorder(new ShadowBorder());

        table.scrollRectToVisible(table.getCellRect(table.getRowCount() - 1, 0, true));

        layoutMainPanel();

        updateAccountState();

        deleteButton.addActionListener(this);
        duplicateButton.addActionListener(this);
        newButton.addActionListener(this);
        securitiesButton.addActionListener(this);

        installPopupHandler();

        updateAccountInfo();

        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT, MessageChannel.COMMODITY, MessageChannel.TRANSACTION);
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);
        builder.border(Borders.DIALOG);

        builder.append(createTopPanel());
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.appendRow(RowSpec.decode("fill:60dlu:g"));
        builder.append(jScrollPane);
        builder.append(createButtonPanel());
        builder.append(transactionPanel);
    }

    private JPanel createTopPanel() {
        FormLayout layout = new FormLayout("45dlu:g, 8dlu, d, 4dlu, d, 8dlu, d, 4dlu, right:d, 8dlu, d, 4dlu, right:d", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(accountPath);
        builder.append(rb.getString("Label.Balance"), accountBalance);
        builder.append(rb.getString("Label.MarketValue"), marketValue);
        builder.append(rb.getString("Label.CashBalance"), cashBalance);
        return builder.getPanel();
    }

    private JPanel createButtonPanel() {
        buttonPanel = StaticUIMethods.buildLeftAlignedBar(newButton, deleteButton, duplicateButton, securitiesButton);
        return buttonPanel;
    }

    @Override
    public AbstractRegisterTableModel getTableModel() {
        return model;
    }

    @Override
    protected final void updateAccountInfo() {
        // massage the value a bit.. avoids a negative zero cash balance.
        BigDecimal cash = account.getCashBalance().setScale(getAccountCurrencyNode().getScale(), BigDecimal.ROUND_HALF_EVEN);
        BigDecimal market = account.getMarketValue();

        accountPath.setText(account.getName());
        accountPath.setToolTipText(getAccountPath()); // show full path in the
        // tool tip
        marketValue.setText(fullFormat.format(market, getAccountCurrencyNode()));
        cashBalance.setText(fullFormat.format(cash, getAccountCurrencyNode()));
        accountBalance.setText(fullFormat.format(cash.add(market), getAccountCurrencyNode()));
    }

    @Override
    protected Account getAccount() {
        return account;
    }

    @Override
    protected void modifyTransaction(int index) {
        Transaction t = model.getTransactionAt(index);
        transactionPanel.modifyTransaction(t);
    }

    @Override
    protected void clear() {
        transactionPanel.cancelAction();
        table.clearSelection();
    }

    private void addRemoveSecurities() {
        AccountSecuritiesDialog.showDialog(getAccount(), this);
    }

    @Override
    protected final void updateAccountState() {
        buttonPanel.setVisible(!account.isLocked());
        transactionPanel.setVisible(!account.isLocked());
    }

    /**
     * Invoked when an action occurs.
     * 
     * @param e action event
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == deleteButton) {
            deleteAction();
        } else if (e.getSource() == newButton) {
            clear();
        } else if (e.getSource() == duplicateButton) {
            duplicateAction();
        } else if (e.getSource() == this.securitiesButton) {
            addRemoveSecurities();
        }
    }

    /**
     * @return returns the RegisterTable
     * @see jgnash.ui.register.AbstractRegisterPanel#getTable()
     */
    @Override
    public RegisterTable getTable() {
        return table;
    }
}
