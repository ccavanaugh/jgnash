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

import javafx.fxml.FXML;

import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionFactory;
import jgnash.uifx.control.TransactionNumberComboBox;
import jgnash.util.NotNull;

/**
 * Transaction Entry Controller for Credits and Debits
 *
 * @author Craig Cavanaugh
 */
public class BuyShareSlipController extends AbstractPriceQtyInvSlipController {

    @FXML
    private FeesPane feesPane;

    @FXML
    protected TransactionNumberComboBox numberComboBox;

    @FXML
    protected AttachmentPane attachmentPane;

    @FXML
    private AccountExchangePane accountExchangePane;

    @FXML
    public void initialize() {
        super.initialize();

        // Bind necessary properties to the exchange panel
        accountExchangePane.getBaseAccountProperty().bind(getAccountProperty());
        accountExchangePane.getAmountProperty().bindBidirectional(totalField.decimalProperty());
        accountExchangePane.getAmountEditable().bind(totalField.editableProperty());

        // Lazy init when account property is set
        accountProperty.addListener((observable, oldValue, newValue) -> {

        });
    }


    @Override
    public void modifyTransaction(@NotNull final Transaction transaction) {

    }

    void updateTotalField() {
        BigDecimal fee = feesPane.getDecimalProperty().get();
        BigDecimal quantity = quantityField.getDecimal();
        BigDecimal price = priceField.getDecimal();

        BigDecimal value = quantity.multiply(price);

        value = value.add(fee);

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

    void newTransaction(final Transaction t) {
        clearForm();
    }

    @Override
    public void clearForm() {
        feesPane.clearForm();

        accountExchangePane.setEnabled(true);
        accountExchangePane.setExchangedAmount(null);
        updateTotalField();
    }

    @Override
    public void handleCancelAction() {

    }

    @Override
    public void handleEnterAction() {

    }
}
