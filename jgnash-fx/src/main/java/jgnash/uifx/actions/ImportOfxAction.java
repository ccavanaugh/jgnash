/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javafx.concurrent.Task;
import javafx.stage.FileChooser;

import jgnash.convert.imports.ofx.OfxBank;
import jgnash.convert.imports.ofx.OfxImport;
import jgnash.convert.imports.ofx.OfxTransaction;
import jgnash.convert.imports.ofx.OfxV1ToV2;
import jgnash.convert.imports.ofx.OfxV2Parser;
import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.control.wizard.WizardDialogController;
import jgnash.uifx.views.main.MainApplication;
import jgnash.uifx.wizard.imports.ImportWizard;
import jgnash.util.FileMagic;
import jgnash.util.ResourceUtils;

/**
 * Utility class to import an OFX file
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

        final File file = fileChooser.showOpenDialog(MainApplication.getInstance().getPrimaryStage());

        if (file != null) {
            Preferences pref = Preferences.userNodeForPackage(ImportOfxAction.class);
            pref.put(LAST_DIR, file.getParentFile().getAbsolutePath());

            new Thread(new ImportTask(file)).start();
        }
    }

    private static FileChooser configureFileChooser() {
        final Preferences pref = Preferences.userNodeForPackage(ImportOfxAction.class);
        final FileChooser fileChooser = new FileChooser();

        fileChooser.setInitialDirectory(new File(pref.get(LAST_DIR, System.getProperty("user.home"))));

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("OFX Files (*.ofx,*.qfx)", "*.ofx", "*.qfx")
        );

        return fileChooser;
    }

    private static class ImportTask extends Task<OfxV2Parser> {

        private final File file;

        private Account match = null;

        ImportTask(final File file) {
            this.file = file;

            setOnSucceeded(event -> onSuccess());
        }

        @Override
        protected OfxV2Parser call() throws Exception {
            final Logger logger = Logger.getLogger(ImportOfxAction.class.getName());

            final OfxV2Parser parser = new OfxV2Parser();

            if (FileMagic.isOfxV1(file)) {
                logger.info("Parsing OFX Version 1 file");

                String encoding = FileMagic.getOfxV1Encoding(file);
                parser.parse(OfxV1ToV2.convertToXML(file), encoding);
            } else if (FileMagic.isOfxV2(file)) {
                logger.info("Parsing OFX Version 2 file");
                parser.parse(file);
            } else {
                logger.info("Unknown OFX Version");
            }

            if (parser.getBank() == null) {
                throw new Exception("Bank import failed");
            }

            /* Preset the best match for the downloaded account */
            final String accountNumber = parser.getBank().accountId;

            if (accountNumber != null && !accountNumber.isEmpty()) {
                match = OfxImport.matchAccount(parser.getBank());
            }

            return parser;
        }

        private void onSuccess() {
            final OfxV2Parser parser = getValue();

            ImportWizard importWizard = new ImportWizard();

            WizardDialogController<ImportWizard.Settings> wizardDialogController
                    = importWizard.wizardControllerProperty().get();

            wizardDialogController.setSetting(ImportWizard.Settings.BANK, parser.getBank());

            if (match != null) {
                wizardDialogController.setSetting(ImportWizard.Settings.ACCOUNT, match);
            }

            importWizard.showAndWait();

            if (wizardDialogController.validProperty().get()) {
                final Account account = (Account) wizardDialogController.getSetting(ImportWizard.Settings.ACCOUNT);
                final OfxBank bank = parser.getBank();

                @SuppressWarnings("unchecked")
                final List<OfxTransaction> transactions = (List<OfxTransaction>) wizardDialogController.getSetting(ImportWizard.Settings.TRANSACTIONS);

                // import threads in the background
                new ImportThread(bank, account, transactions).start();
            }
        }
    }

    private static class ImportThread extends Thread {

        private final OfxBank bank;
        private final Account account;
        private final List<OfxTransaction> transactions;

        public ImportThread(final OfxBank bank, final Account account, final List<OfxTransaction> transactions) {
            this.bank = bank;
            this.account = account;
            this.transactions = transactions;
        }

        @Override
        public void run() {
            String accountNumber = bank.accountId;

                /* set the account number if not a match */
            if (accountNumber != null && !accountNumber.equals(account.getAccountNumber())) {
                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                Objects.requireNonNull(engine);

                engine.setAccountNumber(account, accountNumber);
            }

                /* Import the transactions */
            OfxImport.importTransactions(transactions, account);
        }
    }
}
