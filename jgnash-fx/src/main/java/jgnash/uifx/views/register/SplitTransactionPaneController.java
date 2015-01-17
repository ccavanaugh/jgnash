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

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;

import jgnash.engine.Account;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.uifx.control.AutoCompleteTextField;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.autocomplete.AutoCompleteFactory;

/**
 * Split Transaction Entry Controller for Credits and Debits
 *
 * @author Craig Cavanaugh
 */
public class SplitTransactionPaneController implements Initializable {

    @FXML
    private DecimalTextField amountField;

    @FXML
    private AutoCompleteTextField<Transaction> memoField;

    @FXML
    private AccountExchangePane accountExchangePane;

    @FXML
    private CheckBox reconciledButton;

    @FXML
    private AttachmentPane attachmentPane;

    private final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private PanelType panelType;

    private TransactionEntry oldEntry;

    private final SimpleObjectProperty<List<TransactionEntry>> transactionEntryListProperty = new SimpleObjectProperty<>();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        // Bind necessary properties to the exchange panel
        accountExchangePane.getBaseAccountProperty().bind(getAccountProperty());
        accountExchangePane.getAmountProperty().bindBidirectional(amountField.decimalProperty());
        accountExchangePane.getAmountEditable().bind(amountField.editableProperty());

        // Enabled auto completion
        AutoCompleteFactory.setMemoModel(memoField);
    }

    ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    ObjectProperty<List<TransactionEntry>> getTransactionEntryListProperty() {
        return transactionEntryListProperty;
    }

    void setPanelType(final PanelType panelType) {
        this.panelType = panelType;
    }

    TransactionEntry buildTransactionEntry() {
        TransactionEntry entry = new TransactionEntry();
        entry.setMemo(memoField.getText());

        int signum = amountField.getDecimal().signum();

        if ((panelType == PanelType.DECREASE && signum >= 0) || (panelType == PanelType.INCREASE && signum < 0)) {
            entry.setCreditAccount(accountExchangePane.getSelectedAccount());
            entry.setDebitAccount(accountProperty.get());

            if (hasEqualCurrencies()) {
                entry.setAmount(amountField.getDecimal().abs());
            } else {
                entry.setDebitAmount(amountField.getDecimal().abs().negate());
                entry.setCreditAmount(accountExchangePane.getExchangeAmountProperty().get().abs());
            }
        } else {
            entry.setCreditAccount(accountProperty.get());
            entry.setDebitAccount(accountExchangePane.getSelectedAccount());

            if (hasEqualCurrencies()) {
                entry.setAmount(amountField.getDecimal().abs());
            } else {
                entry.setCreditAmount(amountField.getDecimal().abs());
                entry.setDebitAmount(accountExchangePane.getExchangeAmountProperty().get().abs().negate());
            }
        }

        entry.setReconciled(accountProperty.get(), reconciledButton.isSelected() ? ReconciledState.CLEARED : ReconciledState.NOT_RECONCILED);

        return entry;
    }

   private boolean hasEqualCurrencies() {
        return accountProperty.get().getCurrencyNode().equals(accountExchangePane.getSelectedAccount().getCurrencyNode());
    }

    void modifyTransactionEntry(final TransactionEntry entry) {
        oldEntry = entry;

        memoField.setText(entry.getMemo());

        if (panelType == PanelType.DECREASE) {
            accountExchangePane.setSelectedAccount(entry.getCreditAccount());
            amountField.setDecimal(entry.getDebitAmount().abs());

            accountExchangePane.setExchangedAmount(entry.getCreditAmount());
        } else {
            accountExchangePane.setSelectedAccount(entry.getDebitAccount());
            amountField.setDecimal(entry.getCreditAmount());

            accountExchangePane.setExchangedAmount(entry.getDebitAmount().abs());
        }

        reconciledButton.setSelected(entry.getReconciled(accountProperty.get()) != ReconciledState.NOT_RECONCILED);
    }

    void clearForm() {
        oldEntry = null;

        memoField.setText(null);
        amountField.setDecimal(null);
        reconciledButton.setSelected(false);
        accountExchangePane.setExchangedAmount(null);
    }

    // TODO: Form validation visual
    private boolean validateForm() {
        return !amountField.getText().equals("");
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
        }
    }

    @FXML
    private void handleCancelAction() {
        clearForm();
    }
}
