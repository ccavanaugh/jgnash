/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
package jgnash.ui.actions;

import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.util.builder.Action;
import jgnash.time.DateUtils;
import jgnash.resource.util.ResourceUtils;

/**
 * UI Action to change the default date format
 *
 * @author Craig Cavanaugh
 */
@Action("date-format-command")
public class DefaultDateFormatAction extends AbstractAction {

    @Override
    public void actionPerformed(final ActionEvent e) {
        SwingWorker<String[], Void> worker = new LocaleSwingWorker();

        worker.execute();
    }

    private static class LocaleSwingWorker extends SwingWorker<String[], Void> {

        final ResourceBundle rb = ResourceUtils.getBundle();
        private Object[] options;

        @Override
        public String[] doInBackground() {
            options = new Object[]{rb.getString("Button.Ok"), rb.getString("Button.Cancel")};

            final Set<String> availableDateFormats = DateUtils.getAvailableDateFormats();

            return availableDateFormats.toArray(new String[0]);
        }

        @Override
        public void done() {
            try {
                final JComboBox<String> combo = new JComboBox<>(get());
                combo.setSelectedItem(DateUtils.getShortDatePattern());

                final int result = JOptionPane.showOptionDialog(UIApplication.getFrame(), combo,
                        rb.getString("Title.SelDefDateFormat"), JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                        null, options, options[1]);

                if (result == JOptionPane.YES_OPTION) {
                    final String o = (String) combo.getSelectedItem();

                    try {
                        DateUtils.setDateFormatPattern(o);
                    } catch (IllegalArgumentException e) {
                        StaticUIMethods.displayError(rb.getString("Message.Error.InvalidDateFormat"));
                    }
                }
            } catch (final InterruptedException | ExecutionException | HeadlessException e) {
                Logger.getLogger(LocaleSwingWorker.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }
}
