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
import java.time.LocalDate;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;

import jgnash.engine.MathConstants;
import jgnash.engine.Transaction;
import jgnash.uifx.Options;
import jgnash.uifx.control.AutoCompleteTextField;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.SecurityComboBox;
import jgnash.uifx.control.TransactionNumberComboBox;

/**
 * Base Investment Slip controller.
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractPriceQtyInvSlipController extends AbstractInvSlipController {

    @FXML
    AutoCompleteTextField<Transaction> memoTextField;

    @FXML
    DecimalTextField quantityField;

    @FXML
    DecimalTextField priceField;

    @FXML
    TransactionNumberComboBox numberComboBox;

    @FXML
    SecurityComboBox securityComboBox;

    @FXML
    DecimalTextField totalField;

    @FXML
    @Override
    public void initialize() {
        super.initialize();

        quantityField.scaleProperty().set(MathConstants.SECURITY_QUANTITY_ACCURACY);
        priceField.scaleProperty().set(MathConstants.SECURITY_PRICE_ACCURACY);

        totalField.setEditable(false);
        totalField.setFocusTraversable(false);

        // Lazy init when account property is set
        account.addListener((observable, oldValue, newValue) -> {
            priceField.minScaleProperty().set(newValue.getCurrencyNode().getScale());

            totalField.scaleProperty().set(newValue.getCurrencyNode().getScale());
            totalField.minScaleProperty().set(newValue.getCurrencyNode().getScale());
        });

        securityComboBox.accountProperty().bind(account);

        validFormProperty.bind(Bindings
                .isNotNull(securityComboBox.valueProperty())
                .and(totalField.textProperty().isNotEmpty())
        );
    }

    @Override
    protected void focusFirstComponent() {
        priceField.requestFocus();
    }

    @Override
    public void clearForm() {
        super.clearForm();

        if (!Options.rememberLastDateProperty().get()) {
            datePicker.setValue(LocalDate.now());
        }

        numberComboBox.setValue("");
        memoTextField.clear();
        priceField.setDecimal(BigDecimal.ZERO);
        quantityField.setDecimal(BigDecimal.ZERO);
        totalField.setDecimal(BigDecimal.ZERO);
    }
}
