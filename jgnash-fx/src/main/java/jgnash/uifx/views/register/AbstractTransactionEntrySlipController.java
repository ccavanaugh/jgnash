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
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.uifx.control.AutoCompleteTextField;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.autocomplete.AutoCompleteFactory;

/**
 * Abstract controller for spit transaction entries of various types
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractTransactionEntrySlipController {

    @FXML
    DecimalTextField amountField;

    @FXML
    AutoCompleteTextField<Transaction> memoField;

    @FXML
    AccountExchangePane accountExchangePane;

    @FXML
    CheckBox reconciledButton;

    @FXML
    AttachmentPane attachmentPane;

    final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<List<TransactionEntry>> transactionEntryListProperty = new SimpleObjectProperty<>();

    TransactionEntry oldEntry;

    @FXML
    private void initialize() {
        // Bind necessary properties to the exchange panel
        accountExchangePane.baseAccountProperty().bind(getAccountProperty());
        accountExchangePane.amountProperty().bindBidirectional(amountField.decimalProperty());
        accountExchangePane.amountEditableProperty().bind(amountField.editableProperty());

        // Enabled auto completion
        AutoCompleteFactory.setMemoModel(memoField);
    }

    abstract TransactionEntry buildTransactionEntry();

    ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    ObjectProperty<List<TransactionEntry>> getTransactionEntryListProperty() {
        return transactionEntryListProperty;
    }

    boolean hasEqualCurrencies() {
        return accountProperty.get().getCurrencyNode().equals(accountExchangePane.getSelectedAccount().getCurrencyNode());
    }

    // TODO: Form validation visual
    private boolean validateForm() {
        return !amountField.getText().equals("");
    }

    void clearForm() {
        oldEntry = null;

        memoField.setText(null);
        amountField.setDecimal(BigDecimal.ZERO);
        reconciledButton.setSelected(false);
        accountExchangePane.setExchangedAmount(null);
    }

    @FXML
    private void handleEnterAction() {
        if (validateForm()) {
            final TransactionEntry entry = buildTransactionEntry();

            if (oldEntry != null) {
                transactionEntryListProperty.get().remove(oldEntry);
            }

            transactionEntryListProperty.get().add(entry);

            clearForm();
            memoField.requestFocus();
        }
    }

    @FXML
    private void handleCancelAction() {
        clearForm();
        memoField.requestFocus();
    }
}
