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
import java.util.Objects;
import java.util.logging.Logger;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;

import jgnash.engine.AbstractInvestmentTransactionEntry;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.ReconcileManager;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionEntrySellX;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionType;
import jgnash.util.NotNull;

/**
 * Transaction Entry Controller for Credits and Debits
 *
 * @author Craig Cavanaugh
 */
public class SellShareSlipController extends AbstractPriceQtyInvSlipController {

    @FXML
    private GainLossPane gainLossPane;

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

        gainLossPane.accountProperty().bind(accountProperty());
        feePane.accountProperty().bind(accountProperty());

        accountProperty().addListener((observable, oldValue, newValue) -> {
            clearForm();
        });

        final ChangeListener<BigDecimal> changeListener = (observable, oldValue, newValue) -> updateTotalField();

        quantityField.decimalProperty().addListener(changeListener);
        priceField.decimalProperty().addListener(changeListener);
        gainLossPane.decimalProperty().addListener(changeListener);
        feePane.decimalProperty().addListener(changeListener);
    }

    @Override
    public void modifyTransaction(@NotNull final Transaction transaction) {
        if (transaction.getTransactionType() != TransactionType.SELLSHARE) {
            throw new IllegalArgumentException("bad tranType");
        }

        clearForm();

        datePicker.setDate(transaction.getDate());

        feePane.setTransactionEntries(((InvestmentTransaction) transaction).getInvestmentFeeEntries());
        gainLossPane.setTransactionEntries(((InvestmentTransaction) transaction).getInvestmentGainLossEntries());

        transaction.getTransactionEntries().stream().filter(e -> e instanceof TransactionEntrySellX).forEach(e -> {
            final AbstractInvestmentTransactionEntry entry = (AbstractInvestmentTransactionEntry) e;

            memoTextField.setText(e.getMemo());
            priceField.setDecimal(entry.getPrice());
            quantityField.setDecimal(entry.getQuantity());
            securityComboBox.setValue(entry.getSecurityNode());

            if (entry.getCreditAccount().equals(accountProperty().get())) {
                accountExchangePane.setSelectedAccount(entry.getDebitAccount());
                accountExchangePane.setExchangedAmount(entry.getDebitAmount().abs());
            } else {
                //accountExchangePane.setSelectedAccount(entry.getCreditAccount());
                //accountExchangePane.setExchangedAmount(entry.getCreditAmount());
                Logger.getLogger(SellShareSlipController.class.getName()).warning("was not expected");
            }
        });

        modTrans = transaction;

        reconciledButton.setSelected(transaction.getReconciled(accountProperty().get()) != ReconciledState.NOT_RECONCILED);
    }

    void updateTotalField() {
        totalField.setDecimal(quantityField.getDecimal().multiply(priceField.getDecimal()).add(gainLossPane.getDecimal()));
    }

    @Override
    public boolean validateForm() {
        return true;
    }

    @NotNull
    @Override
    public Transaction buildTransaction() {
        final BigDecimal exchangeRate = accountExchangePane.exchangeAmountProperty().getValue();
        final List<TransactionEntry> gains = gainLossPane.getTransactions();
        final List<TransactionEntry> fees = feePane.getTransactions();

        return TransactionFactory.generateSellXTransaction(accountExchangePane.getSelectedAccount(),
                accountProperty().get(), securityComboBox.getValue(), priceField.getDecimal(),
                quantityField.getDecimal(), exchangeRate, datePicker.getDate(), memoTextField.getText(), fees, gains);
    }

    @Override
    public void clearForm() {
        super.clearForm();

        feePane.clearForm();
        gainLossPane.clearForm();

        attachmentPane.clear();
        accountExchangePane.setEnabled(true);
        accountExchangePane.setExchangedAmount(null);
        accountExchangePane.setSelectedAccount(accountProperty().get());
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
                ReconcileManager.reconcileTransaction(accountProperty().get(), newTrans, reconciledButton.isSelected() ? ReconciledState.CLEARED : ReconciledState.NOT_RECONCILED);

                engine.addTransaction(newTrans);
            } else {
                final Transaction newTrans = buildTransaction();

                newTrans.setDateEntered(modTrans.getDateEntered());

                /* Need to preserve the reconciled state of the opposite side
                 * if both sides are not automatically reconciled
                 */
                ReconcileManager.reconcileTransaction(accountProperty().get(), newTrans, reconciledButton.isSelected() ? ReconciledState.CLEARED : ReconciledState.NOT_RECONCILED);

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
