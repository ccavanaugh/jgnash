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
package jgnash.uifx.views.accounts;

import java.math.BigDecimal;
import java.text.NumberFormat;

import javafx.scene.control.TreeTableCell;

import jgnash.engine.Account;
import jgnash.text.NumericFormats;
import jgnash.uifx.skin.StyleClass;

/**
 * TreeTable cell for styling positive/negative monetary values.
 *
 * @author Craig Cavanaugh
 */
class AccountCommodityFormatTreeTableCell extends TreeTableCell<Account, BigDecimal> {

    AccountCommodityFormatTreeTableCell() {
        setStyle("-fx-alignment: center-right;");  // Right align
    }

    @Override
    protected void updateItem(final BigDecimal amount, final boolean empty) {
        super.updateItem(amount, empty);  // required

        if (!empty && amount != null && getTreeTableRow().getTreeItem() != null) {
            final Account account = getTreeTableRow().getTreeItem().getValue();

            final NumberFormat format = NumericFormats.getFullCommodityFormat(account.getCurrencyNode());

            setText(format.format(amount));

            if (amount.signum() < 0) {
                setId(StyleClass.NORMAL_NEGATIVE_CELL_ID);
            } else {
                setId(StyleClass.NORMAL_CELL_ID);
            }
        } else {
            setText(null);
        }
    }
}
