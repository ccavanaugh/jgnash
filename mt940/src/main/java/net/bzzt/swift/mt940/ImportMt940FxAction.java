/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
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

import java.io.File;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.concurrent.Task;
import javafx.stage.FileChooser;

import jgnash.convert.importat.GenericImport;
import jgnash.convert.importat.ImportBank;
import jgnash.convert.importat.ImportTransaction;
import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.wizard.WizardDialogController;
import jgnash.uifx.views.main.MainView;
import jgnash.uifx.wizard.imports.ImportWizard;
import jgnash.util.FileMagic;

import net.bzzt.swift.mt940.exporter.Mt940Exporter;
import net.bzzt.swift.mt940.parser.Mt940Parser;

/**
 * Utility class to import a Mt940 file.
 *
 * @author Craig Cavanaugh
 */
class ImportMt940FxAction {

    private static final String LAST_DIR = "importDir";

    private ImportMt940FxAction() {
        // Utility class
    }

    static void showAndWait() {
        final ResourceBundle resources = ResourceUtils.getBundle();

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine == null || engine.getRootAccount().getChildCount() == 0) {
            StaticUIMethods.displayError(resources.getString("Message.Error.CreateBasicAccounts"));
            return;
        }

        final FileChooser fileChooser = configureFileChooser();
        fileChooser.setTitle(resources.getString("Title.SelFile"));
      
		final File file = fileChooser.showOpenDialog(MainView.getPrimaryStage());

        if (file != null) {
            Preferences pref = Preferences.userNodeForPackage(ImportMt940FxAction.class);
            pref.put(LAST_DIR, file.getParentFile().getAbsolutePath());

            new Thread(new ImportTask(file)).start();
        }
    }

    private static FileChooser configureFileChooser() {
        final Preferences pref = Preferences.userNodeForPackage(ImportMt940FxAction.class);
        final FileChooser fileChooser = new FileChooser();

        final File initialDirectory = new File(pref.get(LAST_DIR, System.getProperty("user.home")));

        // Protect against an IllegalArgumentException
        if (initialDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );

        return fileChooser;
    }

    private static class ImportTask extends Task<ImportBank<ImportTransaction>> {

        private final File file;

        ImportTask(final File file) {
            this.file = file;

            setOnSucceeded(event -> onSuccess());
        }

        @Override
        protected ImportBank<ImportTransaction> call() throws Exception {
            final Charset charset = FileMagic.detectCharset(file.getAbsolutePath());

            try (final LineNumberReader reader = new LineNumberReader(Files.newBufferedReader(file.toPath(), charset))) {
                final Mt940File parsedFile = Mt940Parser.parse(reader);
                return Mt940Exporter.convert(parsedFile);
            }
        }

        private void onSuccess() {
            ImportBank<ImportTransaction> bank = getValue();

            final ImportWizard importWizard = new ImportWizard();

            WizardDialogController<ImportWizard.Settings> wizardDialogController
                    = importWizard.wizardControllerProperty().get();

            wizardDialogController.setSetting(ImportWizard.Settings.BANK, bank);

            importWizard.showAndWait();

            if (wizardDialogController.validProperty().get()) {
                @SuppressWarnings("unchecked")
                final List<ImportTransaction> transactions =
                        (List<ImportTransaction>) wizardDialogController.getSetting(ImportWizard.Settings.TRANSACTIONS);

                final Account account = (Account) wizardDialogController.getSetting(ImportWizard.Settings.ACCOUNT);

                // import threads in the background
                final ImportTransactionsTask importTransactionsTask = new ImportTransactionsTask(account, transactions);

                new Thread(importTransactionsTask).start();

                StaticUIMethods.displayTaskProgress(importTransactionsTask);
            }
        }
    }

    private static class ImportTransactionsTask extends Task<Void> {

        private final Account account;
        private final List<ImportTransaction> transactions;

        ImportTransactionsTask(final Account account, final List<ImportTransaction> transactions) {
            this.account = account;
            this.transactions = transactions;
        }

        @Override
        public Void call() {
            updateMessage(ResourceUtils.getString("Message.PleaseWait"));
            updateProgress(-1, Long.MAX_VALUE);

            /* Import the transactions */
            GenericImport.importTransactions(transactions, account);

            return null;
        }
    }
}
