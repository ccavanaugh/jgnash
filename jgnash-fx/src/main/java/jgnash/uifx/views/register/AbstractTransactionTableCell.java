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
import java.time.LocalDate;

import javafx.scene.control.TableCell;

import jgnash.engine.Transaction;
import jgnash.uifx.skin.StyleClass;

/**
 * Abstract {@code TableCell} for rendering numeric transaction values.
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractTransactionTableCell extends TableCell<Transaction, BigDecimal> {

    AbstractTransactionTableCell() {

        // Right align numeric values
        setStyle("-fx-alignment: center-right;");
    }

    void applyFormat(final BigDecimal amount, final NumberFormat format) {
        setText(format.format(amount));

        // Not empty and amount is not null, but tableRow can be null... JavaFx Bug?
        if (getTableRow() != null && getTableRow().getItem() != null) {
            final boolean future = getTableRow().getItem().getLocalDate().isAfter(LocalDate.now());
            final boolean negative = amount.signum() < 0;

            // Set font style
            if (future && negative) {
                setId(StyleClass.ITALIC_NEGATIVE_CELL_ID);
            } else if (future) {
                setId(StyleClass.ITALIC_CELL_ID);
            } else if (negative) {
                setId(StyleClass.NORMAL_NEGATIVE_CELL_ID);
            } else {
                setId(StyleClass.NORMAL_CELL_ID);
            }
        }
    }
}
