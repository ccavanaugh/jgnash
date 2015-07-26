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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyEvent;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.ReconcileManager;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.uifx.Options;
import jgnash.uifx.control.AutoCompleteTextField;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.TransactionNumberComboBox;
import jgnash.uifx.control.autocomplete.AutoCompleteFactory;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.util.ValidationFactory;
import jgnash.util.NotNull;

/**
 * Abstract bank transaction entry slip controller
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractSlipController implements Slip {

    @InjectFXML
    private final ObjectProperty<Parent> parentProperty = new SimpleObjectProperty<>();

    @FXML
    protected DecimalTextField amountField;

    @FXML
    protected AttachmentPane attachmentPane;

    @FXML
    protected DatePickerEx datePicker;

    @FXML
    protected AutoCompleteTextField<Transaction> memoTextField;

    @FXML
    protected TransactionNumberComboBox numberComboBox;

    @FXML
    protected AutoCompleteTextField<Transaction> payeeTextField;

    @FXML
    private CheckBox reconciledButton;

    final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    /**
     * Holds a reference to a transaction being modified
     */
    Transaction modTrans = null;

    @FXML
    ResourceBundle resources;

    @FXML
    public void initialize() {

        // Needed to support tri-state capability
        reconciledButton.setAllowIndeterminate(true);

        // Number combo needs to know the account in order to determine the next transaction number
        numberComboBox.accountProperty().bind(accountProperty());

        AutoCompleteFactory.setMemoModel(memoTextField);

        accountProperty.addListener((observable, oldValue, newValue) -> {
            // Set the number of fixed decimal places for entry
            amountField.scaleProperty().set(newValue.getCurrencyNode().getScale());

            // Enabled auto completion for the payee field
            if (payeeTextField != null) {   // transfer slips do not use the payee field
                AutoCompleteFactory.setPayeeModel(payeeTextField, newValue);
            }
        });

        // If focus is lost, check and load the form with an existing transaction
        if (payeeTextField != null) {   // transfer slips do not use the payee field
            payeeTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    handlePayeeFocusChange();
                }
            });
        }

        // Install an event handler when the parent has been set via injection
        parentProperty.addListener((observable, oldValue, newValue) -> {

            newValue.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                if (JavaFXUtils.ESCAPE_KEY.match(event)) {  // clear the form if an escape key is detected
                    clearForm();
                } else if (JavaFXUtils.ENTER_KEY.match(event)) {    // handle an enter key if detected
                    if (validateForm()) {
                        Platform.runLater(AbstractSlipController.this::handleEnterAction);
                    } else {
                        Platform.runLater(() -> {
                            if (event.getSource() instanceof Node) {
                                JavaFXUtils.focusNext((Node) event.getSource());
                            }
                        });
                    }
                }
            });
        });
    }

    @Override
    @NotNull
    public CheckBox getReconcileButton() {
        return reconciledButton;
    }

    /**
     * Determines is this form can be used to modify a transaction
     *
     * @param transaction {@code Transaction} to confirm
     * @return {@code true} if the {@code Transaction} can be modified
     */
    abstract boolean canModifyTransaction(final Transaction transaction);

    @Override
    public void clearForm() {

        modTrans = null;

        amountField.setEditable(true);
        amountField.setDecimal(BigDecimal.ZERO);

        reconciledButton.setDisable(false);
        reconciledButton.setSelected(false);
        reconciledButton.setIndeterminate(false);

        if (payeeTextField != null) {    // transfer slips do not use the payee field
            payeeTextField.setEditable(true);
            payeeTextField.setText(null);
        }

        datePicker.setEditable(true);
        if (!Options.rememberLastDateProperty().get()) {
            datePicker.setValue(LocalDate.now());
        }

        memoTextField.setText(null);

        numberComboBox.setValue(null);
        numberComboBox.setDisable(false);

        attachmentPane.clear();
    }

    ObjectProperty<Account> accountProperty() {
        return accountProperty;
    }

    @FXML
    @Override
    public void handleCancelAction() {
        clearForm();
        if (payeeTextField != null) {
            payeeTextField.requestFocus();
        } else {
            memoTextField.requestFocus();
        }
    }

    @FXML
    @Override
    public void handleEnterAction() {
        if (validateForm()) {
            if (modTrans == null) { // new transaction
                Transaction newTrans = buildTransaction();

                ReconcileManager.reconcileTransaction(accountProperty.get(), newTrans, getReconciledState());

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
                ReconcileManager.reconcileTransaction(accountProperty.get(), newTrans, getReconciledState());

                newTrans = attachmentPane.buildTransaction(newTrans);  // chain the transaction build

                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                if (engine != null && engine.removeTransaction(modTrans)) {
                    engine.addTransaction(newTrans);
                }
            }
            clearForm();

            if (payeeTextField != null) {
                payeeTextField.requestFocus();
            } else {
                memoTextField.requestFocus();
            }
        }
    }

    private void handlePayeeFocusChange() {
        if (modTrans == null && Options.useAutoCompleteProperty().get() && payeeTextField.getLength() > 0) {
            if (payeeTextField.autoCompleteModelObjectProperty().get() != null) {

                // The autocomplete model may return multiple solutions.  Choose the first solution that works
                final List<Transaction> transactions = new ArrayList<>(payeeTextField.autoCompleteModelObjectProperty()
                        .get().getAllExtraInfo(payeeTextField.getText()));

                Collections.reverse(transactions);  // reverse the transactions, most recent first

                for (final Transaction transaction : transactions) {
                    if (canModifyTransaction(transaction)) {
                        try {
                            modifyTransaction(modifyTransactionForAutoComplete((Transaction) transaction.clone()));
                        } catch (final CloneNotSupportedException e) {
                            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
                        }
                        modTrans = null; // clear the modTrans field  TODO: use new transaction instead?
                        break;
                    }
                }
            }
        }
    }

    /**
     * Modify a transaction before it is used to complete the panel for auto fill. The supplied transaction must be a
     * new or cloned transaction. It can't be a transaction that lives in the map. The returned transaction can be the
     * supplied reference or may be a new instance
     *
     * @param t The transaction to modify
     * @return the modified transaction
     */
    private Transaction modifyTransactionForAutoComplete(final Transaction t) {

        // tweak the transaction
        t.setNumber(null);
        t.setReconciled(ReconciledState.NOT_RECONCILED); // clear both sides

        // set the last date as required
        if (!Options.rememberLastDateProperty().get()) {
            t.setDate(new Date());
        } else {
            t.setDate(datePicker.getDate());
        }

        // preserve any transaction entries that may have been entered first
        if (amountField.getLength() > 0) {
            Transaction newTrans = buildTransaction();
            t.clearTransactionEntries();
            t.addTransactionEntries(newTrans.getTransactionEntries());
        }

        // preserve any preexisting memo field info
        if (memoTextField.getLength() > 0) {
            t.setMemo(memoTextField.getText());
        }

        // Do not copy over attachments
        t.setAttachment(null);

        return t;
    }

    @Override
    public boolean validateForm() {
        if (payeeTextField != null) {   // transfer slips do not use the payee field
            if (payeeTextField.getText() == null || payeeTextField.getText().isEmpty()) {
                ValidationFactory.showValidationWarning(payeeTextField, resources.getString("Message.Warn.EmptyPayee"));
            }
        }

        if (amountField.getDecimal().compareTo(BigDecimal.ZERO) == 0) {
            ValidationFactory.showValidationError(amountField, resources.getString("Message.Error.Value"));
            return false;
        }

        return true;
    }
}
