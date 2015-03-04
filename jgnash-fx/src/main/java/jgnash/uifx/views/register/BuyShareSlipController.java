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

import javafx.fxml.FXML;
import jgnash.engine.AbstractInvestmentTransactionEntry;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.ReconcileManager;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionEntryBuyX;
import jgnash.engine.TransactionFactory;
import jgnash.util.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Transaction Entry Controller for Credits and Debits
 *
 * @author Craig Cavanaugh
 */
public class BuyShareSlipController extends AbstractPriceQtyInvSlipController {

    @FXML
    private FeesPane feesPane;

    @FXML
    protected AttachmentPane attachmentPane;

    @FXML
    private AccountExchangePane accountExchangePane;

    @FXML
    public void initialize() {
        super.initialize();

        // don't filter the base account for investment transactions
        accountExchangePane.getFilterBaseAccount().set(false);

        // Bind necessary properties to the exchange panel
        accountExchangePane.getBaseAccountProperty().bind(getAccountProperty());
        accountExchangePane.getAmountProperty().bindBidirectional(totalField.decimalProperty());
        accountExchangePane.getAmountEditable().bind(totalField.editableProperty());

        feesPane.getAccountProperty().bind(getAccountProperty());

        getAccountProperty().addListener((observable, oldValue, newValue) -> {
            clearForm();
        });
    }

    @Override
    public void modifyTransaction(@NotNull final Transaction transaction) {
        if (!(transaction instanceof InvestmentTransaction)) {
            throw new IllegalArgumentException("bad tranType");
        }
        clearForm();

        datePicker.setDate(transaction.getDate());

        List<TransactionEntry> entries = transaction.getTransactionEntries();

        feesPane.setTransactionEntries(((InvestmentTransaction) transaction).getInvestmentFeeEntries());

        entries.stream().filter(e -> e instanceof TransactionEntryBuyX).forEach(e -> {
            final AbstractInvestmentTransactionEntry entry = (AbstractInvestmentTransactionEntry) e;

            memoTextField.setText(e.getMemo());
            priceField.setDecimal(entry.getPrice());
            quantityField.setDecimal(entry.getQuantity());
            securityComboBox.setValue(entry.getSecurityNode());

            /* TODO by default investment account is assigned to debit account.  Should only have to look at the
             * credit side of the entry for information
             */
            if (entry.getCreditAccount().equals(getAccountProperty().get())) {
                accountExchangePane.setSelectedAccount(entry.getDebitAccount());
                accountExchangePane.setExchangedAmount(entry.getDebitAmount().abs());
            } else {
                accountExchangePane.setSelectedAccount(entry.getCreditAccount());
                accountExchangePane.setExchangedAmount(entry.getCreditAmount());
            }
        });

        updateTotalField();

        modTrans = transaction;

        reconciledButton.setSelected(transaction.getReconciled(getAccountProperty().get()) != ReconciledState.NOT_RECONCILED);
    }

    void updateTotalField() {
        final BigDecimal quantity = quantityField.getDecimal();

        BigDecimal value = quantity.multiply(priceField.getDecimal());

        value = value.add(feesPane.getDecimal());

        totalField.setDecimal(value);
    }

    @Override
    public boolean validateForm() {
        return true;
    }

    @NotNull
    @Override
    public Transaction buildTransaction() {
        final BigDecimal exchangeRate = accountExchangePane.getExchangeAmountProperty().getValue();
        final List<TransactionEntry> fees = feesPane.getTransactions();

        return TransactionFactory.generateBuyXTransaction(accountExchangePane.getSelectedAccount(),
                getAccountProperty().get(), securityComboBox.getValue(), priceField.getDecimal(),
                quantityField.getDecimal(), exchangeRate, datePicker.getDate(), memoTextField.getText(), fees);
    }

    @Override
    public void clearForm() {
        super.clearForm();

        feesPane.clearForm();

        attachmentPane.clear();
        accountExchangePane.setEnabled(true);
        accountExchangePane.setExchangedAmount(null);
        accountExchangePane.setSelectedAccount(getAccountProperty().get());
        updateTotalField();
    }

    @Override
    public void handleCancelAction() {
        clearForm();
        focusFirstComponent();
    }

    @Override
    public void handleEnterAction() {
        if (validateForm()) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

            Objects.requireNonNull(engine);

            if (modTrans == null) {
                final Transaction newTrans = buildTransaction();

                // Need to set the reconciled state
                ReconcileManager.reconcileTransaction(getAccountProperty().get(), newTrans, reconciledButton.isSelected() ? ReconciledState.CLEARED : ReconciledState.NOT_RECONCILED);

                engine.addTransaction(newTrans);
            } else {
                final Transaction newTrans = buildTransaction();

                newTrans.setDateEntered(modTrans.getDateEntered());

                /* Need to preserve the reconciled state of the opposite side
                 * if both sides are not automatically reconciled
                 */
                ReconcileManager.reconcileTransaction(getAccountProperty().get(), newTrans, reconciledButton.isSelected() ? ReconciledState.CLEARED : ReconciledState.NOT_RECONCILED);

                if (engine.isTransactionValid(newTrans)) {
                    if (engine.removeTransaction(modTrans)) {
                        engine.addTransaction(newTrans);
                    }
                }
            }
            clearForm();
            focusFirstComponent();
        }
    }
}
