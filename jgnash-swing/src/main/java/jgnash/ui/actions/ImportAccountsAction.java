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

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import jgnash.engine.AccountTreeXMLFactory;
import jgnash.engine.EngineFactory;
import jgnash.engine.RootAccount;
import jgnash.ui.UIApplication;
import jgnash.ui.util.builder.Action;
import jgnash.util.Resource;

/**
 * Import a tree of accounts
 *
 * @author Craig Cavanaugh
 */
@Action("accountsimport-command")
public class ImportAccountsAction extends AbstractEnabledAction {

    private static final long serialVersionUID = -2708791260270035876L;

    private static final String ACCOUNTS_IMPORT_DIR = "AccountsImportDirectroy";

    private static void importAccounts() {
        Preferences pref = Preferences.userNodeForPackage(ImportAccountsAction.class);

        final Resource rb = Resource.get();

        JFileChooser chooser = new JFileChooser(pref.get(ACCOUNTS_IMPORT_DIR, null));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(rb.getString("Label.XMLFiles") + " (*.xml)", "xml"));
        chooser.setMultiSelectionEnabled(false);

        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            final File file = chooser.getSelectedFile();
            pref.put(ACCOUNTS_IMPORT_DIR, chooser.getCurrentDirectory().getAbsolutePath());

            final class Import extends SwingWorker<Void, Void> {

                @Override
                protected Void doInBackground() throws Exception {
                    UIApplication.getFrame().displayWaitMessage(Resource.get().getString("Message.ImportWait"));

                    RootAccount root = AccountTreeXMLFactory.loadAccountTree(file);

                    if (root != null) {
                        AccountTreeXMLFactory.mergeAccountTree(EngineFactory.getEngine(EngineFactory.DEFAULT), root);
                    }

                    return null;
                }

                @Override
                protected void done() {
                    UIApplication.getFrame().stopWaitMessage();
                }
            }

            new Import().execute();
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        importAccounts();
    }

}
