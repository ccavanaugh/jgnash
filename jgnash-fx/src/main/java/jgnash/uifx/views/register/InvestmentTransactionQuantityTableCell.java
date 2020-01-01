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

import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.text.NumericFormats;

/**
 * {@code TableCell} for rendering investment transaction quantities.
 *
 * @author Craig Cavanaugh
 */
class InvestmentTransactionQuantityTableCell extends AbstractTransactionTableCell {

    @Override
    protected void updateItem(final BigDecimal amount, final boolean empty) {
        super.updateItem(amount, empty);  // required

        if (!empty && amount != null && getTableRow() != null) {

            final Transaction transaction = getTableRow().getItem();

            if (transaction instanceof InvestmentTransaction) {

                final NumberFormat format
                        = NumericFormats.getShortCommodityFormat(((InvestmentTransaction) transaction).getSecurityNode());

                applyFormat(amount, format);
            } else {
                setText(null);
            }
        } else {
            setText(null);
        }
    }
}
