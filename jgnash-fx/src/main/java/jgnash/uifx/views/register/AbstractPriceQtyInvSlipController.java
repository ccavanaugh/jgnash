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

import java.time.LocalDate;
import java.util.logging.Logger;

import javafx.fxml.FXML;

import jgnash.engine.MathConstants;
import jgnash.engine.Transaction;
import jgnash.uifx.MainApplication;
import jgnash.uifx.Options;
import jgnash.uifx.control.AutoCompleteTextField;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.SecurityComboBox;

/**
 * @author Craig Cavanaugh
 */
public abstract class AbstractPriceQtyInvSlipController extends AbstractInvSlipController {

    @FXML
    DatePickerEx datePicker;

    @FXML
    AutoCompleteTextField<Transaction> memoTextField;

    @FXML
    DecimalTextField quantityField;

    @FXML
    DecimalTextField priceField;

    @FXML
    SecurityComboBox securityComboBox;

    @FXML
    DecimalTextField totalField;

    private static final Logger logger = MainApplication.getLogger();

    @FXML
    public void initialize() {
        super.initialize();

        quantityField.scaleProperty().setValue(MathConstants.SECURITY_QUANTITY_ACCURACY);
        priceField.scaleProperty().setValue(MathConstants.SECURITY_PRICE_ACCURACY);

        totalField.setEditable(false);
        totalField.setFocusTraversable(false);

        // Lazy init when account property is set
        accountProperty.addListener((observable, oldValue, newValue) -> {
            priceField.minScaleProperty().setValue(newValue.getCurrencyNode().getScale());

            totalField.scaleProperty().setValue(newValue.getCurrencyNode().getScale());
            totalField.minScaleProperty().setValue(newValue.getCurrencyNode().getScale());
        });

        securityComboBox.accountProperty().bind(accountProperty);
    }

    @Override
    protected void focusFirstComponent() {
        memoTextField.requestFocus();
    }

    @Override
    public void clearForm() {
        modTrans = null;

        if (!Options.getRememberLastDate().get()) {
            datePicker.setValue(LocalDate.now());
        }

        memoTextField.clear();
        priceField.setDecimal(null);
        quantityField.setDecimal(null);
        reconciledButton.setSelected(false);
        totalField.setDecimal(null);
    }

    @Override
    public boolean validateForm() {
        if (securityComboBox.getValue() == null) {
            logger.warning(resources.getString("Message.Error.SecuritySelection"));
            //showValidationError(resources.getString("Message.Error.SecuritySelection"), securityComboBox);
            return false;
        }

        if (priceField.getLength() == 0) {
            logger.warning(resources.getString("Message.Error.SecurityPrice"));
            //showValidationError(resources.getString("Message.Error.SecurityPrice"), priceField);
            return false;
        }

        if (quantityField.getLength() == 0) {
            logger.warning(resources.getString("Message.Error.SecurityQuantity"));
            //showValidationError(resources.getString("Message.Error.SecurityQuantity"), quantityField);
            return false;
        }

        return true;
    }
}
