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
package jgnash.uifx.control;

import java.math.BigDecimal;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Skin;

import jgnash.uifx.control.skin.DetailedDecimalTextFieldSkin;
import jgnash.util.NotNull;

/**
 * A {@code DecimalTextField} that supports use of a popup or dialog by overriding
 * {@code show()} and {@code hide()}
 *
 * @author Craig Cavanaugh
 */
public class DetailedDecimalTextField extends ComboBoxBase<BigDecimal> {

    private DecimalTextField decimalTextField;
    private DetailedDecimalTextFieldSkin skin;

    public DetailedDecimalTextField() {
        skin =  new DetailedDecimalTextFieldSkin(this);
        decimalTextField = skin.getDecimalTextField();

        getStyleClass().add(DEFAULT_STYLE_CLASS);
        setEditable(true);
    }

    public DecimalTextField getDecimalTextField() {
        if (decimalTextField == null) {
            decimalTextField = new DecimalTextField();
        }
        return decimalTextField;
    }

    /**
     * @see DecimalTextField#decimalProperty()
     */
    public ObjectProperty<BigDecimal> decimalProperty() {
        return decimalTextField.decimalProperty();
    }

    /**
     * @see DecimalTextField#getDecimal()
     */
    public @NotNull BigDecimal getDecimal() {
        return decimalTextField.getDecimal();
    }

    /**
     * @see DecimalTextField#setDecimal(BigDecimal)
     */
    public void setDecimal(@NotNull final BigDecimal decimal) {
        decimalTextField.setDecimal(decimal);
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return skin;
    }

    private static final String DEFAULT_STYLE_CLASS = "decimal-details";
}
