/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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
package jgnash.ui.register;

import java.awt.Color;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.TransactionEntry;
import jgnash.ui.register.invest.InvestmentRegisterPanel;
import jgnash.ui.register.table.AbstractRegisterTableModel;
import jgnash.ui.register.table.ClippingDecorator;
import jgnash.ui.register.table.ClippingModel;
import jgnash.ui.register.table.FilterDecorator;
import jgnash.ui.register.table.FilterModel;
import jgnash.ui.register.table.InvestmentRegisterTableModel;
import jgnash.ui.register.table.RegisterTable;
import jgnash.ui.register.table.RegisterTableWithSplitEntriesModel;
import jgnash.ui.register.table.SortableTableModel;
import jgnash.ui.register.table.SortedInvestmentTableModel;
import jgnash.ui.register.table.SortedRegisterTable;
import jgnash.ui.register.table.SortedTableModel;
import jgnash.ui.register.table.SplitsRegisterTableModel;
import jgnash.util.ResourceUtils;

/**
 * Factory class for constructing register tables and controlling global options for registers.
 * 
 * @author Craig Cavanaugh
 */
public class RegisterFactory {

    private static final String ACCOUNTING_TERMS = "accountingterms";

    private static final String EVEN_COLOR = "EvenColor";

    private static final String ODD_COLOR = "OddColor";

    private static final ResourceBundle rb = ResourceUtils.getBundle();

    private static boolean sortable;

    private static boolean useAccountingTerms;

    private static boolean confirmTransactionDelete;

    private static final String SORTABLE = "sortable";

    private static final String CONFIRM_ON_DELETE = "confirmdelete";

    private static final String RESTORE_LAST_TRANSACTION_TAB = "restoreTab";

    private static final String CONCATENATE_MEMOS = "concatenateMemos";

    private static boolean concatenateMemos;

    private static Color evenBackgroundColor;

    private static Color oddBackgroundColor;

    private static boolean restoreLastTransactionTab;

    static {
        Preferences p = Preferences.userNodeForPackage(RegisterFactory.class);
        concatenateMemos = p.getBoolean(CONCATENATE_MEMOS, false);
        sortable = p.getBoolean(SORTABLE, true);
        useAccountingTerms = p.getBoolean(ACCOUNTING_TERMS, false);
        confirmTransactionDelete = p.getBoolean(CONFIRM_ON_DELETE, true);
        restoreLastTransactionTab = p.getBoolean(RESTORE_LAST_TRANSACTION_TAB, true);

        oddBackgroundColor = new Color(p.getInt(ODD_COLOR, Color.WHITE.getRGB()));
        evenBackgroundColor = new Color(p.getInt(EVEN_COLOR, 0xE1F7DF)); // 225,247,223
    }

    private static final String COLUMN_PAYEE = "Column.Payee";
    private static final String COLUMN_DATE = "Column.Date";
    private static final String COLUMN_NUM = "Column.Num";
    private static final String COLUMN_MEMO = "Column.Memo";
    private static final String COLUMN_CLR = "Column.Clr";
    private static final String COLUMN_BALANCE = "Column.Balance";
    private static final String COLUMN_ACCOUNT = "Column.Account";
    private static final String COLUMN_DEPOSIT = "Column.Deposit";
    private static final String COLUMN_WITHDRAWAL = "Column.Withdrawal";
    private static final String COLUMN_INCREASE = "Column.Increase";
    private static final String COLUMN_DECREASE = "Column.Decrease";
    private static final String COLUMN_RECEIVE = "Column.Receive";
    private static final String COLUMN_SPEND = "Column.Spend";
    private static final String COLUMN_EXPENSE = "Column.Expense";
    private static final String COLUMN_REBATE = "Column.Rebate";
    private static final String COLUMN_CHARGE = "Column.Charge";
    private static final String COLUMN_INCOME = "Column.Income";
    private static final String COLUMN_DEBIT = "Column.Debit";
    private static final String COLUMN_CREDIT = "Column.Credit";
    private static final String COLUMN_PAYMENT = "Column.Payment";

