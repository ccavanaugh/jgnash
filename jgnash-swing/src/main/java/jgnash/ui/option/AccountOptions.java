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
package jgnash.ui.option;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Objects;
import java.util.ResourceBundle;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.ui.register.AccountBalanceDisplayManager;
import jgnash.ui.register.AccountBalanceDisplayMode;
import jgnash.ui.register.RegisterFactory;
import jgnash.resource.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Panel for general account options
 * 
 * @author Craig Cavanaugh
 */
class AccountOptions extends JPanel implements ActionListener, FocusListener {

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private JTextField accountSeparatorField;

    private JCheckBox useAccountTermsCheckBox;

    private JRadioButton noneButton;

    private JRadioButton creditAccountsButton;

    private JRadioButton incomeExpenseAccountsButton;

    AccountOptions() {
        layoutMainPanel();

        useAccountTermsCheckBox.setSelected(RegisterFactory.isAccountingTermsEnabled());

        switch (AccountBalanceDisplayManager.getDisplayMode()) {
            case NONE:
                noneButton.setSelected(true);
                break;
            case REVERSE_CREDIT:
                creditAccountsButton.setSelected(true);
                break;
            default:
                incomeExpenseAccountsButton.setSelected(true);
                break;
        }

        registerListeners();
    }

    private void registerListeners() {
        accountSeparatorField.addFocusListener(this);
        useAccountTermsCheckBox.addActionListener(this);

        noneButton.addActionListener(this);
        creditAccountsButton.addActionListener(this);
        incomeExpenseAccountsButton.addActionListener(this);
    }

    private void initComponents() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        accountSeparatorField = new JTextField(engine.getAccountSeparator());
        useAccountTermsCheckBox = new JCheckBox(rb.getString("Button.AccTerms"));

        noneButton = new JRadioButton(rb.getString("Button.None"));
        noneButton.setToolTipText(rb.getString("ToolTip.ReversedSignNone"));

        creditAccountsButton = new JRadioButton(rb.getString("Button.CreditAccounts"));
        creditAccountsButton.setToolTipText(rb.getString("ToolTip.ReversedCredit"));

        incomeExpenseAccountsButton = new JRadioButton(rb.getString("Button.IncomeAndExpense"));
        incomeExpenseAccountsButton.setToolTipText(rb.getString("ToolTip.ReversedIncomeExpense"));

        ButtonGroup group = new ButtonGroup();
        group.add(noneButton);
        group.add(creditAccountsButton);
        group.add(incomeExpenseAccountsButton);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("right:p, $lcgap, max(75dlu;p):g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.border(Borders.DIALOG);

        builder.appendSeparator(rb.getString("Title.Display"));
        builder.append(rb.getString("Label.AccountSeparator"), accountSeparatorField);
        builder.appendSeparator(rb.getString("Title.Terms"));
        builder.append(useAccountTermsCheckBox, 3);
        builder.appendSeparator(rb.getString("Title.ReverseAccountBalances"));
        builder.append(noneButton, 3);
        builder.append(creditAccountsButton, 3);
        builder.append(incomeExpenseAccountsButton, 3);
    }

    private void accountBalancesAction() {
        if (noneButton.isSelected()) {
            AccountBalanceDisplayManager.setDisplayMode(AccountBalanceDisplayMode.NONE);
        } else if (creditAccountsButton.isSelected()) {
            AccountBalanceDisplayManager.setDisplayMode(AccountBalanceDisplayMode.REVERSE_CREDIT);
        } else if (incomeExpenseAccountsButton.isSelected()) {
            AccountBalanceDisplayManager.setDisplayMode(AccountBalanceDisplayMode.REVERSE_INCOME_EXPENSE);
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == useAccountTermsCheckBox) {
            RegisterFactory.setAccountingTermsEnabled(useAccountTermsCheckBox.isSelected());
        } else if (e.getSource() == noneButton || e.getSource() == creditAccountsButton || e.getSource() == incomeExpenseAccountsButton) {
            accountBalancesAction();
        }
    }

    @Override
    public void focusGained(final FocusEvent e) {
        // Ignored        
    }

    @Override
    public void focusLost(final FocusEvent e) {
        if (e.getSource() == accountSeparatorField) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            if (!accountSeparatorField.getText().isEmpty()) {
                engine.setAccountSeparator(accountSeparatorField.getText());
            } else {
                accountSeparatorField.setText(engine.getAccountSeparator());
            }
        }
    }
}
