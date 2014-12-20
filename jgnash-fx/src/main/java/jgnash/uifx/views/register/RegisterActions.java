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
import jgnash.uifx.Options;
import jgnash.uifx.StaticUIMethods;
import jgnash.util.Resource;
import jgnash.util.ResourceUtils;

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
            for (final Transaction transaction : transactions) {
                if (engine.removeTransaction(transaction)) {
                    if (transaction.getAttachment() != null) {
                        if (confirmAttachmentDeletion() == ButtonType.CANCEL) {
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

        return StaticUIMethods.showConfirmationDialog(rb.getString("Title.Confirm"),
                count == 1 ? rb.getString("Message.ConfirmTransDelete") : rb.getString("Message.ConfirmMultipleTransDelete"));
    }

    private static ButtonType confirmAttachmentDeletion() {
        final ResourceBundle rb = ResourceUtils.getBundle();

        return StaticUIMethods.showConfirmationDialog(rb.getString("Title.DeleteAttachment"),
                rb.getString("Question.DeleteAttachment"));
    }
}
