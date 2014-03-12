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
package jgnash.uifx.control;

import java.math.BigDecimal;
import java.text.NumberFormat;

import jgnash.engine.Account;
import jgnash.text.CommodityFormat;

import javafx.scene.control.TreeTableCell;
import javafx.scene.paint.Color;

/**
 * @author Craig Cavanaugh
 */
public class CommodityFormatTreeTableCell extends TreeTableCell<Account, BigDecimal> {
    @Override
    protected void updateItem(final BigDecimal amount, final boolean empty) {
        super.updateItem(amount, empty);  // required

        if (!empty && amount != null) {
            Account account = getTreeTableRow().getTreeItem().getValue();

            NumberFormat format = CommodityFormat.getFullNumberFormat(account.getCurrencyNode());

            setText(format.format(amount));

            if (amount.signum() < 0) {
                setTextFill(Color.RED);
            } else {
                setTextFill(Color.BLACK);
            }
        } else {
            setText(null);
        }
    }
}
