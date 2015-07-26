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
package jgnash.ui.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import jgnash.convert.imports.ofx.OfxBank;
import jgnash.convert.imports.ofx.OfxImport;
import jgnash.convert.imports.ofx.OfxTransaction;
import jgnash.convert.imports.ofx.OfxV1ToV2;
import jgnash.convert.imports.ofx.OfxV2Parser;
import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.util.builder.Action;
import jgnash.ui.wizards.imports.ImportDialog;
import jgnash.util.FileMagic;
import jgnash.util.ResourceUtils;

/**
 * Import OFX file action
 * 
 * @author Craig Cavanaugh
 *
 */
@Action("ofximport-command")
public class ImportOfxAction extends AbstractEnabledAction {

    private static final String OFX_DIR = "OfxDirectory";

    private static void importOfx() {
        final ResourceBundle rb = ResourceUtils.getBundle();

        final Preferences pref = Preferences.userNodeForPackage(ImportOfxAction.class);

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine == null || engine.getRootAccount().getChildCount() == 0) {
            StaticUIMethods.displayError(rb.getString("Message.Error.CreateBasicAccounts"));
            return;
        }

        final JFileChooser chooser = new JFileChooser(pref.get(OFX_DIR, null));
        chooser.setMultiSelectionEnabled(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Ofx Files (*.ofx,*.qfx)", "ofx", "qfx"));

        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            pref.put(OFX_DIR, chooser.getCurrentDirectory().getAbsolutePath());

            File file = chooser.getSelectedFile();

            if (file.exists()) {
                new Import(file).execute();
            }
        }
    }

    final static class Import extends SwingWorker<OfxV2Parser, Void> {

        private final File file;

        private Account match = null;

        Import(final File file) {
            this.file = file;
        }

        @Override
        protected OfxV2Parser doInBackground() throws Exception {
            Logger logger = Logger.getLogger(Import.class.getName());

            OfxV2Parser parser = new OfxV2Parser();

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
            String accountNumber = parser.getBank().accountId;

            if (accountNumber != null && !accountNumber.isEmpty()) {
                match = OfxImport.matchAccount(parser.getBank());
            }

            return parser;
        }

        @Override
        protected void done() {
            try {

                OfxV2Parser parser = get();

                ImportDialog d = new ImportDialog();

                d.setSetting(ImportDialog.Settings.BANK, parser.getBank());

                if (match != null) {
                    d.setSetting(ImportDialog.Settings.ACCOUNT, match);
                }

                d.setVisible(true);

                if (d.isWizardValid()) {
                    final Account account = (Account) d.getSetting(ImportDialog.Settings.ACCOUNT);
                    final OfxBank bank = parser.getBank();
                    @SuppressWarnings("unchecked")
                    final List<OfxTransaction> transactions = (List<OfxTransaction>) d.getSetting(ImportDialog.Settings.TRANSACTIONS);

                    // import threads in the background
                    new ImportThread(bank, account, transactions).start();
                }
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(Import.class.getName()).log(Level.SEVERE, null, ex);
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

    @Override
    public void actionPerformed(final ActionEvent e) {
        importOfx();
    }
}
