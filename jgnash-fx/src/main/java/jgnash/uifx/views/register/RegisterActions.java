/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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

import java.util.ResourceBundle;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.uifx.MainApplication;
import jgnash.uifx.Options;
import jgnash.uifx.StaticUIMethods;
import jgnash.util.Resource;
import jgnash.util.ResourceUtils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/**
 * @author Craig Cavanaugh
 */
public class RegisterActions {

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
        final Resource rb = Resource.get();

        if (Options.isConfirmTransactionDeleteEnabled()) {
            if (confirmTransactionRemoval(transactions.length) == ButtonType.CANCEL) {
                return;
            }
        }

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        if (engine != null) {
            for (Transaction transaction : transactions) {
                if (engine.removeTransaction(transaction)) {
                    if (transaction.getAttachment() != null) {
                        if (confirmAttachmentDeletion() == ButtonType.YES) {
                            if (!engine.removeAttachment(transaction.getAttachment())) {
                                StaticUIMethods.displayError(rb.getString("Message.Error.DeleteAttachment", transaction.getAttachment()));
                            }
                        }
                    }
                }
            }
        }
    }

    private static ButtonType confirmTransactionRemoval(final int count) {
        final ResourceBundle rb = ResourceUtils.getBundle();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(rb.getString("Title.Confirm"));
        alert.setHeaderText(null);
        alert.setContentText(count == 1 ? rb.getString("Message.ConfirmTransDelete") : rb.getString("Message.ConfirmMultipleTransDelete"));

        alert.initOwner(MainApplication.getPrimaryStage());
        alert.getDialogPane().getScene().getStylesheets().add(MainApplication.DEFAULT_CSS);

        return alert.showAndWait().get();
    }

    private static ButtonType confirmAttachmentDeletion() {
        final ResourceBundle rb = ResourceUtils.getBundle();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(rb.getString("Title.DeleteAttachment"));
        alert.setHeaderText(null);
        alert.setContentText(rb.getString("Question.DeleteAttachment"));
        alert.initOwner(MainApplication.getPrimaryStage());

        ButtonType buttonTypeYes = new ButtonType(rb.getString("Button.Yes"), ButtonBar.ButtonData.YES);
        ButtonType buttonTypeNo = new ButtonType(rb.getString("Button.No"), ButtonBar.ButtonData.NO);

        alert.getButtonTypes().setAll(buttonTypeNo, buttonTypeYes);

        alert.getDialogPane().getScene().getStylesheets().add(MainApplication.DEFAULT_CSS);

        return alert.showAndWait().get();
    }
}
