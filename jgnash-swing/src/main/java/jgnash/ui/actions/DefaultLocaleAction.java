/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import jgnash.ui.UIApplication;
import jgnash.ui.util.builder.Action;
import jgnash.util.NotNull;
import jgnash.util.ResourceUtils;

/**
 * UI Action to open the new file dialog
 * 
 * @author Craig Cavanaugh
 */
@Action("locale-command")
public class DefaultLocaleAction extends AbstractAction {

    @Override
    public void actionPerformed(final ActionEvent e) {
        SwingWorker<LocaleObject[], Void> worker = new LocaleSwingWorker();

        worker.execute();
    }

    /**
     * Internal class for display Locale objects in a nice readable and sorted order
     */
    private static class LocaleObject implements Comparable<LocaleObject> {

        final Locale locale;

        private final String display;

        LocaleObject(final Locale locale) {
            this.locale = Objects.requireNonNull(locale);

            display = locale.getDisplayName() + " - " + locale + "  [" + locale.getDisplayName(locale) + "]";
        }

        @Override
        public final String toString() {
            return display;
        }

        @Override
        public int compareTo(final @NotNull LocaleObject o) {
            return toString().compareTo(o.toString());
        }

        @Override
        public boolean equals(final Object obj) {
            assert obj instanceof LocaleObject;

            return equals((LocaleObject) obj);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 47 * hash + this.locale.hashCode();
            return 47 * hash + this.display.hashCode();
        }

        public boolean equals(final LocaleObject obj) {
            return obj.locale.equals(locale);
        }
    }

    private static class LocaleSwingWorker extends SwingWorker<LocaleObject[], Void> {

        final ResourceBundle rb = ResourceUtils.getBundle();
        private Object[] options;


        @Override
        public LocaleObject[] doInBackground() {

            options = new Object[] { rb.getString("Button.Ok"), rb.getString("Button.Cancel") };

            Locale[] tList = Locale.getAvailableLocales();
            LocaleObject[] list = new LocaleObject[tList.length];
            for (int i = 0; i < list.length; i++) {
                list[i] = new LocaleObject(tList[i]);
            }
            Arrays.sort(list);

            return list;
        }

        @Override
        public void done() {
            try {
                final JComboBox<LocaleObject> combo = new JComboBox<>(get());
                combo.setSelectedItem(new LocaleObject(Locale.getDefault()));

                final int result = JOptionPane.showOptionDialog(UIApplication.getFrame(), combo,
                        rb.getString("Title.SelDefLocale"), JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                        null, options, options[1]);

                if (result == JOptionPane.YES_OPTION) {
                    final LocaleObject o = (LocaleObject) combo.getSelectedItem();
                    ResourceUtils.setLocale(o.locale);

                    JOptionPane.showMessageDialog(UIApplication.getFrame(),
                            o + "\n" + rb.getString("Message.RestartLocale"));
                }
            } catch (final InterruptedException | ExecutionException | HeadlessException e) {
                Logger.getLogger(LocaleSwingWorker.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }
}
