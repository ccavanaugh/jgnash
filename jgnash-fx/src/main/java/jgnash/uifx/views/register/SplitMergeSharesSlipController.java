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

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;

import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionType;
import jgnash.util.NotNull;

/**
 * Transaction Entry Controller for Split and Merges shares
 *
 * @author Craig Cavanaugh
 */
public class SplitMergeSharesSlipController extends AbstractPriceQtyInvSlipController {

    @FXML
    protected AttachmentPane attachmentPane;

    private TransactionType tranType = TransactionType.SPLITSHARE;

    @FXML
    public void initialize() {
        super.initialize();

        accountProperty().addListener((observable, oldValue, newValue) -> {
            clearForm();
        });

        final ChangeListener<BigDecimal> changeListener = (observable, oldValue, newValue) -> updateTotalField();

        quantityField.decimalProperty().addListener(changeListener);
        priceField.decimalProperty().addListener(changeListener);
    }

    void setTransactionType(final TransactionType tranType) {
        this.tranType = tranType;
    }

    @Override
    public void modifyTransaction(@NotNull final Transaction transaction) {
        if (!(transaction.getTransactionType() == TransactionType.SPLITSHARE ||
                transaction.getTransactionType() == TransactionType.MERGESHARE)) {
            throw new IllegalArgumentException(resources.getString("Message.Error.InvalidTransactionType"));
        }

        clearForm();

        datePicker.setDate(transaction.getDate());
        memoTextField.setText(transaction.getMemo());
        numberComboBox.setValue(transaction.getNumber());
        priceField.setDecimal(((InvestmentTransaction)transaction).getPrice());
        quantityField.setDecimal(((InvestmentTransaction)transaction).getQuantity());
        securityComboBox.setSecurityNode(((InvestmentTransaction)transaction).getSecurityNode());

        setReconciledState(transaction.getReconciled(accountProperty().get()));

        modTrans = transaction;
        modTrans = attachmentPane.modifyTransaction(modTrans);
    }

    private void updateTotalField() {
        totalField.setDecimal(quantityField.getDecimal().multiply(priceField.getDecimal()));
    }

    @NotNull
    @Override
    public Transaction buildTransaction() {

        final Transaction transaction;

        if (tranType == TransactionType.SPLITSHARE) {
            transaction = TransactionFactory.generateSplitXTransaction(accountProperty().get(), securityComboBox.getValue(),
                    priceField.getDecimal(), quantityField.getDecimal(), datePicker.getDate(), memoTextField.getText());
        } else {
            transaction = TransactionFactory.generateMergeXTransaction(accountProperty().get(), securityComboBox.getValue(),
                    priceField.getDecimal(), quantityField.getDecimal(), datePicker.getDate(), memoTextField.getText());
        }

        transaction.setNumber(numberComboBox.getValue());

        return attachmentPane.buildTransaction(transaction);
    }

    @Override
    public void clearForm() {
        super.clearForm();

        attachmentPane.clear();
    }
}
