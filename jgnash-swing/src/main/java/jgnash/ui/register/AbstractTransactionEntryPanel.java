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
package jgnash.ui.register;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.components.AutoCompleteFactory;
import jgnash.ui.components.JFloatField;
import jgnash.ui.register.table.SplitsRegisterTableModel;

/**
 * Abstract Entry panel for spit transactions of various types
 *
 * @author Craig Cavanaugh
 *
 */
public abstract class AbstractTransactionEntryPanel extends AbstractEntryFormPanel implements ActionListener {

    protected AccountExchangePanel accountPanel;

    protected JCheckBox reconciledButton;

    protected JFloatField amountField;

    protected JTextField memoField;

    private JButton enterButton;

    private JButton cancelButton;

    protected final Account account;

    protected TransactionEntry oldEntry;

    private SplitsRegisterTableModel model = null;

    protected AbstractTransactionEntryPanel(SplitsRegisterTableModel model) {

        this.model = model;
        account = model.getAccount();

        layoutMainPanel();        
    }

    private void init() {

        amountField = new JFloatField(account.getCurrencyNode());

        accountPanel = new AccountExchangePanel(account.getCurrencyNode(), account, amountField);

        enterButton = new JButton(rb.getString("Button.Enter"));
        cancelButton = new JButton(rb.getString("Button.Clear"));

        memoField = AutoCompleteFactory.getMemoField();

        reconciledButton = new JCheckBox(rb.getString("Button.Reconciled"));
        reconciledButton.setHorizontalTextPosition(SwingConstants.LEADING);
        reconciledButton.setMargin(new Insets(0, 0, 0, 0));

        /* Connect the buttons to the form */
        cancelButton.addActionListener(this);
        enterButton.addActionListener(this);

        /* Allows the user to submit the form from the keyboard when the enter button is selected */
        KeyAdapter enterKeyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    enterAction();
                }
            }
        };

        enterButton.addKeyListener(enterKeyListener);
        amountField.addKeyListener(enterKeyListener);

        setFocusCycleRoot(true); // focus will return to the first field
    }

    private void layoutMainPanel() {

        init();

        FormLayout layout = new FormLayout("d, 4dlu, d:g, 8dlu, d, 4dlu, 45dlu",
                "f:d, $nlgap, f:d, $nlgap, f:d");
        layout.setRowGroups(new int[][]{{1, 3, 5}});
        CellConstraints cc = new CellConstraints();

        setLayout(layout);
        setBorder(Borders.DIALOG);

        add("Label.Account", cc.xy(1, 1));
        add(accountPanel, cc.xy(3, 1));
        add("Label.Amount", cc.xy(5, 1));
        add(amountField, cc.xy(7, 1));

        add("Label.Memo", cc.xy(1, 3));
        add(memoField, cc.xywh(3, 3, 5, 1));

        add(createBottomPanel(), cc.xywh(1, 5, 7, 1));
        
        clearForm();
    }

    /* The reconciled button is not used for split entry */
    private JPanel createBottomPanel() {
        FormLayout layout = new FormLayout("m, 8dlu, m:g", "f:d");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(reconciledButton, StaticUIMethods.buildOKCancelBar(enterButton, cancelButton));

        return builder.getPanel();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == cancelButton) {
            cancelAction();
        } else if (e.getSource() == enterButton) {
            enterAction();
        }
    }

    protected abstract TransactionEntry buildTransactionEntry();

    @Override
    public void clearForm() {

        oldEntry = null;

        memoField.setText(null);
        amountField.setDecimal(null);
        reconciledButton.setSelected(false);
        accountPanel.setExchangedAmount(null);
    }

    @Override
    public void enterAction() {
        if (validateForm()) {
            TransactionEntry entry = buildTransactionEntry();

            if (oldEntry != null) {
                model.modifyTransaction(oldEntry, entry);
            } else {
                model.addTransaction(entry);
            }

            clearForm();
            fireOkAction();
        }
    }

    void cancelAction() {
        clearForm();
        fireCancelAction();
        focusFirstComponent();
    }

    protected Account getAccount() {
        return account;
    }

    @Override
    protected boolean validateForm() {
        return !amountField.getText().equals("");
    }

    protected boolean hasEqualCurrencies() {
        return getAccount().getCurrencyNode().equals(accountPanel.getSelectedAccount().getCurrencyNode());
    }

    @Override
    public void modifyTransaction(final Transaction t) {

        // this panel is for TransactionEntry manipulation only
        throw new IllegalStateException();
    }
}
