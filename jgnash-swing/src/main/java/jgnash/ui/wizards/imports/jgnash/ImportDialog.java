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
package jgnash.ui.wizards.imports.jgnash;

import java.awt.EventQueue;
import java.util.prefs.Preferences;

import javax.swing.SwingWorker;

import jgnash.engine.DataStoreType;
import jgnash.engine.EngineFactory;
import jgnash.imports.jgnash.Import;
import jgnash.ui.UIApplication;
import jgnash.ui.components.wizard.WizardDialog;
import jgnash.ui.wizards.file.NewFileDialog;
import jgnash.util.Resource;

/**
 * Dialog for creating a new file
 *
 * @author Craig Cavanaugh
 *
 */
public class ImportDialog extends WizardDialog {

    public enum Settings {
        IMPORTFILE
    }

    private ImportDialog(java.awt.Frame parent) {
        super(parent);

        setTitle(rb.getString("Title.NewFile"));
    }

    public static void showDialog(final java.awt.Frame parent) {

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                ImportDialog d = new ImportDialog(parent);

                final String LAST_FILE = "file0";
                Preferences pref = Preferences.userNodeForPackage(ImportDialog.class);
                String lastfile = pref.get(LAST_FILE, "");

                if (lastfile.length() > 0) {
                    d.setSetting(ImportDialog.Settings.IMPORTFILE, lastfile);
                }

                d.setSetting(NewFileDialog.Settings.DATABASENAME, EngineFactory.getDefaultDatabase());

                d.addTaskPage(new ImportZero());
                d.addTaskPage(new ImportOne());

                d.setLocationRelativeTo(parent);
                d.setVisible(true);

                if (d.isWizardValid()) {
                    final String database = (String) d.getSetting(NewFileDialog.Settings.DATABASENAME);

                    final String importFile = (String) d.getSetting(Settings.IMPORTFILE);
                    final DataStoreType type = (DataStoreType) d.getSetting(NewFileDialog.Settings.TYPE);

                    // have to close the engine first
                    EngineFactory.closeEngine(EngineFactory.DEFAULT);

                    // try to delete any existing database
                    EngineFactory.deleteDatabase(database);

                    EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, type);

                    new ImportFile(importFile).execute();
                }

            }
        });
    }

    private final static class ImportFile extends SwingWorker<Void, Void> {
        private final String filename;

        public ImportFile(final String filename) {
            this.filename = filename;
        }

        @Override
        protected Void doInBackground() throws Exception {
            Resource rb = Resource.get();
            UIApplication.getFrame().displayWaitMessage(rb.getString("Message.PleaseWait"));
            Import.doImport(filename);
            return null;
        }

        @Override
        protected void done() {
            UIApplication.getFrame().stopWaitMessage();
        }
    }
}