    private static final String[] BANK_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_NUM),
                    rb.getString(COLUMN_PAYEE), rb.getString(COLUMN_MEMO), rb.getString(COLUMN_ACCOUNT),
                    rb.getString(COLUMN_CLR), rb.getString(COLUMN_DEPOSIT), rb.getString(COLUMN_WITHDRAWAL),
                    rb.getString(COLUMN_BALANCE) };

    private static final String[] GENERIC_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_NUM),
                    rb.getString(COLUMN_PAYEE), rb.getString(COLUMN_MEMO), rb.getString(COLUMN_ACCOUNT),
                    rb.getString(COLUMN_CLR), rb.getString(COLUMN_INCREASE), rb.getString(COLUMN_DECREASE),
                    rb.getString(COLUMN_BALANCE) };

    private static final String[] CASH_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_NUM),
                    rb.getString(COLUMN_PAYEE), rb.getString(COLUMN_MEMO), rb.getString(COLUMN_ACCOUNT),
                    rb.getString(COLUMN_CLR), rb.getString(COLUMN_RECEIVE), rb.getString(COLUMN_SPEND),
                    rb.getString(COLUMN_BALANCE) };

    private static final String[] EXPENSE_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_NUM),
                    rb.getString(COLUMN_PAYEE), rb.getString(COLUMN_MEMO), rb.getString(COLUMN_ACCOUNT),
                    rb.getString(COLUMN_CLR), rb.getString(COLUMN_EXPENSE), rb.getString(COLUMN_REBATE),
                    rb.getString(COLUMN_BALANCE) };

    private static final String[] INCOME_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_NUM),
                    rb.getString(COLUMN_PAYEE), rb.getString(COLUMN_MEMO), rb.getString(COLUMN_ACCOUNT),
                    rb.getString(COLUMN_CLR), rb.getString(COLUMN_CHARGE), rb.getString(COLUMN_INCOME),
                    rb.getString(COLUMN_BALANCE) };

    private static final String[] CREDIT_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_NUM),
                    rb.getString(COLUMN_PAYEE), rb.getString(COLUMN_MEMO), rb.getString(COLUMN_ACCOUNT),
                    rb.getString(COLUMN_CLR), rb.getString(COLUMN_PAYMENT), rb.getString(COLUMN_CHARGE),
                    rb.getString(COLUMN_BALANCE) };

    private static final String[] EQUITY_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_NUM),
                    rb.getString(COLUMN_PAYEE), rb.getString(COLUMN_MEMO), rb.getString(COLUMN_ACCOUNT),
                    rb.getString(COLUMN_CLR), rb.getString(COLUMN_DECREASE), rb.getString(COLUMN_INCREASE),
                    rb.getString(COLUMN_BALANCE) };

    private static final String[] LIABILITY_NAMES = EQUITY_NAMES;

    private static final String[] ACCOUNTING_NAMES = { rb.getString(COLUMN_DATE), rb.getString(COLUMN_NUM),
                    rb.getString(COLUMN_PAYEE), rb.getString(COLUMN_MEMO), rb.getString(COLUMN_ACCOUNT),
                    rb.getString(COLUMN_CLR), rb.getString(COLUMN_DEBIT), rb.getString(COLUMN_CREDIT),
                    rb.getString(COLUMN_BALANCE) };

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

    private static final String[] SPLIT_GAINLOSS_NAMES = { rb.getString(COLUMN_ACCOUNT), rb.getString(COLUMN_CLR),
                    rb.getString(COLUMN_MEMO), rb.getString("Column.Gain"), rb.getString("Column.Loss"),
                    rb.getString(COLUMN_BALANCE) };

    private static final String[] SPLIT_GENERIC_NAMES = { rb.getString(COLUMN_ACCOUNT), rb.getString(COLUMN_CLR),
                    rb.getString(COLUMN_MEMO), rb.getString(COLUMN_DEPOSIT), rb.getString(COLUMN_WITHDRAWAL),
                    rb.getString(COLUMN_BALANCE) };

    private RegisterFactory() {
    }

    static void setConcatenateMemos(final boolean value) {
        Preferences p = Preferences.userNodeForPackage(RegisterFactory.class);
        p.putBoolean(CONCATENATE_MEMOS, value);

        concatenateMemos = value;
    }

    static boolean getConcatenateMemos() {
        return concatenateMemos;
    }

    public static void setOddColor(final Color color) {
        Preferences p = Preferences.userNodeForPackage(RegisterFactory.class);
        p.putInt(ODD_COLOR, color.getRGB());

        oddBackgroundColor = color;
    }

    public static Color getOddColor() {
        return oddBackgroundColor;
    }

    public static Color getEvenColor() {
        return evenBackgroundColor;
    }

    public static void setEvenColor(final Color color) {
        Preferences p = Preferences.userNodeForPackage(RegisterFactory.class);
        p.putInt(EVEN_COLOR, color.getRGB());

        evenBackgroundColor = color;
    }

    /**
     * Sets the availability of sortable registers.
     * 
     * @param enabled true if sorting it enabled
     */
    public static void setSortingEnabled(final boolean enabled) {
        sortable = enabled;
        Preferences p = Preferences.userNodeForPackage(RegisterFactory.class);
        p.putBoolean(SORTABLE, sortable);
    }

    /**
     * Returns the availability of sortable registers.
     * 
     * @return true if auto completion is enabled, false otherwise
     */
    public static boolean isSortingEnabled() {
        return sortable;
    }

    /**
     * Sets if confirm on transaction delete is enabled.
     * 
     * @param enabled true if deletion confirmation is required
     */
    public static void setConfirmTransactionDeleteEnabled(final boolean enabled) {
        confirmTransactionDelete = enabled;
        Preferences p = Preferences.userNodeForPackage(RegisterFactory.class);
        p.putBoolean(CONFIRM_ON_DELETE, confirmTransactionDelete);
    }

    /**
     * Returns the availability of sortable registers.
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

    /**
     * Creates and returns a GenericRegisterTable of the appropriate type.
     * 
     * @param account the account to create a new table for
     * @return returns a GenericRegisterTable of the appropriate type
     */
    public static RegisterTable generateTable(final Account account) {
        AbstractRegisterTableModel m = getTableModel(account);

        if (m instanceof SortableTableModel) {
            return new SortedRegisterTable(m);
        }
        return new RegisterTable(m);
    }

    private static AbstractRegisterTableModel getTableModel(final Account account) {
        return getTableModel(sortable, account, false);
    }

    public static ClippingModel getClippingTableModel(final Account account, final boolean detailSplits) {
        return new ClippingDecorator(getTableModel(false, account, detailSplits));
    }

    public static FilterModel getFilterTableModel(final Account account, final boolean detailSplits) {
        return new FilterDecorator(getTableModel(false, account, detailSplits));
    }

    public static String[] getColumnNames(final Account account) {
        String[] names; // reference to the correct column names

        if (useAccountingTerms) {
            names = ACCOUNTING_NAMES;
        } else {
            if (account.getAccountType() == AccountType.CREDIT) {
                names = CREDIT_NAMES;
            } else if (account.getAccountType() == AccountType.EXPENSE) {
                names = EXPENSE_NAMES;
            } else if (account.getAccountType() == AccountType.INCOME) {
                names = INCOME_NAMES;
            } else if (account.getAccountType() == AccountType.CASH) {
                names = CASH_NAMES;
            } else if (account.getAccountType() == AccountType.EQUITY) {
                names = EQUITY_NAMES;
            } else if (account.getAccountType() == AccountType.LIABILITY) {
                names = LIABILITY_NAMES;
            } else if (account.getAccountType().getAccountGroup() == AccountGroup.ASSET) {
                names = BANK_NAMES;
            } else {
                names = GENERIC_NAMES;
            }
        }
        return names;
    }

    private static AbstractRegisterTableModel getTableModel(final boolean sort, final Account account, final boolean detailSplits) {

        AbstractRegisterTableModel model;

        if (account.memberOf(AccountGroup.INVEST) && (account.getAccountType() != AccountType.SIMPLEINVEST)) {
            if (sort) {
                return new SortedInvestmentTableModel(account);
            }
            return new InvestmentRegisterTableModel(account);
        }

        String[] names = getColumnNames(account); // reference to the correct column names

        if (sort) {
            model = new SortedTableModel(account, names);
        } else {
            model = new RegisterTableWithSplitEntriesModel(account, names, detailSplits);
        }

        return model;
    }

    /**
     * Creates and returns a GenericRegisterTable of the appropriate type.
     * 
     * @param account the account to create a new table for
     * @param splits transaction entries to display
     * @return returns a GenericRegisterTable of the appropriate type
     */
    static RegisterTable generateSplitsTable(final Account account, final List<TransactionEntry> splits) {

        String[] names; // reference to the correct column names

        if (useAccountingTerms) {
            names = SPLIT_ACCOUNTING_NAMES;
        } else {
            if (account.getAccountType() == AccountType.CREDIT) {
                names = SPLIT_CREDIT_NAMES;
            } else if (account.getAccountType() == AccountType.EXPENSE) {
                names = SPLIT_EXPENSE_NAMES;
            } else if (account.getAccountType() == AccountType.INCOME) {
                names = SPLIT_INCOME_NAMES;
            } else if (account.getAccountType() == AccountType.CASH) {
                names = SPLIT_CASH_NAMES;
            } else if (account.getAccountType() == AccountType.EQUITY) {
                names = SPLIT_EQUITY_NAMES;
            } else if (account.getAccountType() == AccountType.LIABILITY) {
                names = SPLIT_LIABILITY_NAMES;
            } else if (account.memberOf(AccountGroup.ASSET)) {
                names = SPLIT_BANK_NAMES;
            } else {
                names = SPLIT_GENERIC_NAMES;
            }
        }
        return new RegisterTable(new SplitsRegisterTableModel(account, names, splits));
    }

    /**
     * Creates and returns a RegisterTable for investment transaction fees.
     * 
     * @param account the account to create a new table for
     * @param splits transaction entries to display
     * @return returns a GenericRegisterTable of the appropriate type
     */
    public static RegisterTable generateInvestmentFeesTable(final Account account, final List<TransactionEntry> splits) {

        if (!account.memberOf(AccountGroup.INVEST)) {
            throw new RuntimeException("The supplied account does not belong to the Investment group");
        }

        String[] names; // reference to the correct column names

        if (useAccountingTerms) {
            names = SPLIT_ACCOUNTING_NAMES;
        } else {
            names = SPLIT_GENERIC_NAMES;
        }
        return new RegisterTable(new SplitsRegisterTableModel(account, names, splits));
    }

    /**
     * Creates and returns a RegisterTable for investment transaction fees.
     * 
     * @param account the account to create a new table for
     * @param splits transaction entries to display
     * @return returns a GenericRegisterTable of the appropriate type
     */
    public static RegisterTable generateInvestmentGainsLossTable(final Account account, final List<TransactionEntry> splits) {

        if (!account.memberOf(AccountGroup.INVEST)) {
            throw new RuntimeException("The supplied account does not belong to the Investment group");
        }

        String[] names; // reference to the correct column names

        if (useAccountingTerms) {
            names = SPLIT_ACCOUNTING_NAMES;
        } else {
            names = SPLIT_GAINLOSS_NAMES;
        }
        return new RegisterTable(new SplitsRegisterTableModel(account, names, splits));
    }

    /**
     * Creates the appropriate register panel given an account.
     * 
     * @param account the account to create a register panel for
     * @return the register panel
     */
    static AbstractRegisterPanel createRegisterPanel(final Account account) {
        
        if (account.getAccountType() == AccountType.SIMPLEINVEST) { // invest account group, but needs a simple register 
            return new RegisterPanel(account); 
        } else if (account.memberOf(AccountGroup.INVEST)) {
            return new InvestmentRegisterPanel(account);
        } else if (account.getAccountType() == AccountType.LIABILITY) {
            return new LiabilityRegisterPanel(account);
        } else {
            return new RegisterPanel(account);
        }
    }

    /**
     * Generates tab names for transaction forms.
     * 
     * @param account account to generate tab names for
     * @return tab names with increase name at 0 and decrease name at 1
     */
    public static String[] getCreditDebitTabNames(final Account account) {
        if (RegisterFactory.isAccountingTermsEnabled()) {

            if (account.memberOf(AccountGroup.INCOME) || account.memberOf(AccountGroup.EXPENSE) || account.memberOf(AccountGroup.ASSET) || account.memberOf(AccountGroup.INVEST) || account.memberOf(AccountGroup.LIABILITY)) {
                return new String[] { rb.getString(COLUMN_DEBIT), rb.getString(COLUMN_CREDIT) };
            }

            return new String[] { rb.getString(COLUMN_CREDIT), rb.getString(COLUMN_DEBIT) };
        }
        if (account.getAccountType() == AccountType.CREDIT) {
            return new String[] { rb.getString(COLUMN_PAYMENT), rb.getString(COLUMN_CHARGE) };
        } else if (account.getAccountType() == AccountType.EXPENSE) {
            return new String[] { rb.getString(COLUMN_EXPENSE), rb.getString(COLUMN_REBATE) };
        } else if (account.getAccountType() == AccountType.INCOME) {
            return new String[] { rb.getString(COLUMN_CHARGE), rb.getString(COLUMN_INCOME) };
        } else if (account.getAccountType() == AccountType.CASH) {
            return new String[] { rb.getString(COLUMN_RECEIVE), rb.getString(COLUMN_SPEND) };
        } else if (account.getAccountType() == AccountType.LIABILITY) {
            return new String[] { rb.getString(COLUMN_DECREASE), rb.getString(COLUMN_INCREASE) };
        } else if (account.getAccountType() == AccountType.EQUITY) {
            return new String[] { rb.getString(COLUMN_DECREASE), rb.getString(COLUMN_INCREASE) };
        } else if (account.getAccountType().getAccountGroup() == AccountGroup.ASSET) {
            return new String[] { rb.getString(COLUMN_DEPOSIT), rb.getString(COLUMN_WITHDRAWAL) };
        } else {
            return new String[] { rb.getString(COLUMN_INCREASE), rb.getString(COLUMN_DECREASE) };
        }
    }

    /**
     * Generates tab names for investment gains and loss.
     * 
     * @return tab names with increase name at 0 and decrease name at 1
     */
    public static String[] getGainLossTabNames() {
        if (RegisterFactory.isAccountingTermsEnabled()) {
            return new String[] { rb.getString(COLUMN_DEBIT), rb.getString(COLUMN_CREDIT) };
        }
        return new String[] { rb.getString("Column.Gain"), rb.getString("Column.Loss") };
    }
}
