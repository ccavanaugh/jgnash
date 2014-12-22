/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import jgnash.uifx.control.DecimalTextField;
import jgnash.util.NotNull;

/**
 * @author Craig Cavanaugh
 */
public class ExchangeRatePopOverController implements Initializable {

    @FXML
    private DecimalTextField exchangeRateField;

    @FXML
    private Label exchangeLabel;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

    }

    void setExchangeText(final String exchangeText) {
        exchangeLabel.setText(exchangeText);
    }

    BigDecimal getExchangeRate() {
        return exchangeRateField.getDecimal();
    }

    void setExchangeRate(@NotNull BigDecimal decimal) {
        exchangeRateField.setDecimal(decimal);
    }
}
