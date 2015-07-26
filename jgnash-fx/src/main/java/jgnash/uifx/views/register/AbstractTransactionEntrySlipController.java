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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyEvent;

import jgnash.engine.Account;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.uifx.control.AutoCompleteTextField;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.autocomplete.AutoCompleteFactory;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.util.ValidationFactory;

/**
 * Abstract controller for spit transaction entries of various types
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractTransactionEntrySlipController {

    @InjectFXML
    private final ObjectProperty<Parent> parentProperty = new SimpleObjectProperty<>();

    @FXML
    DecimalTextField amountField;

    @FXML
    AutoCompleteTextField<Transaction> memoField;

    @FXML
    AccountExchangePane accountExchangePane;

    @FXML
    private CheckBox reconciledButton;

    @FXML
    AttachmentPane attachmentPane;

    @FXML
    private ResourceBundle resources;

    final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private final ObjectProperty<List<TransactionEntry>> transactionEntryListProperty = new SimpleObjectProperty<>();

    private final ObjectProperty<Comparator<TransactionEntry>> comparatorProperty = new SimpleObjectProperty<>();

    TransactionEntry oldEntry;

    @FXML
    private void initialize() {
        reconciledButton.setAllowIndeterminate(true);

        // Bind necessary properties to the exchange panel
        accountExchangePane.baseAccountProperty().bind(accountProperty());
        accountExchangePane.amountProperty().bindBidirectional(amountField.decimalProperty());
        accountExchangePane.amountEditableProperty().bind(amountField.editableProperty());

        // Enabled auto completion
        AutoCompleteFactory.setMemoModel(memoField);

        // Install an event handler when the parent has been set via injection
        parentProperty.addListener((observable, oldValue, newValue) -> {
            newValue.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                if (JavaFXUtils.ESCAPE_KEY.match(event)) {  // clear the form if an escape key is detected
                    clearForm();
                } else if (JavaFXUtils.ENTER_KEY.match(event)) {    // handle an enter key if detected
                    if (validateForm()) {
                        Platform.runLater(AbstractTransactionEntrySlipController.this::handleEnterAction);
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

    void setReconciledState(final ReconciledState reconciledState) {
        switch (reconciledState) {
            case NOT_RECONCILED:
                reconciledButton.setIndeterminate(false);
                reconciledButton.setSelected(false);
                break;
            case RECONCILED:
                reconciledButton.setIndeterminate(false);
                reconciledButton.setSelected(true);
                break;
            case CLEARED:
                reconciledButton.setIndeterminate(true);
        }
    }

    ReconciledState getReconciledState() {
        if (reconciledButton.isIndeterminate()) {
            return ReconciledState.CLEARED;
        } else if (reconciledButton.isSelected()) {
            return ReconciledState.RECONCILED;
        }
        return ReconciledState.NOT_RECONCILED;
    }

    abstract TransactionEntry buildTransactionEntry();

    ObjectProperty<Account> accountProperty() {
        return accountProperty;
    }

    ObjectProperty<List<TransactionEntry>> transactionEntryListProperty() {
        return transactionEntryListProperty;
    }

    ObjectProperty<Comparator<TransactionEntry>> comparatorProperty() {
        return comparatorProperty;
    }

    boolean hasEqualCurrencies() {
        return accountProperty.get().getCurrencyNode().equals(accountExchangePane.getSelectedAccount().getCurrencyNode());
    }

    private boolean validateForm() {
        if (amountField.getDecimal().compareTo(BigDecimal.ZERO) == 0) {
            ValidationFactory.showValidationError(amountField, resources.getString("Message.Error.Value"));
            return false;
        }

        return true;
    }

    void clearForm() {
        oldEntry = null;

        reconciledButton.setDisable(false);
        reconciledButton.setSelected(false);
        reconciledButton.setIndeterminate(false);

        memoField.setText(null);
        amountField.setDecimal(BigDecimal.ZERO);
        accountExchangePane.setExchangedAmount(null);
    }

    @FXML
    private void handleEnterAction() {
        if (validateForm()) {
            final TransactionEntry entry = buildTransactionEntry();

            final List<TransactionEntry> entries = transactionEntryListProperty.get();

            if (oldEntry != null) {
                entries.remove(oldEntry);
            }

            final int index = Collections.binarySearch(entries, entry, comparatorProperty.get());

            if (index < 0) {
                entries.add(-index - 1, entry);
            }

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
