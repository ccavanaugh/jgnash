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

import java.time.LocalDate;

import javafx.scene.control.TableCell;

import jgnash.engine.Transaction;
import jgnash.uifx.skin.StyleClass;

/**
 * Table cell for Transaction strings.
 * <p>
 * Applies a style if the transaction occurs in the future.
 *
 * @author Craig Cavanaugh
 */
class TransactionStringTableCell extends TableCell<Transaction, String> {

    @Override
    protected void updateItem(final String string, final boolean empty) {
        super.updateItem(string, empty);  // required

        if (!empty && string != null) {
            setText(string);

            if (getTableRow() != null) { // null can occur if UI is very busy... JavaFX bug?
                if (getTableRow().getItem() != null) {
                    final boolean future = getTableRow().getItem().getLocalDate()
                            .isAfter(LocalDate.now());

                    if (future) {
                        setId(StyleClass.ITALIC_CELL_ID);
                    } else {
                        setId(StyleClass.NORMAL_CELL_ID);
                    }
                }
            }
        } else {
            setText(null);
        }
    }
}
