/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.awt.BorderLayout;
import java.util.ResourceBundle;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jgnash.engine.recurring.OneTimeReminder;
import jgnash.engine.recurring.Reminder;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.factories.Borders;

/**
 * None repeating reminder panel
 *
 * @author Craig Cavanaugh
 */
public class NoneTab extends JPanel implements RecurringTab {

    private Reminder reminder = new OneTimeReminder();

    public NoneTab() {
        setLayout(new BorderLayout());

        ResourceBundle rb = ResourceUtils.getBundle();

        setBorder(Borders.DIALOG);
        add(new JLabel(rb.getString("Message.NoRepeat")), BorderLayout.CENTER);
    }

    /**
     * @see jgnash.ui.recurring.RecurringTab#getReminder()
     */
    @Override
    public Reminder getReminder() {
        return reminder;
    }

    /**
     * @see jgnash.ui.recurring.RecurringTab#setReminder(jgnash.engine.recurring.Reminder)
     */
    @Override
    public void setReminder(Reminder reminder) {
        assert reminder instanceof OneTimeReminder;

        this.reminder = reminder;
    }
}