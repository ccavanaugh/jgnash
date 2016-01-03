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
package jgnash.engine.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.RootAccount;
import jgnash.engine.SecurityNode;
import jgnash.engine.dao.AccountDAO;

/**
 * Account DAO
 *
 * @author Craig Cavanaugh
 */
class JpaAccountDAO extends AbstractJpaDAO implements AccountDAO {

    private static final Logger logger = Logger.getLogger(JpaAccountDAO.class.getName());

    JpaAccountDAO(final EntityManager entityManager, final boolean isRemote) {
        super(entityManager, isRemote);
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#getRootAccount()
     */
    @Override
    @SuppressWarnings("unchecked")
    public RootAccount getRootAccount() {
        RootAccount root = null;

        emLock.lock();

        try {
            Future<RootAccount> future = executorService.submit(() -> {
                Query q = em.createQuery("select a from RootAccount a");

                List<RootAccount> list = (List<RootAccount>) q.getResultList();

                if (list.size() == 1) {
                    return list.get(0);
                } else if (list.size() > 1) {
                    logger.log(Level.SEVERE, "More than one RootAccount was found: " + list.size(), new Exception());
                    return list.get(0);
                }

                return null;
            });

            root = future.get();
        } catch (final ExecutionException | InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }

        return root;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#getAccountList()
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Account> getAccountList() {
        List<Account> accountList = Collections.emptyList();

        emLock.lock();

        try {
            Future<List<Account>> future = executorService.submit(() -> {
                Query q = em.createQuery("SELECT a FROM Account a WHERE a.markedForRemoval = false");

                return new ArrayList<>(q.getResultList());
            });

            accountList = future.get();
        } catch (final ExecutionException | InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }

        return accountList;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#addAccount(jgnash.engine.Account, jgnash.engine.Account)
     */
    @Override
    public boolean addAccount(final Account parent, final Account child) {
        boolean result = false;

        emLock.lock();

        try {
            Future<Boolean> future = executorService.submit(() -> {

                em.getTransaction().begin();
                em.persist(child);
                em.merge(parent);
                em.getTransaction().commit();

                return true;
            });

            result = future.get();
        } catch (final ExecutionException | InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }
        return result;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#addRootAccount(jgnash.engine.RootAccount)
     */
    @Override
    public boolean addRootAccount(final RootAccount account) {
        return persist(account);
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#addAccountSecurity(jgnash.engine.Account, jgnash.engine.SecurityNode)
     */
    @Override
    public boolean addAccountSecurity(final Account account, final SecurityNode node) {
        return merge(account) != null;
    }

    @SuppressWarnings("unchecked")
    private List<Account> getAccountList(final AccountType type) {
        List<Account> accountList = Collections.emptyList();

        emLock.lock();

        try {
            Future<List<Account>> future = executorService.submit(() -> {
                String queryString = "SELECT a FROM Account a WHERE a.accountType = :type AND a.markedForRemoval = false";
                Query query = em.createQuery(queryString);
                query.setParameter("type", type);

                return new ArrayList<>(query.getResultList());
            });

            accountList = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }

        return accountList;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#getIncomeAccountList()
     */
    @Override
    public List<Account> getIncomeAccountList() {
        return getAccountList(AccountType.INCOME);
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#getExpenseAccountList()
     */
    @Override
    public List<Account> getExpenseAccountList() {
        return getAccountList(AccountType.EXPENSE);
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#getInvestmentAccountList()
     */
    @Override
    public List<Account> getInvestmentAccountList() {
        emLock.lock();

        try {
            return getAccountList().parallelStream().filter(a -> a.memberOf(AccountGroup.INVEST))
                    .collect(Collectors.toList());
        } finally {
            emLock.unlock();
        }
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#getAccountByUuid(java.lang.String)
     */
    @Override
    public Account getAccountByUuid(final String uuid) {
        return getObjectByUuid(Account.class, uuid);
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#updateAccount(jgnash.engine.Account)
     */
    @Override
    public boolean updateAccount(final Account account) {
        return merge(account) != null;
    }

    @Override
    @Deprecated
    public boolean removeAccountProperty(final Account account, final Object object) {
        return false;
    }

    /**
     * @see jgnash.engine.dao.AccountDAO#toggleAccountVisibility(jgnash.engine.Account)
     */
    @Override
    public boolean toggleAccountVisibility(final Account account) {
        return merge(account) != null;
    }
}
