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
package jgnash.ui.option;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jgnash.ui.ThemeManager;
import jgnash.ui.UIApplication;
import jgnash.ui.components.JTextFieldEx;
import jgnash.ui.register.TransactionNumberDialog;
import jgnash.util.Resource;

/**
 * Panel for general program options
 *
 * @author Craig Cavanaugh
 *
 */
class GeneralOptions extends JPanel implements ActionListener {

    private final Resource rb = Resource.get();

    private JCheckBox animationsEnabled;

    private JButton numButton;

    private final JDialog parent;

    private JCheckBox selectOnFocusCheckBox;

    private JSpinner nimbusFontSpinner;

    GeneralOptions(final JDialog parent) {

        this.parent = parent;

        layoutMainPanel();

        showState(); // before event handlers are installed

        selectOnFocusCheckBox.addActionListener(this);
        numButton.addActionListener(this);
        animationsEnabled.addActionListener(this);

        nimbusFontSpinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {

                final int size = ((SpinnerNumberModel) nimbusFontSpinner.getModel()).getNumber().intValue();

                ThemeManager.setNimbusFontSize(size);
            }
        });
    }

    private void initComponents() {

        animationsEnabled = new JCheckBox(rb.getString("Button.SubstanceAnimations"));
        numButton = new JButton(rb.getString("Button.EditDefTranNums"));
        selectOnFocusCheckBox = new JCheckBox(rb.getString("Button.SelectText"));

        SpinnerModel model = new SpinnerNumberModel(ThemeManager.getNimbusFontSize(), 9, 15, 1);
        nimbusFontSpinner = new JSpinner(model);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("right:p, $lcgap, max(75dlu;p):g", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);
        builder.setRowGroupingEnabled(true);
        builder.setDefaultDialogBorder();

        builder.appendSeparator(rb.getString("Title.Display"));
        builder.append(animationsEnabled, 3);
        builder.append(rb.getString("Label.NimbusFontSize"), nimbusFontSpinner);

        builder.appendSeparator(rb.getString("Title.Defaults"));
        builder.append(numButton, 3);

        builder.appendSeparator(rb.getString("Title.Entry"));
        builder.append(selectOnFocusCheckBox, 3);
    }

    private void showState() {
        selectOnFocusCheckBox.setSelected(JTextFieldEx.isSelectOnFocus());
        animationsEnabled.setSelected(ThemeManager.isSubstanceAnimationsEnabled());
    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == numButton) {
            defaultNumAction();
        } else if (e.getSource() == selectOnFocusCheckBox) {
            JTextFieldEx.setSelectOnFocus(selectOnFocusCheckBox.isSelected());
        } else if (e.getSource() == animationsEnabled) {
            ThemeManager.setSubstanceAnimationsEnabled(animationsEnabled.isSelected());
        }
    }

    private void defaultNumAction() {
        parent.setVisible(false);
        TransactionNumberDialog.showDialog(UIApplication.getFrame());
        parent.setVisible(true);
    }
}
