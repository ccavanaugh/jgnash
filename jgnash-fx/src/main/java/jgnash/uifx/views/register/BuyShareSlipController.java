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

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;

import jgnash.engine.AbstractInvestmentTransactionEntry;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionEntryBuyX;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionType;
import jgnash.util.NotNull;

/**
 * Transaction Entry Controller for Credits and Debits
 *
 * @author Craig Cavanaugh
 */
public class BuyShareSlipController extends AbstractPriceQtyInvSlipController {

    @FXML
    private FeePane feePane;

    @FXML
    protected AttachmentPane attachmentPane;

    @FXML
    private AccountExchangePane accountExchangePane;

    @FXML
    public void initialize() {
        super.initialize();

        // don't filter the base account for investment transactions
        accountExchangePane.filterBaseAccountProperty().set(false);

        // Bind necessary properties to the exchange panel
        accountExchangePane.baseAccountProperty().bind(accountProperty());
        accountExchangePane.amountProperty().bindBidirectional(totalField.decimalProperty());
        accountExchangePane.amountEditableProperty().bind(totalField.editableProperty());

        feePane.accountProperty().bind(accountProperty());

        accountProperty().addListener((observable, oldValue, newValue) -> {
            clearForm();
        });

        final ChangeListener<BigDecimal> changeListener = (observable, oldValue, newValue) -> updateTotalField();

        quantityField.decimalProperty().addListener(changeListener);
        priceField.decimalProperty().addListener(changeListener);
        feePane.decimalProperty().addListener(changeListener);
    }

    @Override
    public void modifyTransaction(@NotNull final Transaction transaction) {
        if (transaction.getTransactionType() != TransactionType.BUYSHARE) {
            throw new IllegalArgumentException(resources.getString("Message.Error.InvalidTransactionType"));
        }
        clearForm();

        datePicker.setDate(transaction.getDate());
        numberComboBox.setValue(transaction.getNumber());

        List<TransactionEntry> entries = transaction.getTransactionEntries();

        feePane.setTransactionEntries(((InvestmentTransaction) transaction).getInvestmentFeeEntries());

        entries.stream().filter(e -> e instanceof TransactionEntryBuyX).forEach(e -> {
            final AbstractInvestmentTransactionEntry entry = (AbstractInvestmentTransactionEntry) e;

            memoTextField.setText(e.getMemo());
            priceField.setDecimal(entry.getPrice());
            quantityField.setDecimal(entry.getQuantity());
            securityComboBox.setSecurityNode(entry.getSecurityNode());

            /* TODO by default investment account is assigned to debit account.  Should only have to look at the
             * credit side of the entry for information
             */
            if (entry.getCreditAccount().equals(accountProperty().get())) {
                accountExchangePane.setSelectedAccount(entry.getDebitAccount());
                accountExchangePane.setExchangedAmount(entry.getDebitAmount().abs());
            } else {
                accountExchangePane.setSelectedAccount(entry.getCreditAccount());
                accountExchangePane.setExchangedAmount(entry.getCreditAmount());
            }
        });

        modTrans = transaction;
        modTrans = attachmentPane.modifyTransaction(modTrans);

        setReconciledState(transaction.getReconciled(accountProperty().get()));
    }

    private void updateTotalField() {
        totalField.setDecimal(quantityField.getDecimal().multiply(priceField.getDecimal()).add(feePane.getDecimal()));
    }

    @NotNull
    @Override
    public Transaction buildTransaction() {
        final BigDecimal exchangeRate = accountExchangePane.exchangeAmountProperty().getValue();
        final List<TransactionEntry> fees = feePane.getTransactions();

        final Transaction transaction =  TransactionFactory.generateBuyXTransaction(accountExchangePane.getSelectedAccount(),
                accountProperty().get(), securityComboBox.getValue(), priceField.getDecimal(),
                quantityField.getDecimal(), exchangeRate, datePicker.getDate(), memoTextField.getText(), fees);

        transaction.setNumber(numberComboBox.getValue());

        return attachmentPane.buildTransaction(transaction);
    }

    @Override
    public void clearForm() {
        super.clearForm();

        feePane.clearForm();

        attachmentPane.clear();
        accountExchangePane.setEnabled(true);
        accountExchangePane.setExchangedAmount(null);
        accountExchangePane.setSelectedAccount(accountProperty().get());
    }
}
