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
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.util.DefaultUnitConverter;
import java.awt.Dimension;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.text.StyledEditorKit;

import jgnash.engine.EngineFactory;
import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetFactory;
import jgnash.engine.budget.BudgetPeriod;
import jgnash.ui.UIApplication;
import jgnash.ui.util.DialogUtils;
import jgnash.ui.util.TextResource;
import jgnash.ui.util.ValidationFactory;
import jgnash.util.Resource;
import org.jdesktop.swingx.combobox.EnumComboBoxModel;

/**
 * BudgetWizardDialog is a mini wizard for creating a new budget based on historical data
 *
 * @author Craig Cavanaugh
 * @version $Id: BudgetWizardDialog.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
final class BudgetWizardDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    private static final int DLU_X = 150;
    private static final int DLU_Y = 60;

    private final Resource rb = Resource.get();

    private JButton okButton;

    private JButton cancelButton;

    private JTextField budgetNameField;

    private JComboBox budgetPeriodCombo;

    private JEditorPane helpPane;

    private JCheckBox roundButton;


    public static void showDialog() {

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                BudgetWizardDialog d = new BudgetWizardDialog();
                DialogUtils.addBoundsListener(d);
                d.setLocationRelativeTo(UIApplication.getFrame());
                d.setVisible(true);
            }
        });
    }

    private BudgetWizardDialog() {
        super(UIApplication.getFrame(), true);
        setTitle(rb.getString("Title.NewBudget"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        layoutMainPanel();
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p, $lcgap, f:p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.setDefaultDialogBorder();

        builder.appendRow(RowSpec.decode("f:p:g"));
        builder.append(helpPane, 3);
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(new JLabel(rb.getString("Label.Name")), ValidationFactory.wrap(budgetNameField));
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(rb.getString("Label.Period"), budgetPeriodCombo);
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(roundButton, 3);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(ButtonBarFactory.buildOKCancelBar(okButton, cancelButton), 3);

        getContentPane().add(builder.getPanel());
        pack();

        setMinimumSize(getSize());
    }

    private void initComponents() {
        okButton = new JButton(rb.getString("Button.Ok"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));

        budgetPeriodCombo = new JComboBox();
        budgetPeriodCombo.setModel(new EnumComboBoxModel<BudgetPeriod>(BudgetPeriod.class));
        budgetPeriodCombo.setSelectedItem(BudgetPeriod.MONTHLY);

        budgetNameField = new JTextField();

        DefaultUnitConverter unitConverter = DefaultUnitConverter.getInstance();

        helpPane = new JEditorPane();
        helpPane.setEditable(false);
        helpPane.setEditorKit(new StyledEditorKit());
        helpPane.setBackground(getBackground());
        helpPane.setText(TextResource.getString("NewBudgetOne.txt"));

        helpPane.setPreferredSize(new Dimension(unitConverter.dialogUnitXAsPixel(DLU_X, helpPane),
                unitConverter.dialogUnitYAsPixel(DLU_Y, helpPane)));

        roundButton = new JCheckBox(rb.getString("Button.RoundToWhole"));

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == cancelButton) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } else if (e.getSource() == okButton) {
            if (budgetNameField.getText().length() > 0) {

                BudgetPeriod period =  (BudgetPeriod)budgetPeriodCombo.getSelectedItem();

                Budget budget = BudgetFactory.buildBuildBudget(period, budgetNameField.getText(), roundButton.isSelected());

                EngineFactory.getEngine(EngineFactory.DEFAULT).addBudget(budget);

                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
            } else {
                ValidationFactory.showValidationError(rb.getString("Message.Error.Empty"), budgetNameField);
            }
        }
    }
}
