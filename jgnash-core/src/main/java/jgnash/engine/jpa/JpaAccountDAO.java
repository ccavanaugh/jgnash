/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.RootAccount;
import jgnash.engine.SecurityNode;
import jgnash.engine.dao.AccountDAO;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            Future<RootAccount> future = executorService.submit(new Callable<RootAccount>() {
                @Override
                public RootAccount call() throws Exception {
                    Query q = em.createQuery("select a from RootAccount a");

                    List<RootAccount> list = (List<RootAccount>) q.getResultList();

                    if (list.size() == 1) {
                        return list.get(0);
                    } else if (list.size() > 1) {
                        logger.log(Level.SEVERE, "More than one RootAccount was found: " + list.size(), new Exception());
                        return list.get(0);
                    }

                    return null;
                }
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
        List<Account> accountList = Collections.EMPTY_LIST;

        emLock.lock();

        try {
            Future<List<Account>> future = executorService.submit(new Callable<List<Account>>() {
                @Override
                public List<Account> call() throws Exception {
                    Query q = em.createQuery("SELECT a FROM Account a WHERE a.markedForRemoval = false");

                    return new ArrayList<Account>(q.getResultList());
                }
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
            Future<Boolean> future = executorService.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {

                    em.getTransaction().begin();
                    em.persist(child);
                    em.merge(parent);
                    em.getTransaction().commit();

                    return true;
                }
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
        boolean result = false;

        emLock.lock();

        try {
            Future<Boolean> future = executorService.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    em.getTransaction().begin();
                    em.persist(account);
                    em.getTransaction().commit();

                    return true;
                }
            });

            result = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }

        return result;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#addAccountSecurity(jgnash.engine.Account, jgnash.engine.SecurityNode)
     */
    @Override
    public boolean addAccountSecurity(final Account account, final SecurityNode node) {
        boolean result = false;

        emLock.lock();

        try {
            Future<Boolean> future = executorService.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    em.getTransaction().begin();

                    em.persist(node);
                    em.merge(account);
                    em.getTransaction().commit();

                    return true;
                }
            });

            result = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Account> getAccountList(final AccountType type) {
        List<Account> accountList = Collections.EMPTY_LIST;

        emLock.lock();

        try {
            Future<List<Account>> future = executorService.submit(new Callable<List<Account>>() {
                @Override
                public List<Account> call() throws Exception {
                    String queryString = "SELECT a FROM Account a WHERE a.accountType = :type AND a.markedForRemoval = false";
                    Query query = em.createQuery(queryString);
                    query.setParameter("type", type);

                    return new ArrayList<Account>(query.getResultList());
                }
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
        List<Account> list = new ArrayList<>();

        emLock.lock();

        try {
            for (final Account a : getAccountList()) {
                if (a.memberOf(AccountGroup.INVEST)) {
                    list.add(a);
                }
            }
        } finally {
            emLock.unlock();
        }

        return list;
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
        return simpleUpdate(account);
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
        return simpleUpdate(account);
    }

    @Override
    public void refreshAccount(final Account account) {
        refresh(account);
    }

    private boolean simpleUpdate(final Account account) {
        boolean result = false;

        emLock.lock();

        try {

            Future<Boolean> future = executorService.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {

                    boolean result = false;
                    em.getTransaction().begin();

                    if (em.contains(account)) { // don't try if the EntityManager does not contain the account
                        try {
                            em.merge(account);

                            result = true;
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, e.getMessage(), e);
                        }
                    } else {
                        logger.log(Level.SEVERE, "Tried to update an account that was not persisted", new Exception());
                    }

                    em.getTransaction().commit();

                    return result;
                }
            });

            result = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        } finally {
            emLock.unlock();
        }

        return result;
    }
}
