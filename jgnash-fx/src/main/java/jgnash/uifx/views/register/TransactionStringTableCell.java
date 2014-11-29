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

import java.util.Date;

import jgnash.engine.Transaction;

import javafx.scene.control.TableCell;

/**
 * @author Craig Cavanaugh
 */
class TransactionStringTableCell extends TableCell<Transaction, String> {

    @Override
    protected void updateItem(final String string, final boolean empty) {
        super.updateItem(string, empty);  // required

        if (!empty && string != null) {
            setText(string);

            if (getTableRow().getItem() != null) {
                final boolean future = ((Transaction) getTableRow().getItem()).getDate().after(new Date());

                if (future) {
                    setId("italic-label");
                } else {
                    setId("normal-label");
                }
            }

        } else {
            setText(null);
        }
    }
}
