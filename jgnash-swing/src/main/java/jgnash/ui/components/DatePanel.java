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
package jgnash.ui.components;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.*;

import jgnash.ui.ThemeManager;
import jgnash.ui.plaf.NimbusUtils;
import jgnash.util.Resource;

/**
 * Panel that contains a date field a displays a calendar.
 *
 * @author Craig Cavanaugh
 * @author axnotizes
 *
 */
public class DatePanel extends JPanel implements ActionListener {

    private final JDateField dateField;
    private final JButton button;

    public DatePanel() {
        dateField = new JDateField();

        button = new JButton(Resource.getIcon("/jgnash/resource/office-calendar.png"));

        button.setMargin(new Insets(0, 0, 0, 0)); // take up less space
        button.addActionListener(this);

        if (ThemeManager.isLookAndFeelNimbus()) {
            NimbusUtils.reduceNimbusButtonMargin(button);

            button.setIcon(NimbusUtils.scaleIcon(Resource.getIcon("/jgnash/resource/office-calendar.png")));
        }

        FormLayout layout = new FormLayout("max(40dlu;pref):g, 1px, min", "f:d:g");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);
        builder.append(dateField, button);
    }

    /**
     * @param e event
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == button) {
            Window parent = SwingUtilities.windowForComponent(this);

            setDate(DateSelectDialog.showDialog(parent, getDate()));
        }
    }

    public Date getDate() {
        return (Date) dateField.getValue();
    }

    public void setDate(Date date) {
        dateField.setValue(date);
    }

    public JDateField getDateField() {
        return dateField;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        dateField.setEnabled(enabled);
        button.setEnabled(enabled);
    }

    public void setEditable(final boolean editable) {
        dateField.setEditable(editable);
        button.setEnabled(editable);
    }
}