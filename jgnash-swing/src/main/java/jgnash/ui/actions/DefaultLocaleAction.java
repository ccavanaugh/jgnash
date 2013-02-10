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
package jgnash.ui.actions;

import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import jgnash.ui.UIApplication;
import jgnash.ui.util.builder.Action;
import jgnash.util.Resource;

/**
 * UI Action to open the new file dialog
 * 
 * @author Craig Cavanaugh
 *
 */
@Action("locale-command")
public class DefaultLocaleAction extends AbstractAction {

    private static final long serialVersionUID = 0L;

    @Override
    public void actionPerformed(final ActionEvent e) {
        final Resource rb = Resource.get();

        SwingWorker<LocaleObject[], Void> worker = new SwingWorker<LocaleObject[], Void>() {

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
                    JComboBox<LocaleObject> combo = new JComboBox<>(get());
                    combo.setSelectedItem(new LocaleObject(Locale.getDefault()));

                    int result = JOptionPane.showOptionDialog(UIApplication.getFrame(), combo, rb.getString("Title.SelDefLocale"), JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);

                    if (result == JOptionPane.YES_OPTION) {
                        LocaleObject o = (LocaleObject) combo.getSelectedItem();
                        Resource.setLocale(o.locale);

                        JOptionPane.showMessageDialog(UIApplication.getFrame(), o.toString() + "\n" + rb.getString("Message.RestartLocale"));
                    }
                } catch (InterruptedException | ExecutionException | HeadlessException e) {
                    Logger.getLogger(DefaultLocaleAction.class.getName()).log(Level.SEVERE, null, e);                  
                }
            }
        };

        worker.execute();
    }

    /**
     * Internal class for display Locale objects in a nice readable and sorted order
     */
    private static class LocaleObject implements Comparable<LocaleObject> {

        final Locale locale;

        private final String display;

        LocaleObject(Locale locale) {
            this.locale = locale;
            display = locale.getDisplayName() + " - " + locale.toString() + "  [" + locale.getDisplayName(locale) + "]";
        }

        @Override
        public final String toString() {
            return display;
        }

        @Override
        public int compareTo(LocaleObject o) {
            return toString().compareTo(o.toString());
        }

        @Override
        public boolean equals(Object obj) {
            assert obj instanceof LocaleObject;

            return equals((LocaleObject) obj);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 47 * hash + (this.locale != null ? this.locale.hashCode() : 0);
            return 47 * hash + (this.display != null ? this.display.hashCode() : 0);
        }

        public boolean equals(LocaleObject obj) {
            return obj.locale.equals(locale);
        }
    }
}
