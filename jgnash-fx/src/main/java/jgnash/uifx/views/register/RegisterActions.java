/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javafx.concurrent.Task;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;

import jgnash.convert.exportantur.csv.CsvExport;
import jgnash.convert.exportantur.ofx.OfxExport;
import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.report.poi.Workbook;
import jgnash.report.table.AbstractReportTableModel;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.Options;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.report.AccountRegisterReport;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.views.main.MainView;
import jgnash.uifx.views.register.reconcile.ReconcileSettingsDialogController;
import jgnash.util.FileUtils;

/**
 * Register actions utility class.
 *
 * @author Craig Cavanaugh
 */
public class RegisterActions {

    private static final String EXPORT_DIR = "exportDir";

    private static final String OFX = "ofx";

    private static final String XLS = "xls";

    private RegisterActions() {
        // Utility class
    }

    static void reconcileTransactionAction(final Account account, final Transaction transaction,
                                           final ReconciledState reconciled) {
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

        // Move to a thread so the UI does not block
        Thread thread = new Thread(() -> {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            if (engine != null) {
                for (final Transaction transaction : transactions) {
                    if (engine.removeTransaction(transaction)) {
                        if (transaction.getAttachment() != null) {
                            if (confirmAttachmentDeletion().getButtonData().isCancelButton()) {
                                if (!engine.removeAttachment(transaction.getAttachment())) {
                                    StaticUIMethods.displayError(ResourceUtils.getString("Message.Error.DeleteAttachment",
                                            transaction.getAttachment()));
                                }
                            }
                        }
                    }
                }
            }
        });

        thread.start();
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

                if (transaction instanceof InvestmentTransaction) {
                    InvestmentTransactionDialog.show(account, clone, RegisterActions::addTransaction);
                } else {
                    TransactionDialog.showAndWait(account, clone, RegisterActions::addTransaction);
                }
            } catch (final CloneNotSupportedException e) {
                Logger.getLogger(RegisterActions.class.getName()).log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    private static void addTransaction(final Transaction transaction) {
        if (transaction != null) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            engine.addTransaction(transaction);
        }
    }

    public static void reconcileAccountAction(final Account account) {
        final FXMLUtils.Pair<ReconcileSettingsDialogController> pair =
                FXMLUtils.load(ReconcileSettingsDialogController.class.getResource("ReconcileSettingsDialog.fxml"),
                        ResourceUtils.getString("Title.ReconcileSettings"));

        pair.getController().accountProperty().set(account);

        pair.getStage().setResizable(false);
        pair.getStage().showAndWait();
    }

    static void exportTransactions(final Account account, final LocalDate startDate, final LocalDate endDate) {

        final ResourceBundle resources = ResourceUtils.getBundle();

        final Preferences pref = Preferences.userNodeForPackage(RegisterActions.class);
        final FileChooser fileChooser = new FileChooser();

        final File initialDirectory = new File(pref.get(EXPORT_DIR, System.getProperty("user.home")));

        // Protect against an IllegalArgumentException
        if (initialDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(resources.getString("Label.CsvFiles") + " (*.csv)", "*.csv"),
                new FileChooser.ExtensionFilter(resources.getString("Label.OfxFiles") + " (*.ofx)", "*.ofx"),
                new FileChooser.ExtensionFilter(resources.getString("Label.SpreadsheetFiles") + " (*.xls, *.xlsx)",
                        "*.xls", "*.xlsx")
        );

        final File file = fileChooser.showSaveDialog(MainView.getPrimaryStage());

        if (file != null) {
            pref.put(EXPORT_DIR, file.getParentFile().getAbsolutePath());

            final Task<Void> exportTask = new Task<>() {
                @Override
                protected Void call() {
                    updateMessage(resources.getString("Message.PleaseWait"));
                    updateProgress(-1, Long.MAX_VALUE);

                    if (OFX.equals(FileUtils.getFileExtension(file.getName()))) {
                        final OfxExport export = new OfxExport(account, startDate, endDate, file);
                        export.exportAccount();
                    } else if (FileUtils.getFileExtension(file.getName()).contains(XLS)) {
                        final AbstractReportTableModel reportTableModel = AccountRegisterReport.createReportModel(account,
                                startDate, endDate, false, "", "", true);

                        Workbook.export(reportTableModel, file);


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
                count == 1 ? rb.getString("Message.ConfirmTransDelete")
                        : rb.getString("Message.ConfirmMultipleTransDelete"));
    }

    private static ButtonType confirmAttachmentDeletion() {
        final ResourceBundle rb = ResourceUtils.getBundle();

        return StaticUIMethods.showConfirmationDialog(rb.getString("Title.DeleteAttachment"),
                rb.getString("Question.DeleteAttachment"));
    }
}
