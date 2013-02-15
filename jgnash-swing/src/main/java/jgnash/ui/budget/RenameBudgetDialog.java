/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import jgnash.engine.EngineFactory;
import jgnash.engine.budget.Budget;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.util.DialogUtils;
import jgnash.ui.util.ValidationFactory;
import jgnash.util.Resource;

/**
 * RenameBudgetDialog is for changing the name of a budget
 *
 * @author Craig Cavanaugh
 *
 */
class RenameBudgetDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 1L;

    private final Resource rb = Resource.get();

    private JButton okButton;

    private JButton cancelButton;

    private JTextField budgetNameField;

    private Budget budget;

    public static void showDialog(final Budget budget, final Dialog parent) {

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                RenameBudgetDialog d = new RenameBudgetDialog(budget, parent);
                DialogUtils.addBoundsListener(d);
                d.setLocationRelativeTo(parent);
                d.setVisible(true);
            }
        });
    }

    private RenameBudgetDialog(final Budget budget, final Dialog parent) {
        super(parent, true);
        setTitle(rb.getString("Title.RenameBudget"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        if (budget == null) {
            throw new IllegalArgumentException("budget may not be null");
        }

        this.budget = budget;

        layoutMainPanel();
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p, $lcgap, fill:100dlu:g", "f:p:g, $ugap, f:p");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(Borders.DIALOG);

        builder.append(new JLabel(rb.getString("Label.RenameBudget")), ValidationFactory.wrap(budgetNameField));
        builder.nextRow();
        builder.append(StaticUIMethods.buildOKCancelBar(okButton, cancelButton), 3);

        getContentPane().add(builder.getPanel());
        pack();
    }

    private void initComponents() {
        okButton = new JButton(rb.getString("Button.Ok"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));

        budgetNameField = new JTextField(budget.getName());

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == cancelButton) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } else if (e.getSource() == okButton) {
            if (budgetNameField.getText().length() > 0) {
                budget.setName(budgetNameField.getText());
                EngineFactory.getEngine(EngineFactory.DEFAULT).updateBudget(budget);
                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
            } else {
                ValidationFactory.showValidationError(rb.getString("Message.Error.Empty"), budgetNameField);
            }
        }
    }

}
