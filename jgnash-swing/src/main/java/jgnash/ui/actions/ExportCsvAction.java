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
package jgnash.ui.actions;

import jgnash.engine.Account;
import jgnash.exports.csv.CsvExport;
import jgnash.ui.UIApplication;
import jgnash.util.Resource;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.Date;
import java.util.prefs.Preferences;

/**
 * UI Action to export transactions to a CSV file
 *
 * @author Craig Cavanaugh
 */
public class ExportCsvAction {
    private static final String CURRENT_DIR = "cwd";

    private ExportCsvAction() {}

    public static void exportTransactions(final Account account, final Date startDate, final Date endDate) {

        final Resource rb = Resource.get();

        final Preferences pref = Preferences.userNodeForPackage(ExportCsvAction.class);

        JFileChooser chooser = new JFileChooser(pref.get(CURRENT_DIR, null));

        FileNameExtensionFilter filter = new FileNameExtensionFilter(rb.getString("Label.CsvFiles") + " (*.csv)", "csv");
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);

        if (chooser.showSaveDialog(UIApplication.getFrame()) == JFileChooser.APPROVE_OPTION) {
            pref.put(CURRENT_DIR, chooser.getCurrentDirectory().getAbsolutePath());

            // strip the file extension if added and ensure it ends with XML
            final File file = chooser.getSelectedFile();

            final class Export extends SwingWorker<Void, Void> {

                @Override
                protected Void doInBackground() throws Exception {
                    UIApplication.getFrame().displayWaitMessage(rb.getString("Message.PleaseWait"));
                    CsvExport.exportAccount(account, startDate, endDate, file);
                    return null;
                }

                @Override
                protected void done() {
                    UIApplication.getFrame().stopWaitMessage();
                }
            }

            new Export().execute();
        }
    }
}
