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

import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.util.NotNull;
import jgnash.util.ResourceUtils;

/**
 * Factory class for constructing register tables and controlling global options for registers.
 *
 * @author Craig Cavanaugh
 */
public class RegisterFactory {

    private static final String ACCOUNTING_TERMS = "accountingTerms";

    private static final String CONFIRM_ON_DELETE = "confirmDelete";

    private static final String RESTORE_LAST_TRANSACTION_TAB = "restoreTab";

    private static boolean useAccountingTerms;

    private static boolean confirmTransactionDelete;

    private static boolean restoreLastTransactionTab;

    private static ResourceBundle rb = ResourceUtils.getBundle();

    private static final String[] BANK_NAMES = { rb.getString("Column.Date"), rb.getString("Column.Num"),
            rb.getString("Column.Payee"), rb.getString("Column.Memo"), rb.getString("Column.Account"),
            rb.getString("Column.Clr"), rb.getString("Column.Deposit"), rb.getString("Column.Withdrawal"),
            rb.getString("Column.Balance") };

    private static final String[] GENERIC_NAMES = { rb.getString("Column.Date"), rb.getString("Column.Num"),
            rb.getString("Column.Payee"), rb.getString("Column.Memo"), rb.getString("Column.Account"),
            rb.getString("Column.Clr"), rb.getString("Column.Increase"), rb.getString("Column.Decrease"),
            rb.getString("Column.Balance") };

    private static final String[] CASH_NAMES = { rb.getString("Column.Date"), rb.getString("Column.Num"),
            rb.getString("Column.Payee"), rb.getString("Column.Memo"), rb.getString("Column.Account"),
            rb.getString("Column.Clr"), rb.getString("Column.Receive"), rb.getString("Column.Spend"),
            rb.getString("Column.Balance") };

    private static final String[] EXPENSE_NAMES = { rb.getString("Column.Date"), rb.getString("Column.Num"),
            rb.getString("Column.Payee"), rb.getString("Column.Memo"), rb.getString("Column.Account"),
            rb.getString("Column.Clr"), rb.getString("Column.Expense"), rb.getString("Column.Rebate"),
            rb.getString("Column.Balance") };

    private static final String[] INCOME_NAMES = { rb.getString("Column.Date"), rb.getString("Column.Num"),
            rb.getString("Column.Payee"), rb.getString("Column.Memo"), rb.getString("Column.Account"),
            rb.getString("Column.Clr"), rb.getString("Column.Charge"), rb.getString("Column.Income"),
            rb.getString("Column.Balance") };

    private static final String[] CREDIT_NAMES = { rb.getString("Column.Date"), rb.getString("Column.Num"),
            rb.getString("Column.Payee"), rb.getString("Column.Memo"), rb.getString("Column.Account"),
            rb.getString("Column.Clr"), rb.getString("Column.Payment"), rb.getString("Column.Charge"),
            rb.getString("Column.Balance") };

    private static final String[] EQUITY_NAMES = { rb.getString("Column.Date"), rb.getString("Column.Num"),
            rb.getString("Column.Payee"), rb.getString("Column.Memo"), rb.getString("Column.Account"),
            rb.getString("Column.Clr"), rb.getString("Column.Decrease"), rb.getString("Column.Increase"),
            rb.getString("Column.Balance") };

    private static final String[] LIABILITY_NAMES = EQUITY_NAMES;

    private static final String[] ACCOUNTING_NAMES = { rb.getString("Column.Date"), rb.getString("Column.Num"),
            rb.getString("Column.Payee"), rb.getString("Column.Memo"), rb.getString("Column.Account"),
            rb.getString("Column.Clr"), rb.getString("Column.Debit"), rb.getString("Column.Credit"),
            rb.getString("Column.Balance") };

    private static final String[] SPLIT_ACCOUNTING_NAMES = { rb.getString("Column.Account"), rb.getString("Column.Clr"),
            rb.getString("Column.Memo"), rb.getString("Column.Debit"), rb.getString("Column.Credit"),
            rb.getString("Column.Balance") };

    private static final String[] SPLIT_CREDIT_NAMES = { rb.getString("Column.Account"), rb.getString("Column.Clr"),
            rb.getString("Column.Memo"), rb.getString("Column.Payment"), rb.getString("Column.Charge"),
            rb.getString("Column.Balance") };

    private static final String[] SPLIT_EXPENSE_NAMES = { rb.getString("Column.Account"), rb.getString("Column.Clr"),
            rb.getString("Column.Memo"), rb.getString("Column.Expense"), rb.getString("Column.Rebate"),
            rb.getString("Column.Balance") };

    private static final String[] SPLIT_INCOME_NAMES = { rb.getString("Column.Account"), rb.getString("Column.Clr"),
            rb.getString("Column.Memo"), rb.getString("Column.Charge"), rb.getString("Column.Income"),
            rb.getString("Column.Balance") };

    private static final String[] SPLIT_CASH_NAMES = { rb.getString("Column.Account"), rb.getString("Column.Clr"),
            rb.getString("Column.Memo"), rb.getString("Column.Receive"), rb.getString("Column.Spend"),
            rb.getString("Column.Balance") };

