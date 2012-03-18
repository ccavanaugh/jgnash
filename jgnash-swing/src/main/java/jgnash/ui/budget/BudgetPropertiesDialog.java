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
package jgnash.ui.budget;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import jgnash.engine.EngineFactory;
import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetPeriod;
import jgnash.ui.UIApplication;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * Dialog for displaying and changing a budget's properties
 *
 * @author Craig Cavanaugh
 *
 */
public final class BudgetPropertiesDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private final Resource rb = Resource.get();

    private JComboBox<BudgetPeriod> budgetPeriodCombo;

    private JButton okButton;

    private JButton cancelButton;

    private JTextField descriptionField;

    private JCheckBox assetGroupCheckBox;

    private JCheckBox expenseGroupCheckBox;

    private JCheckBox incomeGroupCheckBox;

    private JCheckBox liabilityGroupCheckBox;

    private Budget budget;

    public BudgetPropertiesDialog(final Budget budget) {
        super(UIApplication.getFrame(), true);
        setTitle(rb.getString("Title.BudgetProperties"));
        setIconImage(Resource.getImage("/jgnash/resource/gnome-money.png"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        this.budget = budget;

        layoutMainPanel();

        setSelectedPeriod(budget.getBudgetPeriod());
        descriptionField.setText(budget.getDescription());
    }

    private void setSelectedPeriod(final BudgetPeriod period) {
        budgetPeriodCombo.setSelectedItem(period);
    }

    private BudgetPeriod getSelectedPeriod() {
        return (BudgetPeriod) budgetPeriodCombo.getSelectedItem();
    }

    private void initComponents() {
        budgetPeriodCombo = new JComboBox<>();       
        budgetPeriodCombo.setModel(new DefaultComboBoxModel<>(BudgetPeriod.values()));

        descriptionField = new JTextField();

        assetGroupCheckBox = new JCheckBox(rb.getString("Button.AssetAccounts"));
        incomeGroupCheckBox = new JCheckBox(rb.getString("Button.IncomeAccounts"));
        expenseGroupCheckBox = new JCheckBox(rb.getString("Button.ExpenseAccounts"));
        liabilityGroupCheckBox = new JCheckBox(rb.getString("Button.LiabilityAccounts"));

        assetGroupCheckBox.setSelected(budget.areAssetAccountsIncluded());
        incomeGroupCheckBox.setSelected(budget.areIncomeAccountsIncluded());
        expenseGroupCheckBox.setSelected(budget.areExpenseAccountsIncluded());
        liabilityGroupCheckBox.setSelected(budget.areLiabilityAccountsIncluded());

        okButton = new JButton(rb.getString("Button.Ok"));
        okButton.addActionListener(this);

        cancelButton = new JButton(rb.getString("Button.Cancel"));
        cancelButton.addActionListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p, $lcgap, f:max(90dlu;p)", "f:p, $rgap, f:p, $ugap, f:p, $rgap, f:p, $rgap, f:p, $rgap, f:p, $rgap, f:p, $ugap, p");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();

        builder.add(new JLabel(rb.getString("Label.Description")), cc.xy(1, 1));
        builder.add(descriptionField, cc.xy(3, 1));

        builder.add(new JLabel(rb.getString("Label.Period")), cc.xy(1, 3));
        builder.add(budgetPeriodCombo, cc.xy(3, 3));

        builder.addSeparator(rb.getString("Title.AccountGroups"), cc.xyw(1, 5, 3));
        builder.add(incomeGroupCheckBox, cc.xyw(1, 7, 3));
        builder.add(expenseGroupCheckBox, cc.xyw(1, 9, 3));
        builder.add(assetGroupCheckBox, cc.xyw(1, 11, 3));
        builder.add(liabilityGroupCheckBox, cc.xyw(1, 13, 3));

        builder.add(ButtonBarFactory.buildOKCancelBar(okButton, cancelButton), cc.xyw(1, 15, 3));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        pack();

        setResizable(false);

        DialogUtils.addBoundsListener(this);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == okButton) {

            boolean modified = false;

            if (budget.getBudgetPeriod() != getSelectedPeriod()) {
                modified = true;
                budget.setBudgetPeriod(getSelectedPeriod());
            }

            if (descriptionField.getText().length() > 0 && !budget.getDescription().equals(descriptionField.getText())) {
                modified = true;
                budget.setDescription(descriptionField.getText());
            }

            if (assetGroupCheckBox.isSelected() != budget.areAssetAccountsIncluded()) {
                modified = true;
                budget.setAssetAccountsIncluded(assetGroupCheckBox.isSelected());
            }

            if (incomeGroupCheckBox.isSelected() != budget.areIncomeAccountsIncluded()) {
                modified = true;
                budget.setIncomeAccountsIncluded(incomeGroupCheckBox.isSelected());
            }

            if (expenseGroupCheckBox.isSelected() != budget.areExpenseAccountsIncluded()) {
                modified = true;
                budget.setExpenseAccountsIncluded(expenseGroupCheckBox.isSelected());
            }

            if (liabilityGroupCheckBox.isSelected() != budget.areLiabilityAccountsIncluded()) {
                modified = true;
                budget.setLiabilityAccountsIncluded(liabilityGroupCheckBox.isSelected());
            }

            if (modified) {
                Thread thread = new Thread() {

                    @Override
                    public void run() {
                        EngineFactory.getEngine(EngineFactory.DEFAULT).updateBudget(budget);
                    }
                };

                thread.start();
            }

            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } else if (e.getSource() == cancelButton) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }
}
