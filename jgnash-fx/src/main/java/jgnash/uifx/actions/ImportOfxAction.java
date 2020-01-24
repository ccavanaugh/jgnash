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
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javafx.concurrent.Task;
import javafx.stage.FileChooser;

import jgnash.convert.importat.GenericImport;
import jgnash.convert.importat.ofx.OfxBank;
import jgnash.convert.importat.ofx.OfxImport;
import jgnash.convert.importat.ofx.OfxV2Parser;
import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.wizard.WizardDialogController;
import jgnash.uifx.views.main.MainView;
import jgnash.uifx.wizard.imports.ImportWizard;
import jgnash.resource.util.ResourceUtils;

/**
 * Utility class to import an OFX file.
 *
 * @author Craig Cavanaugh
 */
public class ImportOfxAction {

    private static final String LAST_DIR = "importDir";

    private ImportOfxAction() {
        // Utility class
    }

    public static void showAndWait() {
        final ResourceBundle resources = ResourceUtils.getBundle();

        final FileChooser fileChooser = configureFileChooser();
        fileChooser.setTitle(resources.getString("Title.SelFile"));

        final File file = fileChooser.showOpenDialog(MainView.getPrimaryStage());

        if (file != null) {
            Preferences pref = Preferences.userNodeForPackage(ImportOfxAction.class);
            pref.put(LAST_DIR, file.getParentFile().getAbsolutePath());

            new Thread(new ImportTask(file)).start();
        }
    }

    private static FileChooser configureFileChooser() {
        final Preferences pref = Preferences.userNodeForPackage(ImportOfxAction.class);
        final FileChooser fileChooser = new FileChooser();

        final File lastDirectory = new File(pref.get(LAST_DIR, System.getProperty("user.home")));

        if (lastDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(lastDirectory);
        }

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("OFX Files (*.ofx,*.qfx)", "*.ofx", "*.qfx", "*.OFX", "*.QFX"),
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );

        return fileChooser;
    }

    private static class ImportTask extends Task<OfxBank> {

        private final File file;

        private Account match = null;

        ImportTask(final File file) {
            this.file = file;

            setOnSucceeded(event -> onSuccess());
        }

        @Override
        protected OfxBank call() throws Exception {
            final OfxBank ofxBank = OfxV2Parser.parse(file.toPath());

            /* Preset the best match for the downloaded account */
            final String accountNumber = ofxBank.accountId;

            if (accountNumber != null && !accountNumber.isEmpty()) {
                match = OfxImport.matchAccount(ofxBank);
            }

            return ofxBank;
        }

        private void onSuccess() {
            final OfxBank ofxBank = getValue();

            final ImportWizard importWizard = new ImportWizard();

            WizardDialogController<ImportWizard.Settings> wizardDialogController
                    = importWizard.wizardControllerProperty().get();

            // Set the bank match first for a better work flow
            if (match != null) {
                wizardDialogController.setSetting(ImportWizard.Settings.ACCOUNT, match);
            }

            wizardDialogController.setSetting(ImportWizard.Settings.BANK, ofxBank);

            importWizard.showAndWait();

            if (wizardDialogController.validProperty().get()) {
                final Account account = (Account) wizardDialogController.getSetting(ImportWizard.Settings.ACCOUNT);

                // import threads in the background
                final ImportTransactionsTask importTransactionsTask =
                        new ImportTransactionsTask(ofxBank, account);

                new Thread(importTransactionsTask).start();

                StaticUIMethods.displayTaskProgress(importTransactionsTask);
            }
        }
    }

    private static class ImportTransactionsTask extends Task<Void> {

        private final OfxBank bank;
        private final Account account;

        ImportTransactionsTask(final OfxBank bank, final Account account) {
            this.bank = bank;
            this.account = account;
        }

        @Override
        public Void call() {
            updateMessage(ResourceUtils.getString("Message.PleaseWait"));
            updateProgress(-1, Long.MAX_VALUE);

            String accountNumber = bank.accountId;

            /* set the account number if not a match */
            if (accountNumber != null && !accountNumber.equals(account.getAccountNumber())) {
                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                Objects.requireNonNull(engine);

                engine.setAccountNumber(account, accountNumber);
            }

            // Import or update securities that were found
            if (bank.getSecurityList().size() > 0) {
                GenericImport.importSecurities(bank.getSecurityList(), account.getCurrencyNode());
            }

            // Import the transactions
            try {
                OfxImport.importTransactions(bank, account);
            } catch (final Exception e) {
                Logger.getLogger(ImportOfxAction.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            }

            return null;
        }
    }
}
