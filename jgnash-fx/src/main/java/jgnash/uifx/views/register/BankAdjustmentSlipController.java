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

import java.util.Objects;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.ReconcileManager;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionType;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.views.accounts.StaticAccountsMethods;
import jgnash.util.NotNull;

/**
 * Transaction Entry Controller for Credits and Debits
 *
 * @author Craig Cavanaugh
 */
public class BankAdjustmentSlipController extends AbstractSlipController {

    @FXML
    private Button convertButton;

    @NotNull
    @Override
    public Transaction buildTransaction() {
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
    boolean canModifyTransaction(final Transaction t) {
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
        final Optional<Account> accountOptional = StaticAccountsMethods.selectAccount(null, accountProperty.get());
        if (accountOptional.isPresent()) {
            final Account opp = accountOptional.get();

            final Transaction t = new Transaction();

            t.setDate(datePicker.getDate());
            t.setNumber(numberComboBox.getValue());
            t.setPayee(payeeTextField.getText());

            final TransactionEntry entry = new TransactionEntry();
            entry.setMemo(memoTextField.getText());

            if (amountField.getDecimal().signum() >= 0) {
                entry.setCreditAccount(accountProperty.get());
                entry.setDebitAccount(opp);
            } else {
                entry.setDebitAccount(accountProperty.get());
                entry.setCreditAccount(opp);
            }

            entry.setCreditAmount(amountField.getDecimal().abs());
            entry.setDebitAmount(amountField.getDecimal().abs().negate());

            t.addTransactionEntry(entry);

            ReconcileManager.reconcileTransaction(accountProperty.get(), t, reconciledButton.isSelected() ? ReconciledState.CLEARED : ReconciledState.NOT_RECONCILED);

            final Optional<Transaction> transactionOptional = TransactionDialog.showAndWait(accountProperty.get(), t);

            if (transactionOptional.isPresent()) {
                final Transaction tran = transactionOptional.get();
                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                Objects.requireNonNull(engine);

                if (engine.removeTransaction(modTrans)) {
                    engine.addTransaction(tran);
                }
                clearForm();
            }
        }
    }
}
