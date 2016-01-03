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
package jgnash.ui.actions;

import java.io.File;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import jgnash.convert.exports.csv.CsvExport;
import jgnash.convert.exports.ofx.OfxExport;
import jgnash.convert.exports.ssf.AccountExport;
import jgnash.engine.Account;
import jgnash.ui.UIApplication;
import jgnash.ui.register.RegisterFactory;
import jgnash.util.FileUtils;
import jgnash.util.ResourceUtils;

/**
 * UI Action to export transactions to a CSV file
 *
 * @author Craig Cavanaugh
 */
public class ExportTransactionsAction {
    private static final String CURRENT_DIR = "cwd";

    private static final String OFX = "ofx";

    private static final String XLS = "xls";

    private ExportTransactionsAction() {}

    public static void exportTransactions(final Account account, final LocalDate startDate, final LocalDate endDate) {

        final ResourceBundle rb = ResourceUtils.getBundle();

        final Preferences pref = Preferences.userNodeForPackage(ExportTransactionsAction.class);

        JFileChooser chooser = new JFileChooser(pref.get(CURRENT_DIR, null));

        FileNameExtensionFilter csvFilter = new FileNameExtensionFilter(rb.getString("Label.CsvFiles") + " (*.csv)", "csv");
        FileNameExtensionFilter ofxFilter = new FileNameExtensionFilter(rb.getString("Label.OfxFiles") + " (*.ofx)", OFX);
        FileNameExtensionFilter ssFilter = new FileNameExtensionFilter(rb.getString("Label.SpreadsheetFiles") + " (*.xls, *.xlsx)", "xls", "xlsx");
        chooser.addChoosableFileFilter(csvFilter);
        chooser.addChoosableFileFilter(ofxFilter);
        chooser.addChoosableFileFilter(ssFilter);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileFilter(csvFilter);

        if (chooser.showSaveDialog(UIApplication.getFrame()) == JFileChooser.APPROVE_OPTION) {
            pref.put(CURRENT_DIR, chooser.getCurrentDirectory().getAbsolutePath());

            final File file = chooser.getSelectedFile();

            final class Export extends SwingWorker<Void, Void> {

                @Override
                protected Void doInBackground() throws Exception {
                    UIApplication.getFrame().displayWaitMessage(rb.getString("Message.PleaseWait"));

                    if (OFX.equals(FileUtils.getFileExtension(file.getName()))) {
                        OfxExport export = new OfxExport(account, startDate, endDate, file);
                        export.exportAccount();
                    } else if (FileUtils.getFileExtension(file.getName()).contains(XLS)) {
                        AccountExport.exportAccount(account, RegisterFactory.getColumnNames(account), startDate,
                                endDate, file);
                    } else {
                        CsvExport.exportAccount(account, startDate, endDate, file);
                    }
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
