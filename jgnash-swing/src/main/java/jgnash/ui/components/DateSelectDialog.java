/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JPanel;

import jgnash.ui.util.DialogUtils;
import jgnash.util.DateUtils;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import org.jdesktop.swingx.JXMonthView;
import org.jdesktop.swingx.calendar.DateSelectionModel.SelectionMode;

/**
 * Simple close dialog to display a calendar to select a dates.
 *
 * @author Craig Cavanaugh
 *
 */
class DateSelectDialog extends GenericCloseDialog {

    private JXMonthView view;

    public static LocalDate showDialog(final Window parent, final LocalDate date) {
        DateSelectDialog d = new DateSelectDialog(parent, date);

        d.pack();

        d.setMaximumSize(d.getSize());
        d.setMinimumSize(d.getSize());
        d.setResizable(false);

        DialogUtils.addBoundsListener(d);

        d.setVisible(true);

        final LocalDate selectedDate = d.getDate();

        if (selectedDate != null) {
            return selectedDate;
        }

        return date;
    }

    private DateSelectDialog(final Window parent, final LocalDate date) {
        super(parent, new JPanel(), ResourceUtils.getString("Title.SelDate"));
        createPanel((JPanel) getComponent());
        setDate(date);
    }

    private void setDate(final LocalDate localDate) {

        final Date date = DateUtils.asDate(localDate);

        view.setFirstDisplayedDay(date);
        view.setSelectionDate(date);
    }

    private LocalDate getDate() {
        return DateUtils.asLocalDate(view.getFirstSelectionDate());
    }

    private void createPanel(final JPanel panel) {

        view = new JGJXMonthView();
        view.setSelectionMode(SelectionMode.SINGLE_SELECTION);
        view.setTraversable(true);
        view.setShowingLeadingDays(true);
        view.setShowingTrailingDays(true);
        view.addActionListener(e -> {
            if (e.getActionCommand().equals(JGJXMonthView.DATE_ACCEPTED)) {
                closeWindow();
            }
        });

        final JButton today = new JButton(ResourceUtils.getString("Button.Today"));

        today.addActionListener(e -> {
            view.setFirstDisplayedDay(new Date());
            view.setSelectionDate(new Date());
        });

        FormLayout layout = new FormLayout("fill:p:g", "f:p:g, $rgap, f:p");
        panel.setLayout(layout);
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, panel);

        builder.append(view);
        builder.nextLine();
        builder.nextLine();
        builder.append(today);
    }

    private void closeWindow() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }
}
