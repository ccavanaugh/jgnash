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

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.ReconcileManager;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionType;
import jgnash.uifx.Options;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.AutoCompleteTextField;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.TransactionNumberComboBox;
import jgnash.uifx.control.autocomplete.AutoCompleteFactory;
import jgnash.util.NotNull;

/**
 * Transaction Entry Controller for Credits and Debits
 *
 * @author Craig Cavanaugh
 */
public class AdjustTransactionPaneController implements TransactionEntryController, Initializable {

    @FXML
    private Button convertButton;

    @FXML
    private TextField payeeTextField;

    @FXML
    private TransactionNumberComboBox numberComboBox;

    @FXML
    private DatePickerEx datePicker;

    @FXML
    private DecimalTextField amountField;

    @FXML
    private AutoCompleteTextField<Transaction> memoTextField;

    @FXML
    private CheckBox reconciledButton;

    @FXML
    private AttachmentPane attachmentPane;

    private ResourceBundle resources;

    final private ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private Transaction modTrans = null;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.resources = resources;

        // Number combo needs to know the account in order to determine the next transaction number
        numberComboBox.getAccountProperty().bind(getAccountProperty());

        AutoCompleteFactory.setMemoModel(memoTextField);

        // Set the number of fixed decimal places for entry
        accountProperty.addListener(new ChangeListener<Account>() {
            @Override
            public void changed(final ObservableValue<? extends Account> observable, final Account oldValue, final Account newValue) {
                amountField.scaleProperty().set(newValue.getCurrencyNode().getScale());
            }
        });
    }

    ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    protected Transaction buildTransaction() {
        return TransactionFactory.generateSingleEntryTransaction(accountProperty.get(), amountField.getDecimal(), datePicker.getDate(), memoTextField.getText(), payeeTextField.getText(), numberComboBox.getValue());
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

    protected boolean canModifyTransaction(final Transaction t) {
        return t.getTransactionType() == TransactionType.SINGLENTRY;
    }

    @Override
    public boolean validateForm() {
        return amountField.getDecimal().compareTo(BigDecimal.ZERO) != 0;
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

        modTrans = null;

        amountField.setEditable(true);
        amountField.setDecimal(null);

        reconciledButton.setDisable(false);
        reconciledButton.setSelected(false);

        payeeTextField.setEditable(true);
        payeeTextField.setText(null);

        datePicker.setEditable(true);
        if (!Options.getRememberLastDate().get()) {
            datePicker.setValue(LocalDate.now());
        }

        memoTextField.setText(null);

        numberComboBox.setValue(null);
        numberComboBox.setDisable(false);

        attachmentPane.clear();

        convertButton.setDisable(true);
    }

    @FXML
    private void okAction() {
        if (validateForm()) {
            if (modTrans == null) { // new transaction
                Transaction newTrans = buildTransaction();

                ReconcileManager.reconcileTransaction(accountProperty.get(), newTrans, reconciledButton.isSelected() ? ReconciledState.CLEARED : ReconciledState.NOT_RECONCILED);

                newTrans = attachmentPane.buildTransaction(newTrans);  // chain the transaction build

                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                if (engine != null) {
                    engine.addTransaction(newTrans);
                }
            } else {
                Transaction newTrans = buildTransaction();

                newTrans.setDateEntered(modTrans.getDateEntered());

                // restore the reconciled state of the previous old transaction
                for (final Account a : modTrans.getAccounts()) {
                    if (!a.equals(accountProperty.get())) {
                        ReconcileManager.reconcileTransaction(a, newTrans, modTrans.getReconciled(a));
                    }
                }

                /*
                 * Reconcile the modified transaction for this account.
                 * This must be performed last to ensure consistent results per the ReconcileManager rules
                 */
                ReconcileManager.reconcileTransaction(accountProperty.get(), newTrans, reconciledButton.isSelected() ? ReconciledState.CLEARED : ReconciledState.NOT_RECONCILED);

                newTrans = attachmentPane.buildTransaction(newTrans);  // chain the transaction build

                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                if (engine != null && engine.removeTransaction(modTrans)) {
                    engine.addTransaction(newTrans);
                }
            }
            clearForm();
        }
    }

    @FXML
    private void cancelAction() {
        clearForm();
    }

    @FXML
    private void convertAction() {
        // TODO: Implement convert dialog
    }
}
