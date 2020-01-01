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

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import jgnash.engine.ReconcileManager;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionFactory;
import jgnash.util.NotNull;

/**
 * Transaction Entry Controller for Transfers.
 *
 * @author Craig Cavanaugh
 */
public class TransferSlipController extends AbstractSlipController {

    @FXML
    private Button enterButton;

    @FXML
    private AccountExchangePane accountExchangePane;

    @FXML
    @Override
    public void initialize() {
        super.initialize();

        validFormProperty.bind(amountField.validDecimalProperty()
                .and(Bindings.isNotNull(accountExchangePane.selectedAccountProperty())));

        // Bind necessary properties to the exchange panel
        accountExchangePane.baseAccountProperty().bind(accountProperty());
        accountExchangePane.amountProperty().bindBidirectional(amountField.decimalProperty());
        accountExchangePane.amountEditableProperty().bind(amountField.editableProperty());

        numberComboBox.setValue(resources.getString("Item.Trans"));

        enterButton.disableProperty().bind(validFormProperty.not());
    }

    @NotNull
    @Override
    public Transaction buildTransaction() {
        String number = numberComboBox.getValue();

        String payee = resources.getString("Tab.Transfer");

        Transaction transaction;

        final int signum = amountField.getDecimal().signum();

        if (signum >= 0) {
            if (hasEqualCurrencies()) {
                transaction =
                        TransactionFactory.generateDoubleEntryTransaction(accountExchangePane.getSelectedAccount(),
                        accountProperty().get(), amountField.getDecimal().abs(), datePicker.getValue(),
                                memoTextField.getText(), payee, number);
            } else {
                transaction =
                        TransactionFactory.generateDoubleEntryTransaction(accountExchangePane.getSelectedAccount(),
                        accountProperty().get(), accountExchangePane.exchangeAmountProperty().get().abs(),
                        amountField.getDecimal().abs().negate(), datePicker.getValue(), memoTextField.getText(), payee,
                        number);
            }
        } else {
            if (hasEqualCurrencies()) {
                transaction = TransactionFactory.generateDoubleEntryTransaction(accountProperty().get(),
                        accountExchangePane.getSelectedAccount(), amountField.getDecimal().abs(), datePicker.getValue(),
                        memoTextField.getText(), payee, number);
            } else {
                transaction = TransactionFactory.generateDoubleEntryTransaction(accountProperty().get(),
                        accountExchangePane.getSelectedAccount(), amountField.getDecimal().abs(),
                        accountExchangePane.exchangeAmountProperty().get().abs().negate(), datePicker.getValue(),
                        memoTextField.getText(), payee, number);
            }
        }

        transaction.setTags(tagPane.getSelectedTags());

        ReconcileManager.reconcileTransaction(accountProperty().get(), transaction, getReconciledState());

        return transaction;
    }

    private boolean hasEqualCurrencies() {
        return account.get().getCurrencyNode().equals(accountExchangePane.getSelectedAccount().getCurrencyNode());
    }

    @Override
    public void modifyTransaction(@NotNull final Transaction transaction) {
        throw new RuntimeException("Use for modification is not supported");
    }

    @Override
    boolean canModifyTransaction(final Transaction t) {
        return false;
    }
}
