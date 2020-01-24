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
package jgnash.engine.dao;

import java.util.List;
import java.util.UUID;

import jgnash.engine.Account;
import jgnash.engine.RootAccount;
import jgnash.engine.SecurityNode;

/**
 * Account DAO Interface.
 *
 * @author Craig Cavanaugh
 */
public interface AccountDAO extends DAO {

    RootAccount getRootAccount();

    List<Account> getAccountList();

    boolean addAccount(Account parent, final Account child);

    boolean addRootAccount(RootAccount account);

    /**
     * Adds a SecurityNode from a InvestmentAccount.
     *
     * @param account account to add security to
     * @param node    security to add
     * @return true if success
     */
    boolean addAccountSecurity(final Account account, final SecurityNode node);

    /**
     * Returns a list of IncomeAccounts.
     *
     * @return list of income accounts
     */
    List<Account> getIncomeAccountList();

    /**
     * Returns a list of ExpenseAccounts.
     *
     * @return list of expense accounts
     */
    List<Account> getExpenseAccountList();

    /**
     * Returns a list of InvestmentAccounts.
     *
     * @return list of investment accounts
     */
    List<Account> getInvestmentAccountList();

    Account getAccountByUuid(final UUID uuid);

    boolean updateAccount(Account account);

    /**
     * Toggles the visibility of an account given its ID.
     *
     * @param account The account to toggle visibility
     * @return <tt>true</tt> if the supplied account ID was found
     *         <tt>false</tt> if the supplied account ID was not found
     */
    boolean toggleAccountVisibility(final Account account);

}