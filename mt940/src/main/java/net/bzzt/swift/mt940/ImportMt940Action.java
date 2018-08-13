/*
 * Copyright (C) 2008 Arnout Engelen
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
package net.bzzt.swift.mt940;

import net.bzzt.swift.mt940.exporter.Mt940Exporter;
import net.bzzt.swift.mt940.parser.Mt940Parser;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.SwingWorker;

import jgnash.convert.importat.GenericImport;
import jgnash.convert.importat.ImportBank;
import jgnash.convert.importat.ImportTransaction;
import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.actions.AbstractEnabledAction;
import jgnash.ui.wizards.imports.ImportDialog;
import jgnash.resource.util.ResourceUtils;

/**
 * UI Action to import an mt940 file.
 *
 * @author Arnout Engelen
 * @author Craig Cavanaugh
 */
class ImportMt940Action extends AbstractEnabledAction {

    ImportMt940Action() {

        final ResourceBundle rb = ResourceUtils.getBundle();

        // set name, etc
        putValue(NAME, rb.getString("Menu.ImportMt940.Name"));
        putValue(Action.SHORT_DESCRIPTION, rb.getString("Menu.ImportMt940.Tooltip"));
    }

    private static void importMt940() {
        final ResourceBundle rb = ResourceUtils.getBundle();

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine == null || engine.getRootAccount().getChildCount() == 0) {
            StaticUIMethods.displayError(rb.getString("Message.Error.CreateBasicAccounts"));
            return;
        }

        // Choose the file to be imported
        final JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);

        if (chooser.showOpenDialog(UIApplication.getFrame()) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file.exists()) {
                new ImportMt940(file.getAbsolutePath()).execute();
            }
        }
    }

    final static class ImportMt940 extends SwingWorker<ImportBank<ImportTransaction>, Void> {
        private final String fileName;

        ImportMt940(String fileName) {
            this.fileName = fileName;
        }

        @Override
        protected ImportBank<ImportTransaction> doInBackground() throws Exception {
            final Mt940Parser parser = new Mt940Parser();
            
            try (final LineNumberReader reader = new LineNumberReader(Files.newBufferedReader(Paths.get(fileName),
                    StandardCharsets.ISO_8859_1))) {

                Mt940File parsedFile = parser.parse(reader);
                return Mt940Exporter.convert(parsedFile);
            }                                             
        }

        @Override
        protected void done() {

            try {
                ImportDialog d = new ImportDialog();

                ImportBank<ImportTransaction> bank = get();

                d.setSetting(ImportDialog.Settings.BANK, bank);
                d.setVisible(true);

                if (d.isWizardValid()) {
                    final Account account = (Account) d.getSetting(ImportDialog.Settings.ACCOUNT);

                    @SuppressWarnings("unchecked")
                    final List<ImportTransaction> transactions = (List<ImportTransaction>) d.getSetting(ImportDialog.Settings.TRANSACTIONS);

                    GenericImport.importTransactions(transactions, account);
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        importMt940();
    }
}
