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
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

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

/**
 * Abstract bank transaction entry slip controller
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractSlipController implements Slip {

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
    protected CheckBox reconciledButton;

    final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    /**
     * Holds a reference to a transaction being modified
     */
    Transaction modTrans = null;

    @FXML
    ResourceBundle resources;

    @FXML
    public void initialize() {

        // Number combo needs to know the account in order to determine the next transaction number
        numberComboBox.getAccountProperty().bind(getAccountProperty());

        AutoCompleteFactory.setMemoModel(memoTextField);


        accountProperty.addListener(new ChangeListener<Account>() {
            @Override
            public void changed(final ObservableValue<? extends Account> observable, final Account oldValue, final Account newValue) {
                // Set the number of fixed decimal places for entry
                amountField.scaleProperty().set(newValue.getCurrencyNode().getScale());

                // Enabled auto completion for the payee field
                AutoCompleteFactory.setPayeeModel(payeeTextField, newValue);
            }
        });

        // If focus is lost, check and load the form with an existing transaction
        payeeTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                handlePayeeFocusChange();
            }
        });
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
    }

    ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    @FXML
    @Override
    public void handleCancelAction() {
        clearForm();
        payeeTextField.requestFocus();
    }

    @FXML
    @Override
    public void handleEnterAction() {
        if (validateForm()) {
            if (modTrans == null) { // new transaction
                Transaction newTrans = buildTransaction();

                ReconcileManager.reconcileTransaction(accountProperty.get(), newTrans,
                        reconciledButton.isSelected() ? ReconciledState.CLEARED : ReconciledState.NOT_RECONCILED);

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
                ReconcileManager.reconcileTransaction(accountProperty.get(), newTrans,
                        reconciledButton.isSelected() ? ReconciledState.CLEARED : ReconciledState.NOT_RECONCILED);

                newTrans = attachmentPane.buildTransaction(newTrans);  // chain the transaction build

                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                if (engine != null && engine.removeTransaction(modTrans)) {
                    engine.addTransaction(newTrans);
                }
            }
            clearForm();
            payeeTextField.requestFocus();
        }
    }

    void handlePayeeFocusChange() {
        if (modTrans == null && Options.getAutoCompleteEnabled().get() && payeeTextField.getLength() > 0) {
            if (payeeTextField.getAutoCompleteModelObjectProperty().get() != null) {
                final Optional<Transaction> optional= payeeTextField.getAutoCompleteModelObjectProperty().get().getExtraInfo(payeeTextField.getText());

                if (optional.isPresent()) {
                    if (canModifyTransaction(optional.get())) {
                        try {
                            modifyTransaction(modifyTransactionForAutoComplete((Transaction) optional.get().clone()));
                        } catch (final CloneNotSupportedException e) {
                            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
                        }
                        modTrans = null; // clear the modTrans field  TODO: use new transaction instead?
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
    Transaction modifyTransactionForAutoComplete(final Transaction t) {

        // tweak the transaction
        t.setNumber(null);
        t.setReconciled(ReconciledState.NOT_RECONCILED); // clear both sides

        // set the last date as required
        if (!Options.getRememberLastDate().get()) {
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

    public boolean validateForm() {
        return amountField.getDecimal().compareTo(BigDecimal.ZERO) != 0;
    }
}
