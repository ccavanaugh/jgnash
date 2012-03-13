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
package jgnash.engine.dao;

import java.util.List;

import jgnash.engine.Account;
import jgnash.engine.RootAccount;
import jgnash.engine.SecurityNode;

/**
 * Account DAO Interface
 *
 * @author Craig Cavanaugh
 * @version $Id: AccountDAO.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public interface AccountDAO {

    public RootAccount getRootAccount();

    public List<Account> getAccountList();

    public boolean addAccount(Account parent, final Account child);

    public boolean addRootAccount(RootAccount account);

    /**
     * Adds a SecurityNode from a InvestmentAccount
     *
     * @param account account to add security to
     * @param node    security to add
     * @return true if success
     */
    public boolean addAccountSecurity(final Account account, final SecurityNode node);

    /**
     * Returns a list of IncomeAccounts
     *
     * @return list of income accounts
     */
    public List<Account> getIncomeAccountList();

    /**
     * Returns a list of ExpenseAccounts
     *
     * @return list of expense accounts
     */
    public List<Account> getExpenseAccountList();

    /**
     * Returns a list of InvestmentAccounts
     *
     * @return list of investment accounts
     */
    public List<Account> getInvestmentAccountList();

    public Account getAccountByUuid(final String uuid);

    public boolean updateAccount(Account account);

    public boolean setAccountProperty(Account account, Object object);

    public boolean removeAccountProperty(Account account, Object object);

    /**
     * Toggles the visibility of an account given its ID.
     *
     * @param account The account to toggle visibility
     * @return <tt>true</tt> if the supplied account ID was found
     *         <tt>false</tt> if the supplied account ID was not found
     */
    public boolean toggleAccountVisibility(final Account account);

    /**
     * Force the engine to refresh the loading account.
     * Intended for client / server use
     *
     * @param account account to refresh
     */
    public void refreshAccount(Account account);

}