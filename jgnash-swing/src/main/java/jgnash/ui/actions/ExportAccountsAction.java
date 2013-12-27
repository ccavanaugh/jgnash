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

import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;

import jgnash.engine.AccountTreeXMLFactory;
import jgnash.engine.EngineFactory;
import jgnash.ui.UIApplication;
import jgnash.ui.util.builder.Action;
import jgnash.util.FileUtils;
import jgnash.util.Resource;

/**
 * UI Action to export the current account tree
 *
 * @author Craig Cavanaugh
 */
@Action("exportAccounts-command")
public class ExportAccountsAction extends AbstractEnabledAction {

    private static final String CURRENT_DIR = "cwd";

    private static void exportAccounts() {

        final Resource rb = Resource.get();

        final Preferences pref = Preferences.userNodeForPackage(ExportAccountsAction.class);

        JFileChooser chooser = new JFileChooser(pref.get(CURRENT_DIR, null));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(rb.getString("Label.XMLFiles") + " (*.xml)", "xml"));
        chooser.setMultiSelectionEnabled(false);

        if (chooser.showSaveDialog(UIApplication.getFrame()) == JFileChooser.APPROVE_OPTION) {
            pref.put(CURRENT_DIR, chooser.getCurrentDirectory().getAbsolutePath());

            // strip the file extension if added and ensure it ends with XML
            final File file = new File(FileUtils.stripFileExtension(chooser.getSelectedFile().getAbsolutePath()) + ".xml");

            final class Export extends SwingWorker<Void, Void> {

                @Override
                protected Void doInBackground() throws Exception {
                    UIApplication.getFrame().displayWaitMessage(rb.getString("Message.PleaseWait"));
                    AccountTreeXMLFactory.exportAccountTree(EngineFactory.getEngine(EngineFactory.DEFAULT), file);
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

    @Override
    public void actionPerformed(final ActionEvent e) {
        exportAccounts();
    }
}
