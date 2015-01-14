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

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionType;
import jgnash.uifx.StaticUIMethods;
import jgnash.util.NotNull;

/**
 * Transaction Entry Controller for Credits and Debits
 *
 * @author Craig Cavanaugh
 */
public class AdjustTransactionPaneController extends AbstractBankTransactionPaneController {

    @FXML
    private Button convertButton;

    @Override
    protected Transaction buildTransaction() {
        return TransactionFactory.generateSingleEntryTransaction(accountProperty.get(), amountField.getDecimal(),
                datePicker.getDate(), memoTextField.getText(), payeeTextField.getText(), numberComboBox.getValue());
    }

    @Override
    public void modifyTransaction(@NotNull final Transaction transaction) {
        if (canModifyTransaction(transaction)) {
            if (transaction.areAccountsLocked()) {
                clearForm();
                StaticUIMethods.displayError(resources.getString("Message.TransactionModifyLocked"));
                return;
            }

            newTransaction(transaction); // load the form

            modTrans = transaction; // save reference to old transaction
            modTrans = attachmentPane.modifyTransaction(modTrans);

            convertButton.setDisable(false);
        }
    }

    @Override
    protected boolean canModifyTransaction(final Transaction t) {
        return t.getTransactionType() == TransactionType.SINGLENTRY;
    }

    void newTransaction(final Transaction t) {
        clearForm();

        amountField.setDecimal(t.getAmount(getAccountProperty().get()));

        memoTextField.setText(t.getMemo());
        payeeTextField.setText(t.getPayee());
        numberComboBox.setValue(t.getNumber());

        datePicker.setDate(t.getDate());
        reconciledButton.setSelected(t.getReconciled(getAccountProperty().get()) != ReconciledState.NOT_RECONCILED);
    }

    @Override
    public void clearForm() {
        super.clearForm();

        convertButton.setDisable(true);
    }

    @FXML
    private void convertAction() {
        // TODO: Implement convert dialog
    }
}
