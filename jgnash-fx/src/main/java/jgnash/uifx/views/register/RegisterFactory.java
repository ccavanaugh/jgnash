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

import java.util.ResourceBundle;

import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.uifx.Options;
import jgnash.util.NotNull;
import jgnash.resource.util.ResourceUtils;

/**
 * Factory class for constructing register tables and controlling global options for registers.
 *
 * @author Craig Cavanaugh
 */
public class RegisterFactory {
    private static final ResourceBundle rb = ResourceUtils.getBundle();

    private static final String COLUMN_DATE = "Column.Date";
    private static final String COLUMN_NUM = "Column.Num";
    private static final String COLUMN_PAYEE = "Column.Payee";
    private static final String COLUMN_MEMO = "Column.Memo";
    private static final String COLUMN_ACCOUNT = "Column.Account";
    private static final String COLUMN_CLR = "Column.Clr";
    private static final String COLUMN_DEPOSIT = "Column.Deposit";
    private static final String COLUMN_WITHDRAWAL = "Column.Withdrawal";
    private static final String COLUMN_BALANCE = "Column.Balance";
    private static final String COLUMN_INCREASE = "Column.Increase";
    private static final String COLUMN_DECREASE = "Column.Decrease";
    private static final String COLUMN_CHARGE = "Column.Charge";
    private static final String COLUMN_DEBIT = "Column.Debit";
    private static final String COLUMN_CREDIT = "Column.Credit";
    private static final String COLUMN_REBATE = "Column.Rebate";
    private static final String COLUMN_EXPENSE = "Column.Expense";
    private static final String COLUMN_INCOME = "Column.Income";
    private static final String COLUMN_RECEIVE = "Column.Receive";
    private static final String COLUMN_SPEND = "Column.Spend";
    private static final String COLUMN_PAYMENT = "Column.Payment";
    private static final String COLUMN_ACTION = "Column.Action";
    private static final String COLUMN_INVESTMENT = "Column.Investment";
    private static final String COLUMN_PRICE = "Column.Price";
    private static final String COLUMN_TIMESTAMP = "Column.Timestamp";
    private static final String COLUMN_TOTAL = "Column.Total";
    private static final String COLUMN_QUANTITY = "Column.Quantity";
    private static final String COLUMN_GAIN = "Column.Gain";
    private static final String COLUMN_LOSS = "Column.Loss";

