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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

import jgnash.ui.UIApplication;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * A dialog that lets the user toggle the filter settings of and AccountListPane
 * 
 * @author Craig Cavanaugh
 *
 */
public class AccountListFilterDialog extends JDialog implements ActionListener {
    private transient Resource rb = Resource.get();

    private AccountFilterModel filterModel;

    private JCheckBox incomeCheck;

    private JCheckBox hiddenCheck;

    private JCheckBox expenseCheck;

    private JCheckBox accountCheck;

    private JButton closeButton;

    public AccountListFilterDialog(final AccountFilterModel filterModel) {
        super(UIApplication.getFrame(), false);
        this.setTitle(rb.getString("Title.AccountFilter"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        this.filterModel = filterModel;

        layoutMainPanel();

        resetForm();        
    }

    private void initComponents() {
        closeButton = new javax.swing.JButton(rb.getString("Button.Close"));

        accountCheck = new JCheckBox(rb.getString("Button.BankAccounts"));
        expenseCheck = new JCheckBox(rb.getString("Button.ExpenseAccounts"));
        incomeCheck = new JCheckBox(rb.getString("Button.IncomeAccounts"));
        hiddenCheck = new JCheckBox(rb.getString("Button.Hidden"));

        closeButton.addActionListener(this);
        accountCheck.addActionListener(this);
        expenseCheck.addActionListener(this);
        hiddenCheck.addActionListener(this);
        incomeCheck.addActionListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("max(100dlu;pref):g(1.0)", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator(rb.getString("Title.AccountFilter"));

        builder.append(accountCheck);
        builder.nextLine();
        builder.append(expenseCheck);
        builder.nextLine();
        builder.append(incomeCheck);
        builder.nextLine();
        builder.append(hiddenCheck);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(ButtonBarFactory.buildCloseBar(closeButton));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        pack();

        setMinimumSize(getSize());
        
        DialogUtils.addBoundsListener(this);
    }

    private void resetForm() {
        hiddenCheck.setSelected(filterModel.isHiddenVisible());
        accountCheck.setSelected(filterModel.isAccountVisible());
        incomeCheck.setSelected(filterModel.isIncomeVisible());
        expenseCheck.setSelected(filterModel.isExpenseVisible());
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == closeButton) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } else if (e.getSource() == hiddenCheck) {
            filterModel.setHiddenVisible(hiddenCheck.isSelected());
        } else if (e.getSource() == accountCheck) {
            filterModel.setAccountVisible(accountCheck.isSelected());
        } else if (e.getSource() == incomeCheck) {
            filterModel.setIncomeVisible(incomeCheck.isSelected());
        } else if (e.getSource() == expenseCheck) {
            filterModel.setExpenseVisible(expenseCheck.isSelected());
        }
    }
}
