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
package jgnash.ui.components;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JPanel;

import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

import org.jdesktop.swingx.JXMonthView;
import org.jdesktop.swingx.calendar.DateSelectionModel.SelectionMode;

/**
 * Simple close dialog to display a calendar to select a dates.
 *
 * @author Craig Cavanaugh
 *
 */
public class DateSelectDialog extends GenericCloseDialog {

    private static final long serialVersionUID = 1L;
    
    private JXMonthView view;

    public static Date showDialog(final Window parent, final Date date) {
        DateSelectDialog d = new DateSelectDialog(parent, date);

        d.pack();

        d.setMaximumSize(d.getSize());
        d.setMinimumSize(d.getSize());
        d.setResizable(false);

        DialogUtils.addBoundsListener(d);

        d.setVisible(true);

        Date selectedDate = d.getDate();

        if (selectedDate != null) {
            return selectedDate;
        }

        return date;
    }

    private DateSelectDialog(final Window parent, final Date date) {
        super(parent, new JPanel(), Resource.get().getString("Title.SelDate"));
        createPanel((JPanel) getComponent());
        setDate(date);
    }

    final void setDate(final Date date) {
        view.setFirstDisplayedDay(date);
        view.setSelectionDate(date);
    }

    Date getDate() {
        return view.getFirstSelectionDate();
    }

    private void createPanel(final JPanel panel) {

        view = new JGJXMonthView();
        view.setSelectionMode(SelectionMode.SINGLE_SELECTION);
        view.setTraversable(true);
        view.setShowingLeadingDays(true);
        view.setShowingTrailingDays(true);
        view.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                if (e.getActionCommand().equals(JGJXMonthView.DATE_ACCEPTED)) {
                    closeWindow();
                }
            }
        });

        Resource rb = Resource.get();

        JButton today = new JButton(rb.getString("Button.Today"));

        today.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                view.setFirstDisplayedDay(new Date());
                view.setSelectionDate(new Date());
            }
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
