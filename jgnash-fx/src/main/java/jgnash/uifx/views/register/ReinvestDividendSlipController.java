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
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;

import jgnash.engine.AbstractInvestmentTransactionEntry;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionEntryReinvestDivX;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionType;
import jgnash.util.NotNull;

/**
 * Transaction Entry Controller for Reinvested dividends.
 *
 * @author Craig Cavanaugh
 */
public class ReinvestDividendSlipController extends AbstractPriceQtyInvSlipController {

    @FXML
    private GainLossPane gainLossPane;

    @FXML
    private FeePane feePane;

    @FXML
    protected AttachmentPane attachmentPane;

    @FXML
    @Override
    public void initialize() {
        super.initialize();

        gainLossPane.accountProperty().bind(accountProperty());
        feePane.accountProperty().bind(accountProperty());

        accountProperty().addListener((observable, oldValue, newValue) -> clearForm());

        final ChangeListener<BigDecimal> changeListener = (observable, oldValue, newValue) -> updateTotalField();

        quantityField.decimalProperty().addListener(changeListener);
        priceField.decimalProperty().addListener(changeListener);
        gainLossPane.decimalProperty().addListener(changeListener);
        feePane.decimalProperty().addListener(changeListener);
    }

    @Override
    public void modifyTransaction(@NotNull final Transaction transaction) {
        if (!(transaction instanceof InvestmentTransaction)
                || transaction.getTransactionType() != TransactionType.REINVESTDIV) {
            throw new IllegalArgumentException(resources.getString("Message.Error.InvalidTransactionType"));
        }

        clearForm();

        datePicker.setValue(transaction.getLocalDate());
        numberComboBox.setValue(transaction.getNumber());

        feePane.setTransactionEntries(((InvestmentTransaction) transaction).getInvestmentFeeEntries());
        gainLossPane.setTransactionEntries(((InvestmentTransaction) transaction).getInvestmentGainLossEntries());

        transaction.getTransactionEntries().stream().filter(TransactionEntryReinvestDivX.class::isInstance).forEach(e -> {
            final AbstractInvestmentTransactionEntry entry = (AbstractInvestmentTransactionEntry) e;

            memoTextField.setText(e.getMemo());
            priceField.setDecimal(entry.getPrice());
            quantityField.setDecimal(entry.getQuantity());
            securityComboBox.setSecurityNode(entry.getSecurityNode());
        });

        tagPane.setSelectedTags(transaction.getTags(TransactionEntryReinvestDivX.class));

        modTrans = transaction;
        modTrans = attachmentPane.modifyTransaction(modTrans);

        setReconciledState(transaction.getReconciled(accountProperty().get()));
    }

    private void updateTotalField() {
        totalField.setDecimal(quantityField.getDecimal().multiply(priceField.getDecimal()).subtract(feePane.getDecimal()));
    }

    @NotNull
    @Override
    public Transaction buildTransaction() {
        final List<TransactionEntry> gains = gainLossPane.getTransactions();
        final List<TransactionEntry> fees = feePane.getTransactions();

        final Transaction transaction = TransactionFactory.generateReinvestDividendXTransaction(accountProperty().get(),
                securityComboBox.getValue(), priceField.getDecimal(), quantityField.getDecimal(), datePicker.getValue(),
                memoTextField.getText(), fees, gains);

        transaction.setNumber(numberComboBox.getValue());

        transaction.setTags(TransactionEntryReinvestDivX.class, tagPane.getSelectedTags());

        return attachmentPane.buildTransaction(transaction);
    }

    @Override
    public void clearForm() {
        super.clearForm();

        feePane.clearForm();
        gainLossPane.clearForm();

        attachmentPane.clear();
    }
}
