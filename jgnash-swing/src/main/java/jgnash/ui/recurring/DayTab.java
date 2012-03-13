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
package jgnash.ui.recurring;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import jgnash.engine.recurring.DailyReminder;
import jgnash.engine.recurring.Reminder;
import jgnash.ui.components.DatePanel;
import jgnash.util.Resource;

/**
 * Daily Reminder panel.
 *
 * @author Craig Cavanaugh
 * @version $Id: DayTab.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class DayTab extends JPanel implements RecurringTab, ActionListener {

    private JRadioButton noEndButton;

    private JRadioButton endButton;

    private JSpinner numberSpinner;

    private DatePanel endDateField;

    private final Resource rb = Resource.get();

    private ButtonGroup group = new ButtonGroup();

    private Reminder reminder = new DailyReminder();

    /**
     * Creates new form DayTab
     */
    public DayTab() {
        layoutMainPanel();

        noEndButton.addActionListener(this);
        endButton.addActionListener(this);

        noEndButton.setSelected(true);
        updateForm();
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("right:p, $lcgap, f:p, 2dlu, max(48dlu;min), 2dlu, f:p", "f:p, $lgap, f:p, $lgap, f:p");
        layout.setRowGroups(new int[][]{{1, 3, 5}});
        setLayout(layout);
        setBorder(Borders.DIALOG_BORDER);

        CellConstraints cc = new CellConstraints();

        noEndButton = new JRadioButton(rb.getString("Button.NoEndDate"));
        endButton = new JRadioButton();
        endDateField = new DatePanel();

        group.add(noEndButton);
        group.add(endButton);

        numberSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 365, 1));

        add(new JLabel(rb.getString("Label.Every")), cc.xy(1, 1));
        add(numberSpinner, cc.xywh(3, 1, 3, 1));
        add(new JLabel(rb.getString("Tab.Day")), cc.xy(7, 1));

        add(new JLabel(rb.getString("Label.EndOn")), cc.xy(1, 3));
        add(noEndButton, cc.xywh(3, 3, 5, 1));

        add(endButton, cc.xy(3, 5));
        add(endDateField, cc.xy(5, 5));
    }

    private void updateForm() {
        endDateField.setEnabled(endButton.isSelected());
    }

    /**
     * @param e action event
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == noEndButton || e.getSource() == endButton) {
            updateForm();
        }
    }

    /**
     * @see jgnash.ui.recurring.RecurringTab#getReminder()
     */
    @Override
    public Reminder getReminder() {
        DailyReminder r = (DailyReminder) reminder;

        int inc = ((Number) numberSpinner.getValue()).intValue();
        Date endDate = null;

        if (endButton.isSelected()) {
            endDate = endDateField.getDate();
        }

        r.setIncrement(inc);
        r.setEndDate(endDate);

        return reminder;
    }

    /**
     * @see jgnash.ui.recurring.RecurringTab#setReminder(jgnash.engine.recurring.Reminder)
     */
    @Override
    public void setReminder(final Reminder reminder) {
        assert reminder instanceof DailyReminder;

        this.reminder = reminder;
        DailyReminder r = (DailyReminder) reminder;

        numberSpinner.setValue(r.getIncrement());

        if (r.getEndDate() != null) {
            endDateField.setDate(r.getEndDate());
            endButton.setSelected(true);
        }
        updateForm();
    }
}
