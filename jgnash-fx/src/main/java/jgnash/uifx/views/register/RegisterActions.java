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
package jgnash.uifx.views.register;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jgnash.convert.exports.csv.CsvExport;
import jgnash.convert.exports.ofx.OfxExport;
import jgnash.convert.exports.ssf.AccountExport;
import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.uifx.Options;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.views.main.MainApplication;
import jgnash.uifx.views.register.reconcile.ReconcileSettingsDialogController;
import jgnash.util.FileUtils;
import jgnash.util.ResourceUtils;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * @author Craig Cavanaugh
 */
class RegisterActions {

    private static final String EXPORT_DIR = "exportDir";

    private static final String OFX = "ofx";

    private static final String XLS = "xls";

    private RegisterActions() {
        // Utility class
    }

    static void reconcileTransactionAction(final Account account, final Transaction transaction, final ReconciledState reconciled) {
        if (transaction != null) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            if (engine != null) {
                engine.setTransactionReconciled(transaction, account, reconciled);
            }
        }
    }

    static void deleteTransactionAction(final Transaction... transactions) {
        if (Options.confirmOnTransactionDeleteProperty().get()) {
            if (confirmTransactionRemoval(transactions.length).getButtonData().isCancelButton()) {
                return;
            }
        }

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        if (engine != null) {
            for (final Transaction transaction : transactions) {
                if (engine.removeTransaction(transaction)) {
                    if (transaction.getAttachment() != null) {
                        if (confirmAttachmentDeletion().getButtonData().isCancelButton()) {
                            if (!engine.removeAttachment(transaction.getAttachment())) {
                                StaticUIMethods.displayError(ResourceUtils.getString("Message.Error.DeleteAttachment", transaction.getAttachment()));
                            }
                        }
                    }
                }
            }
        }
    }

    static void duplicateTransaction(final Account account, final List<Transaction> transactions) {
        final ResourceBundle resourceBundle = ResourceUtils.getBundle();

        final String eftNumber = resourceBundle.getString("Item.EFT");

        for (final Transaction transaction: transactions ) {

            try {
                final Transaction clone = (Transaction) transaction.clone();
                clone.setDate(LocalDate.now());

                if (!clone.getNumber().isEmpty() && !clone.getNumber().equals(eftNumber)) {
                    final String nextTransactionNumber = account.getNextTransactionNumber();    // may return an empty string
                    if (!nextTransactionNumber.isEmpty()) {
                        clone.setNumber(nextTransactionNumber);
                    }
                }

                final Optional<Transaction> optional;

                if (transaction instanceof InvestmentTransaction) {
                    optional = InvestmentTransactionDialog.showAndWait(account, clone);
                } else {
                    optional= TransactionDialog.showAndWait(account, clone);
                }

                if (optional.isPresent()) {
                    final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                    Objects.requireNonNull(engine);

                    engine.addTransaction(optional.get());
                }
            } catch (final CloneNotSupportedException e) {
                Logger.getLogger(RegisterActions.class.getName()).log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    static void reconcileAccountAction(final Account account) {
        final ObjectProperty<ReconcileSettingsDialogController> controllerObjectProperty = new SimpleObjectProperty<>();

        final URL fxmlUrl = ReconcileSettingsDialogController.class.getResource("ReconcileSettingsDialog.fxml");
        final Stage stage = FXMLUtils.loadFXML(fxmlUrl, controllerObjectProperty, ResourceUtils.getBundle());
        stage.setTitle(ResourceUtils.getString("Title.ReconcileSettings"));

        Objects.requireNonNull(controllerObjectProperty.get());

        controllerObjectProperty.get().accountProperty().setValue(account);

        stage.setResizable(false);
        stage.showAndWait();
    }

    static void exportTransactions(final Account account, final LocalDate startDate, final LocalDate endDate) {

        final ResourceBundle resources = ResourceUtils.getBundle();

        final Preferences pref = Preferences.userNodeForPackage(RegisterActions.class);
        final FileChooser fileChooser = new FileChooser();

        fileChooser.setInitialDirectory(new File(pref.get(EXPORT_DIR, System.getProperty("user.home"))));

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(resources.getString("Label.CsvFiles") + " (*.csv)", "*.csv"),
                new FileChooser.ExtensionFilter(resources.getString("Label.OfxFiles") + " (*.ofx)", "*.ofx"),
                new FileChooser.ExtensionFilter(resources.getString("Label.SpreadsheetFiles") + " (*.xls, *.xlsx)",
                        "*.xls", "*.xlsx")
        );

        final File file = fileChooser.showSaveDialog(MainApplication.getInstance().getPrimaryStage());

        if (file != null) {
            pref.put(EXPORT_DIR, file.getParentFile().getAbsolutePath());

            final Task<Void> exportTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    updateMessage(resources.getString("Message.PleaseWait"));
                    updateProgress(-1, Long.MAX_VALUE);

                    if (OFX.equals(FileUtils.getFileExtension(file.getName()))) {
                        OfxExport export = new OfxExport(account, startDate, endDate, file);
                        export.exportAccount();
                    } else if (FileUtils.getFileExtension(file.getName()).contains(XLS)) {
                        AccountExport.exportAccount(account, RegisterFactory.getColumnNames(account.getAccountType()),
                                startDate, endDate, file);
                    } else {
                        CsvExport.exportAccount(account, startDate, endDate, file);
                    }
                    return null;
                }
            };

            new Thread(exportTask).start();

            StaticUIMethods.displayTaskProgress(exportTask);
        }
    }

    private static ButtonType confirmTransactionRemoval(final int count) {
        final ResourceBundle rb = ResourceUtils.getBundle();

        return StaticUIMethods.showConfirmationDialog(rb.getString("Title.Confirm"),
                count == 1 ? rb.getString("Message.ConfirmTransDelete") : rb.getString("Message.ConfirmMultipleTransDelete"));
    }

    private static ButtonType confirmAttachmentDeletion() {
        final ResourceBundle rb = ResourceUtils.getBundle();

        return StaticUIMethods.showConfirmationDialog(rb.getString("Title.DeleteAttachment"),
                rb.getString("Question.DeleteAttachment"));
    }
}
