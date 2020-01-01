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
import java.text.NumberFormat;

import javafx.scene.control.TableCell;

import jgnash.engine.TransactionEntry;
import jgnash.uifx.skin.StyleClass;

/**
 * Table cell for Transaction amounts.
 * <p>
 * Applies a style if the amount is negative
 *
 * @author Craig Cavanaugh
 */
class TransactionEntryCommodityFormatTableCell extends TableCell<TransactionEntry, BigDecimal> {

    private final NumberFormat format;

    TransactionEntryCommodityFormatTableCell(final NumberFormat format) {
        this.format = format;

        // Right align
        setStyle("-fx-alignment: center-right;");
    }

    @Override
    protected void updateItem(final BigDecimal amount, final boolean empty) {
        super.updateItem(amount, empty);  // required

        if (!empty && amount != null) {
            setText(format.format(amount));

            // Not empty and amount is not null, but tableRow can be null... JavaFx Bug?
            if (getTableRow() != null && getTableRow().getItem() != null) {
                final boolean negative = amount.signum() < 0;

                // Set font style
                if (negative) {
                    setId(StyleClass.NORMAL_NEGATIVE_CELL_ID);
                } else {
                    setId(StyleClass.NORMAL_CELL_ID);
                }
            }

        } else {
            setText(null);
        }
    }
}
