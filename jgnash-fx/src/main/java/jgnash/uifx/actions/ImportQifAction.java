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
package jgnash.uifx.actions;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.concurrent.Task;
import javafx.stage.FileChooser;

import jgnash.convert.importat.GenericImport;
import jgnash.convert.importat.ImportTransaction;
import jgnash.convert.importat.qif.QifAccount;
import jgnash.convert.importat.qif.QifImport;
import jgnash.convert.importat.qif.QifParser;
import jgnash.convert.importat.qif.QifUtils;
import jgnash.engine.Account;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.wizard.WizardDialogController;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.views.main.MainView;
import jgnash.uifx.wizard.imports.ImportWizard;

/**
 * Utility class to import an OFX file.
 *
 * @author Craig Cavanaugh
 */
public class ImportQifAction {

    private static final String LAST_DIR = "importDir";

    private ImportQifAction() {
        // Utility class
    }

    public static void showAndWait() {
        final ResourceBundle resources = ResourceUtils.getBundle();

        final FileChooser fileChooser = configureFileChooser();
        fileChooser.setTitle(resources.getString("Title.SelFile"));

        final File file = fileChooser.showOpenDialog(MainView.getPrimaryStage());

        if (file != null) {
            Preferences pref = Preferences.userNodeForPackage(ImportQifAction.class);
            pref.put(LAST_DIR, file.getParentFile().getAbsolutePath());

            new Thread(new ImportTask(file)).start();
        }
    }

    private static FileChooser configureFileChooser() {
        final Preferences pref = Preferences.userNodeForPackage(ImportQifAction.class);
        final FileChooser fileChooser = new FileChooser();

        final File initialDirectory = new File(pref.get(LAST_DIR, System.getProperty("user.home")));

        // Protect against an IllegalArgumentException
        if (initialDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Qif Files (*.qif)", "*.qif")
        );

        return fileChooser;
    }

    private static class ImportTask extends Task<QifImport> {

        private final File file;

        ImportTask(final File file) {
            this.file = file;
            setOnSucceeded(event -> onSuccess());
        }

        @Override
        protected QifImport call() {
            if (QifUtils.isFullFile(file)) {
                JavaFXUtils.runLater(() -> StaticUIMethods.displayError("Only bank statement based QIF file are " +
                                                                                "supported at this time"));
                cancel();
                return null;
            }

            final QifImport qifImport = new QifImport();

            if (!qifImport.doPartialParse(file)) {
                JavaFXUtils.runLater(() -> StaticUIMethods.displayError(
                        ResourceUtils.getString("Message.Error.ParseTransactions")));

                cancel();
                return null;
            }

            qifImport.dumpStats();

            if (qifImport.getParser().accountList.isEmpty()) {
                JavaFXUtils.runLater(() -> StaticUIMethods.displayError(
                        ResourceUtils.getString("Message.Error.ParseTransactions")));
                cancel();
                return null;
            }

            return qifImport;
        }

        private void onSuccess() {
            final QifImport qifImport = getValue();
            final QifParser parser = qifImport.getParser();

            final ImportWizard importWizard = new ImportWizard();

            final WizardDialogController<ImportWizard.Settings> wizardDialogController
                    = importWizard.wizardControllerProperty().get();

            importWizard.dateFormatSelectionEnabled().set(true);

            final QifAccount qAccount = parser.getBank();

            wizardDialogController.setSetting(ImportWizard.Settings.BANK, qAccount);

            importWizard.showAndWait();

            if (wizardDialogController.validProperty().get()) {
                final Account account = (Account) wizardDialogController.getSetting(ImportWizard.Settings.ACCOUNT);

                @SuppressWarnings("unchecked") final List<ImportTransaction> transactions = (List<ImportTransaction>) wizardDialogController.getSetting(ImportWizard.Settings.TRANSACTIONS);

                // import threads in the background
                ImportTransactionsTask importTransactionsTask = new ImportTransactionsTask(account, transactions);

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