    private static final String[] BANK_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_TIMESTAMP),
            rb.getString(COLUMN_NUM), rb.getString(COLUMN_PAYEE), rb.getString(COLUMN_MEMO),
            rb.getString(COLUMN_ACCOUNT), rb.getString(COLUMN_CLR), rb.getString(COLUMN_DEPOSIT),
            rb.getString(COLUMN_WITHDRAWAL), rb.getString(COLUMN_BALANCE) };

    private static final String[] GENERIC_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_TIMESTAMP), rb.getString(COLUMN_NUM),
            rb.getString(COLUMN_PAYEE), rb.getString(COLUMN_MEMO), rb.getString(COLUMN_ACCOUNT),
            rb.getString(COLUMN_CLR), rb.getString(COLUMN_INCREASE), rb.getString(COLUMN_DECREASE),
            rb.getString(COLUMN_BALANCE) };

    private static final String[] CASH_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_TIMESTAMP), rb.getString(COLUMN_NUM),
            rb.getString(COLUMN_PAYEE), rb.getString(COLUMN_MEMO), rb.getString(COLUMN_ACCOUNT),
            rb.getString(COLUMN_CLR), rb.getString(COLUMN_RECEIVE), rb.getString(COLUMN_SPEND),
            rb.getString(COLUMN_BALANCE) };

    private static final String[] EXPENSE_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_TIMESTAMP), rb.getString(COLUMN_NUM),
            rb.getString(COLUMN_PAYEE), rb.getString(COLUMN_MEMO), rb.getString(COLUMN_ACCOUNT),
            rb.getString(COLUMN_CLR), rb.getString(COLUMN_EXPENSE), rb.getString(COLUMN_REBATE),
            rb.getString(COLUMN_BALANCE) };

    private static final String[] INCOME_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_TIMESTAMP), rb.getString(COLUMN_NUM),
            rb.getString(COLUMN_PAYEE), rb.getString(COLUMN_MEMO), rb.getString(COLUMN_ACCOUNT),
            rb.getString(COLUMN_CLR), rb.getString(COLUMN_CHARGE), rb.getString(COLUMN_INCOME),
            rb.getString(COLUMN_BALANCE) };

    private static final String[] CREDIT_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_TIMESTAMP), rb.getString(COLUMN_NUM),
            rb.getString(COLUMN_PAYEE), rb.getString(COLUMN_MEMO), rb.getString(COLUMN_ACCOUNT),
            rb.getString(COLUMN_CLR), rb.getString(COLUMN_PAYMENT), rb.getString(COLUMN_CHARGE),
            rb.getString(COLUMN_BALANCE) };

    private static final String[] EQUITY_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_TIMESTAMP), rb.getString(COLUMN_NUM),
            rb.getString(COLUMN_PAYEE), rb.getString(COLUMN_MEMO), rb.getString(COLUMN_ACCOUNT),
            rb.getString(COLUMN_CLR), rb.getString(COLUMN_DECREASE), rb.getString(COLUMN_INCREASE),
            rb.getString(COLUMN_BALANCE) };

    private static final String[] LIABILITY_NAMES = EQUITY_NAMES;

    private static final String[] ACCOUNTING_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_TIMESTAMP), rb.getString(COLUMN_NUM),
            rb.getString(COLUMN_PAYEE), rb.getString(COLUMN_MEMO), rb.getString(COLUMN_ACCOUNT),
            rb.getString(COLUMN_CLR), rb.getString(COLUMN_DEBIT), rb.getString(COLUMN_CREDIT),
            rb.getString(COLUMN_BALANCE) };

    private static final String[] INVESTMENT_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_TIMESTAMP), rb.getString(COLUMN_ACTION),
            rb.getString(COLUMN_INVESTMENT), rb.getString(COLUMN_MEMO), rb.getString(COLUMN_CLR),
            rb.getString(COLUMN_QUANTITY), rb.getString(COLUMN_PRICE), rb.getString(COLUMN_TOTAL) };

    private static final String[] SPLIT_ACCOUNTING_NAMES = { rb.getString(COLUMN_ACCOUNT), rb.getString(COLUMN_CLR),
            rb.getString(COLUMN_MEMO), rb.getString(COLUMN_DEBIT), rb.getString(COLUMN_CREDIT),
            rb.getString(COLUMN_BALANCE) };

    private static final String[] SPLIT_CREDIT_NAMES = { rb.getString(COLUMN_ACCOUNT), rb.getString(COLUMN_CLR),

            rb.getString(COLUMN_MEMO), rb.getString(COLUMN_PAYMENT), rb.getString(COLUMN_CHARGE),
            rb.getString(COLUMN_BALANCE) };

    private static final String[] SPLIT_EXPENSE_NAMES = { rb.getString(COLUMN_ACCOUNT), rb.getString(COLUMN_CLR),
            rb.getString(COLUMN_MEMO), rb.getString(COLUMN_EXPENSE), rb.getString(COLUMN_REBATE),
            rb.getString(COLUMN_BALANCE) };

    private static final String[] SPLIT_INCOME_NAMES = { rb.getString(COLUMN_ACCOUNT), rb.getString(COLUMN_CLR),
            rb.getString(COLUMN_MEMO), rb.getString(COLUMN_CHARGE), rb.getString(COLUMN_INCOME),
            rb.getString(COLUMN_BALANCE) };

    private static final String[] SPLIT_CASH_NAMES = { rb.getString(COLUMN_ACCOUNT), rb.getString(COLUMN_CLR),
            rb.getString(COLUMN_MEMO), rb.getString(COLUMN_RECEIVE), rb.getString(COLUMN_SPEND),
            rb.getString(COLUMN_BALANCE) };

    private static final String[] SPLIT_EQUITY_NAMES = { rb.getString(COLUMN_ACCOUNT), rb.getString(COLUMN_CLR),
            rb.getString(COLUMN_MEMO), rb.getString(COLUMN_DECREASE), rb.getString(COLUMN_INCREASE),
            rb.getString(COLUMN_BALANCE) };

    private static final String[] SPLIT_LIABILITY_NAMES = SPLIT_EQUITY_NAMES;

    private static final String[] SPLIT_BANK_NAMES = { rb.getString(COLUMN_ACCOUNT), rb.getString(COLUMN_CLR),
            rb.getString(COLUMN_MEMO), rb.getString(COLUMN_DEPOSIT), rb.getString(COLUMN_WITHDRAWAL),
            rb.getString(COLUMN_BALANCE) };

    private static final String[] SPLIT_GAIN_LOSS_NAMES = { rb.getString(COLUMN_ACCOUNT), rb.getString(COLUMN_CLR),
            rb.getString(COLUMN_MEMO), rb.getString(COLUMN_GAIN), rb.getString(COLUMN_LOSS),
            rb.getString(COLUMN_BALANCE) };

    private static final String[] SPLIT_GENERIC_NAMES = { rb.getString(COLUMN_ACCOUNT), rb.getString(COLUMN_CLR),
            rb.getString(COLUMN_MEMO), rb.getString(COLUMN_DEPOSIT), rb.getString(COLUMN_WITHDRAWAL),
            rb.getString(COLUMN_BALANCE) };

    private RegisterFactory() {
        // Utility class
    }

    static String[] getGainLossSplitColumnName() {
        String[] names; // reference to the correct column names

        if (Options.useAccountingTermsProperty().get()) {
            names = SPLIT_ACCOUNTING_NAMES;
        } else {
            names = SPLIT_GAIN_LOSS_NAMES;
        }

        return names;
    }

    static String[] getSplitColumnNames(@NotNull final AccountType accountType) {
        String[] names; // reference to the correct column names

        if (Options.useAccountingTermsProperty().get()) {
            names = SPLIT_ACCOUNTING_NAMES;
        } else {
            if (accountType == AccountType.CREDIT) {
                names = SPLIT_CREDIT_NAMES;
            } else if (accountType == AccountType.EXPENSE) {
                names = SPLIT_EXPENSE_NAMES;
            } else if (accountType == AccountType.INCOME) {
                names = SPLIT_INCOME_NAMES;
            } else if (accountType == AccountType.CASH) {
                names = SPLIT_CASH_NAMES;
            } else if (accountType == AccountType.EQUITY) {
                names = SPLIT_EQUITY_NAMES;
            } else if (accountType == AccountType.LIABILITY) {
                names = SPLIT_LIABILITY_NAMES;
            } else if (accountType.getAccountGroup() == AccountGroup.ASSET) {
                names = SPLIT_BANK_NAMES;
            } else {    // Investment accounts
                names = SPLIT_GENERIC_NAMES;
            }
        }

        return names;
    }

    public static String[] getColumnNames(@NotNull final AccountType accountType) {
        String[] names; // reference to the correct column names

        if (Options.useAccountingTermsProperty().get()) {
            names = ACCOUNTING_NAMES;
        } else {
            if (accountType == AccountType.CREDIT) {
                names = CREDIT_NAMES;
            } else if (accountType == AccountType.EXPENSE) {
                names = EXPENSE_NAMES;
            } else if (accountType == AccountType.INCOME) {
                names = INCOME_NAMES;
            } else if (accountType == AccountType.CASH) {
                names = CASH_NAMES;
            } else if (accountType == AccountType.EQUITY) {
                names = EQUITY_NAMES;
            } else if (accountType == AccountType.LIABILITY) {
                names = LIABILITY_NAMES;
            } else if (accountType.getAccountGroup() == AccountGroup.ASSET) {
                names = BANK_NAMES;
            } else if (accountType.getAccountGroup() == AccountGroup.INVEST) {
                names = INVESTMENT_NAMES;
            } else {
                names = GENERIC_NAMES;
            }
        }

        return names;
    }

    /**
     * Generates tab names for transaction forms.
     *
     * @param accountType {@code AccountType} to generate tab names for
     * @return tab names with increase name at 0 and decrease name at 1
     */
    public static String[] getCreditDebitTabNames(final AccountType accountType) {

        if (Options.useAccountingTermsProperty().get()) {
            if (accountType.getAccountGroup() == AccountGroup.INCOME
                    || accountType.getAccountGroup() == AccountGroup.EXPENSE
                    || accountType.getAccountGroup() == AccountGroup.ASSET
                    || accountType.getAccountGroup() == AccountGroup.INVEST
                    || accountType.getAccountGroup() == AccountGroup.LIABILITY) {
                return new String[] { rb.getString(COLUMN_DEBIT), rb.getString(COLUMN_CREDIT) };
            }
            return new String[] { rb.getString(COLUMN_CREDIT), rb.getString(COLUMN_DEBIT) };
        }
        if (accountType == AccountType.CREDIT) {
            return new String[] { rb.getString(COLUMN_PAYMENT), rb.getString(COLUMN_CHARGE) };
        } else if (accountType == AccountType.EXPENSE) {
            return new String[] { rb.getString(COLUMN_EXPENSE), rb.getString(COLUMN_REBATE) };
        } else if (accountType == AccountType.INCOME) {
            return new String[] { rb.getString(COLUMN_CHARGE), rb.getString(COLUMN_INCOME) };
        } else if (accountType == AccountType.CASH) {
            return new String[] { rb.getString(COLUMN_RECEIVE), rb.getString(COLUMN_SPEND) };
        } else if (accountType == AccountType.LIABILITY) {
            return new String[] { rb.getString(COLUMN_DECREASE), rb.getString(COLUMN_INCREASE) };
        } else if (accountType == AccountType.EQUITY) {
            return new String[] { rb.getString(COLUMN_DECREASE), rb.getString(COLUMN_INCREASE) };
        } else if (accountType.getAccountGroup() == AccountGroup.ASSET) {
            return new String[] { rb.getString(COLUMN_DEPOSIT), rb.getString(COLUMN_WITHDRAWAL) };
        } else {
            return new String[] { rb.getString(COLUMN_INCREASE), rb.getString(COLUMN_DECREASE) };
        }
    }
}
