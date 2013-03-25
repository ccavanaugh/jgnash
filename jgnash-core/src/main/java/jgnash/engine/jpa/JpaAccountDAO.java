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

import jgnash.engine.*;
import jgnash.engine.dao.AccountDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

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

        Query q = em.createQuery("select a from RootAccount a");

        List<RootAccount> list = (List<RootAccount>) q.getResultList();

        if (list.size() == 1) {
            root = list.get(0);
        } else if (list.size() > 1) {
            logger.log(Level.SEVERE, "More than one RootAccount was found: " + list.size(), new Exception());
            root = list.get(0);
        }

        return root;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#getAccountList()
     */
    @Override
    public List<Account> getAccountList() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Account> cq = cb.createQuery(Account.class);
        Root<Account> root = cq.from(Account.class);
        cq.select(root);

        TypedQuery<Account> q = em.createQuery(cq);

        // result lists are readonly
        return stripMarkedForRemoval(new ArrayList<>(q.getResultList()));
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#addAccount(jgnash.engine.Account, jgnash.engine.Account)
     */
    @Override
    public boolean addAccount(final Account parent, final Account child) {
        em.getTransaction().begin();

        em.persist(child);
        em.persist(parent);

        em.getTransaction().commit();

        return true;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#addRootAccount(jgnash.engine.RootAccount)
     */
    @Override
    public boolean addRootAccount(final RootAccount account) {
        em.getTransaction().begin();

        em.persist(account);

        em.getTransaction().commit();

        return true;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#addAccountSecurity(jgnash.engine.Account, jgnash.engine.SecurityNode)
     */
    @Override
    public boolean addAccountSecurity(final Account account, final SecurityNode node) {
        em.getTransaction().begin();

        em.persist(node);
        em.persist(account);

        em.getTransaction().commit();

        return true;
    }

    @SuppressWarnings("unchecked")
    private List<Account> getAccountList(final AccountType type) {

        String queryString = "SELECT a FROM Account a WHERE a.accountType = :type";
        Query query = em.createQuery(queryString);
        query.setParameter("type", type.name());

        return stripMarkedForRemoval(new ArrayList<Account>(query.getResultList()));
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

        for (Account a : getAccountList()) {
            if (a.memberOf(AccountGroup.INVEST)) {
                list.add(a);
            }
        }

        return list;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#getAccountByUuid(java.lang.String)
     */
    @Override
    public Account getAccountByUuid(final String uuid) {
        Account account = null;

        try {
            account = em.find(Account.class, uuid, LockModeType.PESSIMISTIC_READ);
        } catch (Exception e) {
            logger.info("Did not find Account for uuid: " + uuid);
        }

        return account;
    }

    /*
     * @see jgnash.engine.AccountDAOInterface#updateAccount(jgnash.engine.Account)
     */
    @Override
    public boolean updateAccount(final Account account) {
        return simpleUpdate(account);
    }

    @Override
    public boolean setAccountProperty(final Account account, final Object object) {
        return false;
    }

    @Override
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
        em.merge(account);
    }

    private boolean simpleUpdate(final Account account) {
        boolean result = false;

        if (em.contains(account)) { // don't try if the EntityManager does not contain the account
            try {
                em.getTransaction().begin();
                em.lock(account, LockModeType.PESSIMISTIC_WRITE);

                em.persist(account);

                em.lock(account, LockModeType.NONE);
                em.getTransaction().commit();

                result = true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        } else {
            logger.log(Level.SEVERE, "Tried to update an account that was not persisted", new Exception());
        }

        return result;
    }
}
