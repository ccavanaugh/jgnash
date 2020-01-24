/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.uifx.Options;
import jgnash.uifx.control.AutoCompleteTextField;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.autocomplete.AutoCompleteFactory;
import jgnash.uifx.util.InjectFXML;
import jgnash.util.NotNull;

/**
 * Abstract controller for spit transaction entries of various types.
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractTransactionEntrySlipController implements BaseSlip {

    @InjectFXML
    private final ObjectProperty<Parent> parent = new SimpleObjectProperty<>();

    @FXML
    DecimalTextField amountField;

    @FXML
    AutoCompleteTextField<Transaction> memoField;

    @FXML
    AccountExchangePane accountExchangePane;

    @FXML
    private CheckBox reconciledButton;

    @FXML
    TransactionTagPane tagPane;

    @FXML
    AttachmentPane attachmentPane;

    @FXML
    private ButtonBar buttonBar;

    @FXML
    private ResourceBundle resources;

    final ObjectProperty<Account> account = new SimpleObjectProperty<>();

    private final ObjectProperty<List<TransactionEntry>> transactionEntryList = new SimpleObjectProperty<>();

    private final ObjectProperty<Comparator<TransactionEntry>> comparator = new SimpleObjectProperty<>();

    private final BooleanProperty validFormProperty = new SimpleBooleanProperty();

    TransactionEntry oldEntry;

    @FXML
    protected void initialize() {
        if (buttonBar != null) {
            buttonBar.buttonOrderProperty().bind(Options.buttonOrderProperty());
        }

        reconciledButton.setAllowIndeterminate(true);

        // Bind necessary properties to the exchange panel
        accountExchangePane.baseAccountProperty().bind(accountProperty());
        accountExchangePane.amountProperty().bindBidirectional(amountField.decimalProperty());
        accountExchangePane.amountEditableProperty().bind(amountField.editableProperty());

        // Enabled auto completion
        AutoCompleteFactory.setMemoModel(memoField);

        // Install an event handler when the parent has been set via injection
        parent.addListener((observable, oldValue, newValue) -> installKeyPressedHandler(newValue));

        validFormProperty.bind(amountField.textProperty().isNotEmpty());
    }

    @Override
    public BooleanProperty validFormProperty() {
        return validFormProperty;
    }

    @Override
    @NotNull
    public CheckBox getReconcileButton() {
        return reconciledButton;
    }

    abstract TransactionEntry buildTransactionEntry();

    ObjectProperty<Account> accountProperty() {
        return account;
    }

    ObjectProperty<List<TransactionEntry>> transactionEntryListProperty() {
        return transactionEntryList;
    }

    ObjectProperty<Comparator<TransactionEntry>> comparatorProperty() {
        return comparator;
    }

    boolean hasEqualCurrencies() {
        return account.get().getCurrencyNode().equals(accountExchangePane.getSelectedAccount().getCurrencyNode());
    }

    public void clearForm() {
        oldEntry = null;

        reconciledButton.setDisable(false);
        reconciledButton.setSelected(false);
        reconciledButton.setIndeterminate(false);

        memoField.setText(null);
        amountField.setDecimal(BigDecimal.ZERO);
        accountExchangePane.setExchangedAmount(null);

        if (tagPane != null) {
            tagPane.clearSelectedTags();
        }
    }

    @FXML
    public void handleEnterAction() {

        final TransactionEntry entry = buildTransactionEntry();

        final List<TransactionEntry> entries = transactionEntryList.get();

        if (oldEntry != null) {
            entries.remove(oldEntry);
        }

        final int index = Collections.binarySearch(entries, entry, comparator.get());

        if (index < 0) {
            entries.add(-index - 1, entry);
        }

        clearForm();
        memoField.requestFocus();
    }

    @FXML
    public void handleCancelAction() {
        clearForm();
        memoField.requestFocus();
    }
}
