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
package jgnash.ui.option;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import jgnash.engine.ReconcileManager;
import jgnash.ui.MainFrame;
import jgnash.ui.ThemeManager;
import jgnash.ui.UIApplication;
import jgnash.ui.components.AutoCompleteFactory;
import jgnash.ui.register.AbstractTransactionPanel;
import jgnash.ui.register.RegisterFactory;
import jgnash.util.Resource;

import org.jdesktop.swingx.JXColorSelectionButton;

/**
 * Register view options panel
 * 
 * @author Craig Cavanaugh
 * @author Peter Vida
 * @version $Id: RegisterOptions.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
class RegisterOptions extends JPanel implements ActionListener {

    private final Resource rb = Resource.get();

    private JButton evenButton;

    private JCheckBox registerFollowsCheckBox;

    private JCheckBox autoCompleteCheckBox;

    private JCheckBox sortableCheckBox;

    private JCheckBox regDateCheckBox;

    private JCheckBox ignoreCaseCheckBox;

    private JCheckBox confirmTransDeleteCheckBox;

    private JButton oddButton;

    private JRadioButton autoReconcileBothSidesButton;

    private JRadioButton autoReconcileIncomeExpenseButton;

    private JRadioButton disableAutoReconcileButton;

    public RegisterOptions() {
        layoutMainPanel();

        evenButton.setBackground(RegisterFactory.getEvenColor());
        oddButton.setBackground(RegisterFactory.getOddColor());

        if (ThemeManager.isLookAndFeelSubstance() || ThemeManager.isLookAndFeelNimbus()) {
            evenButton.setEnabled(false);
            oddButton.setEnabled(false);
        }

        autoCompleteCheckBox.setSelected(AutoCompleteFactory.isEnabled());
        ignoreCaseCheckBox.setSelected(!AutoCompleteFactory.ignoreCase());
        confirmTransDeleteCheckBox.setSelected(RegisterFactory.isConfirmTransactionDeleteEnabled());
        sortableCheckBox.setSelected(RegisterFactory.isSortingEnabled());
        registerFollowsCheckBox.setSelected(MainFrame.doesRegisterFollowTree());

        regDateCheckBox.setSelected(AbstractTransactionPanel.getRememberLastDate());

        if (ReconcileManager.getAutoReconcileBothSides()) {
            autoReconcileBothSidesButton.setSelected(true);
        } else if (ReconcileManager.getAutoReconcileIncomeExpense()) {
            autoReconcileIncomeExpenseButton.setSelected(true);
        } else if (ReconcileManager.isAutoReconcileDisabled()) {
            disableAutoReconcileButton.setSelected(true);
        }

        ignoreCaseCheckBox.setEnabled(autoCompleteCheckBox.isSelected());

        registerListeners();
    }

    private void registerListeners() {
        autoCompleteCheckBox.addActionListener(this);
        ignoreCaseCheckBox.addActionListener(this);
        confirmTransDeleteCheckBox.addActionListener(this);
        sortableCheckBox.addActionListener(this);
        registerFollowsCheckBox.addActionListener(this);

        regDateCheckBox.addActionListener(this);

        disableAutoReconcileButton.addActionListener(this);
        autoReconcileBothSidesButton.addActionListener(this);
        autoReconcileIncomeExpenseButton.addActionListener(this);

        oddButton.addPropertyChangeListener("background", new PropertyChangeListener() {

            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                RegisterFactory.setOddColor(oddButton.getBackground());
                oddButton.setToolTipText(buildColorString(oddButton.getBackground()));

                UIApplication.repaint();
            }
        });

        evenButton.addPropertyChangeListener("background", new PropertyChangeListener() {

            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                RegisterFactory.setEvenColor(evenButton.getBackground());
                evenButton.setToolTipText(buildColorString(evenButton.getBackground()));

                UIApplication.repaint();
            }
        });


        autoCompleteCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ignoreCaseCheckBox.setEnabled(autoCompleteCheckBox.isSelected());
            }
        });
    }

    private void initComponents() {
        evenButton = new JXColorSelectionButton();
        oddButton = new JXColorSelectionButton();

        autoCompleteCheckBox = new JCheckBox(rb.getString("Button.EnableAutoComplete"));
        ignoreCaseCheckBox = new JCheckBox(rb.getString("Button.MatchCaseSensitive"));

        confirmTransDeleteCheckBox = new JCheckBox(rb.getString("Button.ConfirmTransDelete"));
        sortableCheckBox = new JCheckBox(rb.getString("Button.EnableSortCol"));
        registerFollowsCheckBox = new JCheckBox(rb.getString("Button.RegisterFollowsList"));

        regDateCheckBox = new JCheckBox(rb.getString("Button.RegDate"));

        autoReconcileIncomeExpenseButton = new JRadioButton(rb.getString("Button.ReconcileIncomeExpense"));
        autoReconcileBothSidesButton = new JRadioButton(rb.getString("Button.ReconcileBoth"));
        disableAutoReconcileButton = new JRadioButton(rb.getString("Button.ReconcileDisable"));

        ButtonGroup g = new ButtonGroup();
        g.add(autoReconcileIncomeExpenseButton);
        g.add(autoReconcileBothSidesButton);
        g.add(disableAutoReconcileButton);

    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.setDefaultDialogBorder();

        builder.appendSeparator(rb.getString("Title.Colors"));
        builder.append(buildColorPanel());
        builder.appendSeparator(rb.getString("Title.DefaultBehavior"));
        builder.append(sortableCheckBox);
        builder.append(registerFollowsCheckBox);
        builder.append(autoCompleteCheckBox);
        builder.append(buildCasePanel());

        builder.append(regDateCheckBox);
        builder.append(confirmTransDeleteCheckBox);
        builder.appendSeparator(rb.getString("Title.ReconcileSettings"));
        builder.append(disableAutoReconcileButton);
        builder.append(autoReconcileBothSidesButton);
        builder.append(autoReconcileIncomeExpenseButton);

    }

    private JPanel buildCasePanel() {
        FormLayout layout = new FormLayout("$ug, p", "d");
        JPanel panel = new JPanel(layout);

        panel.add(ignoreCaseCheckBox, CC.xy(2, 1));                    

        return panel;
    }

    private JPanel buildColorPanel() {
        FormLayout layout = new FormLayout("p, $lcgap, p, 8dlu, p, $lcgap, p", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(rb.getString("Label.EvenRows"), evenButton);
        builder.append(rb.getString("Label.OddRows"), oddButton);

        return builder.getPanel();
    }

    private static String buildColorString(final Color c) {
        StringBuilder buf = new StringBuilder("[");
        buf.append(c.getRed());
        buf.append(',');
        buf.append(c.getGreen());
        buf.append(',');
        buf.append(c.getBlue());
        buf.append(']');

        return buf.toString();
    }

    private void registerFollowsAccountListAction() {
        MainFrame.setRegisterFollowsTree(registerFollowsCheckBox.isSelected());
    }

    private void reconcileAction() {
        if (disableAutoReconcileButton.isSelected()) {
            ReconcileManager.setDoNotAutoReconcile();
        } else if (autoReconcileBothSidesButton.isSelected()) {
            ReconcileManager.setAutoReconcileBothSides(true);
        } else {
            ReconcileManager.setAutoReconcileIncomeExpense(true);
        }
    }

    /**
     * Invoked when an action occurs.
     * 
     * @param e action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == sortableCheckBox) {
            RegisterFactory.setSortingEnabled(sortableCheckBox.isSelected());
        } else if (e.getSource() == autoCompleteCheckBox) {
            AutoCompleteFactory.setEnabled(autoCompleteCheckBox.isSelected());
        } else if (e.getSource() == disableAutoReconcileButton || e.getSource() == autoReconcileBothSidesButton || e.getSource() == autoReconcileIncomeExpenseButton) {
            reconcileAction();
        } else if (e.getSource() == registerFollowsCheckBox) {
            registerFollowsAccountListAction();
        } else if (e.getSource() == regDateCheckBox) {
            AbstractTransactionPanel.setRememberLastDate(regDateCheckBox.isSelected());
        } else if (e.getSource() == confirmTransDeleteCheckBox) {
            RegisterFactory.setConfirmTransactionDeleteEnabled(confirmTransDeleteCheckBox.isSelected());
        } else if (e.getSource() == ignoreCaseCheckBox) {          
            AutoCompleteFactory.setIgnoreCase(!ignoreCaseCheckBox.isSelected());
        }
    }
}
