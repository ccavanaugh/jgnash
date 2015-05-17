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
package jgnash.uifx.control.skin;

import java.math.BigDecimal;

import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.DetailedDecimalTextField;
import jgnash.uifx.control.behavior.DetailedDecimalTextFieldBehavior;

import com.sun.javafx.scene.control.skin.ComboBoxPopupControl;

/**
 * Skin for DetailedDecimalTextField
 *
 * @author Craig Cavanaugh
 */
public class DetailedDecimalTextFieldSkin extends ComboBoxPopupControl<BigDecimal> {

    private DetailedDecimalTextField detailedDecimalTextField;
    private DecimalTextField decimalTextField;

    public DetailedDecimalTextFieldSkin(final DetailedDecimalTextField detailedDecimalTextField) {
        super(detailedDecimalTextField, new DetailedDecimalTextFieldBehavior(detailedDecimalTextField));

        this.detailedDecimalTextField = detailedDecimalTextField;
    }

    @Override
    protected Node getPopupContent() {
        return null;
    }

    public DecimalTextField getDecimalTextField() {
        return decimalTextField;
    }

    @Override
    protected TextField getEditor() {
        return null;
    }

    @Override
    protected StringConverter<BigDecimal> getConverter() {
        return null;
    }

    @Override
    public Node getDisplayNode() {
        if (decimalTextField == null) {
            decimalTextField = detailedDecimalTextField.getDecimalTextField();
            decimalTextField.editableProperty().bindBidirectional(detailedDecimalTextField.editableProperty());

            updateDisplayNode();
        }

        return decimalTextField;
    }
}
