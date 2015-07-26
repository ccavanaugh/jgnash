/*
 * jGnash, account personal finance application
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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.uifx.control;

import java.math.BigDecimal;
import java.text.NumberFormat;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TableCell;

/**
 * {@code TableCell} for {@code BigDecimals}
 *
 * @author Craig Cavanaugh
 */
public class BigDecimalTableCell<S> extends TableCell<S, BigDecimal> {

    private final SimpleObjectProperty<NumberFormat> numberFormatProperty = new SimpleObjectProperty<>();

    public BigDecimalTableCell(final ObjectProperty<NumberFormat> numberFormatProperty) {
        setStyle("-fx-alignment: center-right;");  // Right align
        numberFormatProperty().bind(numberFormatProperty);
    }

    public BigDecimalTableCell(final NumberFormat numberFormat) {
        setStyle("-fx-alignment: center-right;");  // Right align
        numberFormatProperty().setValue(numberFormat);
    }

    @Override
    protected void updateItem(final BigDecimal amount, final boolean empty) {
        super.updateItem(amount, empty);  // required

        if (!empty && amount != null) {
            setText(numberFormatProperty.get().format(amount));
        } else {
            setText(null);
        }
    }

    public SimpleObjectProperty<NumberFormat> numberFormatProperty() {
        return numberFormatProperty;
    }
}
