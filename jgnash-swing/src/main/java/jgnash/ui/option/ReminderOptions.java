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
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import jgnash.ui.recurring.RecurringPanel;
import jgnash.util.Resource;

/**
 * Reminder options panel.
 *
 * @author Craig Cavanaugh
 * @version $Id: ReminderOptions.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
class ReminderOptions extends JPanel implements ActionListener {

    private final Resource rb = Resource.get();

    private JCheckBox confirmDeleteButton;

    public ReminderOptions() {
        layoutMainPanel();

        confirmDeleteButton.addActionListener(this);
    }

    private void initComponents() {
        confirmDeleteButton = new JCheckBox(rb.getString("Button.ConfirmReminderDelete"));
        confirmDeleteButton.setSelected(RecurringPanel.isConfirmReminderDeleteEnabled());
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("right:p, $lcgap, max(75dlu;p):g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.setRowGroupingEnabled(true);
        builder.setDefaultDialogBorder();

        builder.append(confirmDeleteButton, 3);
    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == confirmDeleteButton) {
            RecurringPanel.setConfirmReminderDeleteEnabled(confirmDeleteButton.isSelected());
        }
    }
}