    private static final String[] SPLIT_EQUITY_NAMES = { rb.getString("Column.Account"), rb.getString("Column.Clr"),
            rb.getString("Column.Memo"), rb.getString("Column.Decrease"), rb.getString("Column.Increase"),
            rb.getString("Column.Balance") };

    private static final String[] SPLIT_LIABILITY_NAMES = SPLIT_EQUITY_NAMES;

    private static final String[] SPLIT_BANK_NAMES = { rb.getString("Column.Account"), rb.getString("Column.Clr"),
            rb.getString("Column.Memo"), rb.getString("Column.Deposit"), rb.getString("Column.Withdrawal"),
            rb.getString("Column.Balance") };

    private static final String[] SPLIT_GAINLOSS_NAMES = { rb.getString("Column.Account"), rb.getString("Column.Clr"),
            rb.getString("Column.Memo"), rb.getString("Column.Gain"), rb.getString("Column.Loss"),
            rb.getString("Column.Balance") };

    private static final String[] SPLIT_GENERIC_NAMES = { rb.getString("Column.Account"), rb.getString("Column.Clr"),
            rb.getString("Column.Memo"), rb.getString("Column.Deposit"), rb.getString("Column.Withdrawal"),
            rb.getString("Column.Balance") };

    static {
        Preferences p = Preferences.userNodeForPackage(RegisterFactory.class);

        useAccountingTerms = p.getBoolean(ACCOUNTING_TERMS, false);
        confirmTransactionDelete = p.getBoolean(CONFIRM_ON_DELETE, true);
        restoreLastTransactionTab = p.getBoolean(RESTORE_LAST_TRANSACTION_TAB, true);
    }

    private RegisterFactory() {
        // Utility class
    }

    /**
     * Sets if confirm on transaction delete is enabled
     *
     * @param enabled true if deletion confirmation is required
     */
    public static void setConfirmTransactionDeleteEnabled(final boolean enabled) {
        confirmTransactionDelete = enabled;
        Preferences p = Preferences.userNodeForPackage(RegisterFactory.class);
        p.putBoolean(CONFIRM_ON_DELETE, confirmTransactionDelete);
    }

    /**
     * Returns the availability of sortable registers
     *
     * @return true if confirm on transaction delete is enabled, false otherwise
     */
    public static boolean isConfirmTransactionDeleteEnabled() {
        return confirmTransactionDelete;
    }

    public static void setAccountingTermsEnabled(final boolean enabled) {
        useAccountingTerms = enabled;
        Preferences p = Preferences.userNodeForPackage(RegisterFactory.class);
        p.putBoolean(ACCOUNTING_TERMS, useAccountingTerms);
    }

    public static boolean isAccountingTermsEnabled() {
        return useAccountingTerms;
    }

    public static void setRestoreLastTransactionTab(final boolean enabled) {
        restoreLastTransactionTab = enabled;
        Preferences p = Preferences.userNodeForPackage(RegisterFactory.class);
        p.putBoolean(RESTORE_LAST_TRANSACTION_TAB, restoreLastTransactionTab);
    }

    public static boolean isRestoreLastTransactionTabEnabled() {
        return restoreLastTransactionTab;
    }

    public static String[] getColumnNames(@NotNull final AccountType accountType) {
        String[] names; // reference to the correct column names

        if (useAccountingTerms) {
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
            } else {
                names = GENERIC_NAMES;
            }
        }

        return names;
    }

    /**
     * Generates tab names for transaction forms
     *
     * @param accountType {@code AccountType} to generate tab names for
     * @return tab names with increase name at 0 and decrease name at 1
     */
    public static String[] getCreditDebitTabNames(final AccountType accountType) {

        if (useAccountingTerms) {
            if (accountType.getAccountGroup() == AccountGroup.INCOME
                    || accountType.getAccountGroup() == AccountGroup.EXPENSE
                    || accountType.getAccountGroup() == AccountGroup.ASSET
                    || accountType.getAccountGroup() == AccountGroup.INVEST
                    || accountType.getAccountGroup() == AccountGroup.LIABILITY) {
                return new String[] { rb.getString("Column.Debit"), rb.getString("Column.Credit") };
            }
            return new String[] { rb.getString("Column.Credit"), rb.getString("Column.Debit") };
        }
        if (accountType == AccountType.CREDIT) {
            return new String[] { rb.getString("Column.Payment"), rb.getString("Column.Charge") };
        } else if (accountType == AccountType.EXPENSE) {
            return new String[] { rb.getString("Column.Expense"), rb.getString("Column.Rebate") };
        } else if (accountType == AccountType.INCOME) {
            return new String[] { rb.getString("Column.Charge"), rb.getString("Column.Income") };
        } else if (accountType == AccountType.CASH) {
            return new String[] { rb.getString("Column.Receive"), rb.getString("Column.Spend") };
        } else if (accountType == AccountType.LIABILITY) {
            return new String[] { rb.getString("Column.Decrease"), rb.getString("Column.Increase") };
        } else if (accountType == AccountType.EQUITY) {
            return new String[] { rb.getString("Column.Decrease"), rb.getString("Column.Increase") };
        } else if (accountType.getAccountGroup() == AccountGroup.ASSET) {
            return new String[] { rb.getString("Column.Deposit"), rb.getString("Column.Withdrawal") };
        } else {
            return new String[] { rb.getString("Column.Increase"), rb.getString("Column.Decrease") };
        }
    }
}
