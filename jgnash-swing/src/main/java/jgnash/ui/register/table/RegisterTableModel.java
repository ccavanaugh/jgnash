/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
import java.time.LocalDate;

import jgnash.engine.Account;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.util.ResourceUtils;

/**
 * Generic register table model.
 * 
 * @author Craig Cavanaugh
 */
public class RegisterTableModel extends AbstractRegisterTableModel {

    static final String split = ResourceUtils.getString("Button.Splits");

    private static final int[] PREF_COLUMN_WEIGHTS = { 0, 0, 20, 20, 20, 0, 0, 0, 0 };

    /**
     * Default column classes
     */
    static final Class<?>[] _clazz = { LocalDate.class, String.class, String.class, String.class, String.class,
                    String.class, ShortCommodityStyle.class, ShortCommodityStyle.class, FullCommodityStyle.class };

    RegisterTableModel(Account account, String[] names, Class<?>[] clazz) {
        super(account, names, clazz);
        setPreferredColumnWeights(PREF_COLUMN_WEIGHTS);
    }

    RegisterTableModel(Account account, String[] names) {
        super(account, names, _clazz);
        setPreferredColumnWeights(PREF_COLUMN_WEIGHTS);
    }

    @Override
    protected Object getInternalValueAt(int row, int col) {
        Transaction t = getTransactionAt(row);
        BigDecimal amount = t.getAmount(account);
        int signum = amount.signum();

        switch (col) {
            case 0:
                return t.getLocalDate();
            case 1:
                return t.getNumber();
            case 2:
                return t.getPayee();
            case 3:
                return t.getMemo();
            case 4:
                if (t instanceof InvestmentTransaction) {
                    return ((InvestmentTransaction) t).getInvestmentAccount().getName();
                }

                int count = t.size();
                if (count > 1) {
                    return "[ " + count + " " + split + " ]";
                }

                Account creditAccount = t.getTransactionEntries().get(0).getCreditAccount();
                if (creditAccount != account) {
                    return creditAccount.getName();
                }
                return t.getTransactionEntries().get(0).getDebitAccount().getName();
            case 5:
                return t.getReconciled(account).toString();
            case 6:
                if (signum >= 0) {
                    return amount;
                }
                return null;
            case 7:
                if (signum < 0) {
                    return amount.abs();
                }
                return null;
            case 8:
                return getBalanceAt(row);
            default:
                return ERROR;
        }
    }
}
