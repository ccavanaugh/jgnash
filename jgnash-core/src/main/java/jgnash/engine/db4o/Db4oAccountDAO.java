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
package jgnash.engine.db4o;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.RootAccount;
import jgnash.engine.SecurityNode;
import jgnash.engine.dao.AccountDAO;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.query.Predicate;
import com.db4o.query.Query;

/**
 * db4o Account DAO
 *
 * @author Craig Cavanaugh
 * @version $Id: Db4oAccountDAO.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
class Db4oAccountDAO extends AbstractDb4oDAO implements AccountDAO {

    private static final Logger logger = Logger.getLogger(Db4oAccountDAO.class.getName());

    Db4oAccountDAO(final ObjectContainer container, final boolean isRemote) {
        super(container, isRemote);
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#getRootAccount()
     */
    @Override
    public RootAccount getRootAccount() {

        RootAccount root = null;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            List<RootAccount> list = container.query(RootAccount.class);

            if (list.size() == 1) {
                root = list.get(0);
            }

            // cleanup empty RootAccounts from previous bug 2222143
            if (list.size() > 1) {
                logger.severe("More than one RootAccount found");
                root = list.get(0);

                List<RootAccount> emptyList = new ArrayList<>();

                // find the root with accounts
                for (RootAccount a : list) {
                    if (a.getChildCount() > 0) {
                        root = a;
                    } else {
                        emptyList.add(a);
                    }
                }

                for (RootAccount a : emptyList) {
                    container.delete(a);
                }
            }

            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
        }

        return root;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#getAccountList()
     */
    @Override
    public List<Account> getAccountList() {

        List<Account> list = Collections.emptyList();
        List<Account> resultList = new ArrayList<>();

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            list = container.query(Account.class);
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        /* Flush any that have been marked for removal
         * Returned list for query does not support iterator interface */
        for (Account a : list) {
            if (!a.isMarkedForRemoval()) {
                resultList.add(a);
            }
        }
        return resultList;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#addAccount(jgnash.engine.Account, jgnash.engine.Account)
     */
    @Override
    public boolean addAccount(Account parent, final Account child) {
        boolean result = false;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.set(child);
            container.set(parent);
            commit();
            result = true;
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#addRootAccount(jgnash.engine.RootAccount)
     */
    @Override
    public boolean addRootAccount(RootAccount account) {
        boolean result = false;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.set(account);
            commit();
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
            result = true;
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#addAccountSecurity(jgnash.engine.Account, jgnash.engine.SecurityNode)
     */
    @Override
    public boolean addAccountSecurity(final Account account, final SecurityNode node) {
        boolean result = false;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.set(node);
            container.set(account);
            commit();
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
            result = true;
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    /*
    * @see jgnash.engine.AccountDAOInterface#getIncomeAccountList()
    */
    @Override
    public List<Account> getIncomeAccountList() {
        List<Account> list = Collections.emptyList();

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            list = container.query(new AccountTypePredicate(AccountType.INCOME));
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return list;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#getExpenseAccountList()
     */
    @Override
    public List<Account> getExpenseAccountList() {
        List<Account> list = Collections.emptyList();

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            list = container.query(new AccountTypePredicate(AccountType.EXPENSE));
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return list;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#getInvestmentAccountList()
     */
    @Override
    public List<Account> getInvestmentAccountList() {
        List<Account> list = Collections.emptyList();

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            list = container.query(new AccountGroupPredicate(AccountGroup.INVEST));
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return list;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#getAccountByUuid(java.lang.String)
     */
    @Override
    public Account getAccountByUuid(final String uuid) {
        Account account = null;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            Query query = container.query();
            query.constrain(Account.class);
            query.descend("uuid").constrain(uuid);

            ObjectSet<?> result = query.execute();

            if (result.size() == 1) {
                account = (Account) result.get(0);
            }
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return account;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#updateAccount(jgnash.engine.Account)
     */
    @Override
    public boolean updateAccount(Account account) {
        boolean result = false;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.set(account);
            commit();
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
            result = true;
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    @Override
    public boolean setAccountProperty(Account account, Object object) {
        boolean result = false;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.set(object);
            container.set(account);
            commit();
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
            result = true;
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    @Override
    public boolean removeAccountProperty(Account account, Object object) {
        boolean result = false;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.delete(object);
            container.ext().purge(object);

            container.set(account);
            commit();
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
            result = true;
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    /**
     * @see jgnash.engine.dao.AccountDAO#toggleAccountVisibility(jgnash.engine.Account)
     */
    @Override
    public boolean toggleAccountVisibility(final Account account) {
        boolean result = false;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.set(account);
            commit();
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
            result = true;
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    @Override
    public void refreshAccount(Account account) {
        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.ext().refresh(account, 4);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }
    }

    private static class AccountGroupPredicate extends Predicate<Account> {
        private static final long serialVersionUID = -860987774949510386L;

        private final AccountGroup group;

        AccountGroupPredicate(AccountGroup group) {
            this.group = group;
        }

        @Override
        public boolean match(Account account) {
            return account.memberOf(group) && !account.isMarkedForRemoval();
        }
    }

    private static class AccountTypePredicate extends Predicate<Account> {
        private static final long serialVersionUID = -7268308931621048944L;

        private final AccountType type;

        AccountTypePredicate(AccountType type) {
            this.type = type;
        }

        @Override
        public boolean match(Account account) {
            return account.instanceOf(type) && !account.isMarkedForRemoval();
        }
    }
}
