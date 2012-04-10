/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
package jgnash.ui.register.table;

import java.math.BigDecimal;
import java.util.Date;

import jgnash.engine.Account;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;

/**
 * Table model for investment transactions
 * 
 * @author Craig Cavanaugh
 *
 */

public class InvestmentRegisterTableModel extends AbstractRegisterTableModel {

    private static final long serialVersionUID = -8280207838778167891L;

    private static String[] names = { rb.getString("Column.Date"), rb.getString("Column.Action"),
                    rb.getString("Column.Investment"), rb.getString("Column.Clr"), rb.getString("Column.Quantity"),
                    rb.getString("Column.Price"), rb.getString("Column.Total") };

    private static int[] PREF_COLUMN_WEIGHTS = { 0, 0, 20, 0, 0, 0, 0 };

    private static Class<?>[] clazz = { Date.class, String.class, String.class, String.class, QuantityStyle.class,
                    ShortCommodityStyle.class, ShortCommodityStyle.class };

    public InvestmentRegisterTableModel(Account account) {
        super(account, names, clazz);
        setPreferredColumnWeights(PREF_COLUMN_WEIGHTS);
    }

    @Override
    public Object getInternalValueAt(int row, int col) {
        InvestmentTransaction _t = null;
        Transaction t = getTransactionAt(row);

        if (t instanceof InvestmentTransaction) {
            _t = (InvestmentTransaction) t;
        }

        switch (col) {
            case 0:
                return t.getDate();
            case 1:
                if (_t != null) {
                    return _t.getTransactionType().toString();
                } else if (t.getAmount(account).signum() > 0) {
                    return rb.getString("Item.CashDeposit");
                }
                return rb.getString("Item.CashWithdrawal");
            case 2:
                if (_t != null) {
                    return _t.getSecurityNode().getSymbol();
                }
                return t.getMemo();
            case 3:
                return t.getReconciled(account).toString();
            case 4: // quantity
                if (_t != null) {
                    BigDecimal quantity = _t.getQuantity();

                    if (quantity.compareTo(BigDecimal.ZERO) != 0) {
                        return quantity;
                    }
                }
                return null;
            case 5: // price
                if (_t != null) {
                    BigDecimal price = _t.getPrice();

                    if (price.compareTo(BigDecimal.ZERO) != 0) {
                        return price;
                    }
                }
                return null;
            case 6: // net cash amount
                if (_t != null) {
                    return _t.getNetCashValue();
                }
                return t.getAmount(account);
            default:
                return ERROR;
        }
    }
}
