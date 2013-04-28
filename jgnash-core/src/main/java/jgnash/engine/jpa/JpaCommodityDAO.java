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
import jgnash.engine.CommodityNode;
import jgnash.engine.CurrencyNode;
import jgnash.engine.ExchangeRate;
import jgnash.engine.ExchangeRateHistoryNode;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.engine.dao.CommodityDAO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;


/**
 * Commodity DAO
 *
 * @author Craig Cavanaugh
 */
class JpaCommodityDAO extends AbstractJpaDAO implements CommodityDAO {

    // private static final Logger logger = Logger.getLogger(JpaCommodityDAO.class.getName());

    JpaCommodityDAO(final EntityManager entityManager, final boolean isRemote) {
        super(entityManager, isRemote);
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#addCurrency(jgnash.engine.CommodityNode)
     */
    @Override
    public boolean addCommodity(final CommodityNode node) {
        try {
            emLock.lock();

            em.getTransaction().begin();

            em.persist(node);

            em.getTransaction().commit();

            return true;
        } finally {
            emLock.unlock();
        }
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#addSecurityHistory(jgnash.engine.SecurityNode, jgnash.engine.SecurityHistoryNode)
     */
    @Override
    public boolean addSecurityHistory(final SecurityNode node, final SecurityHistoryNode hNode) {

        try {
            emLock.lock();

            em.getTransaction().begin();

            em.persist(node);

            em.getTransaction().commit();

            return true;
        } finally {
            emLock.unlock();
        }
    }

    @Override
    public boolean addExchangeRateHistory(final ExchangeRate rate, final ExchangeRateHistoryNode hNode) {
        try {
            emLock.lock();

            em.getTransaction().begin();

            em.persist(rate);

            em.getTransaction().commit();
            return true;
        } finally {
            emLock.unlock();
        }
    }

    @Override
    public boolean removeExchangeRateHistory(final ExchangeRate rate, final ExchangeRateHistoryNode hNode) {
        try {
            emLock.lock();
            em.getTransaction().begin();

            em.remove(hNode);
            em.persist(rate);

            em.getTransaction().commit();
            return true;
        } finally {
            emLock.unlock();
        }
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#getCurrencies()
     */
    @Override
    public List<CurrencyNode> getCurrencies() {
        try {
            emLock.lock();
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<CurrencyNode> cq = cb.createQuery(CurrencyNode.class);
            Root<CurrencyNode> root = cq.from(CurrencyNode.class);
            cq.select(root);

            TypedQuery<CurrencyNode> q = em.createQuery(cq);


            return stripMarkedForRemoval(new ArrayList<>(q.getResultList()));
        } finally {
            emLock.unlock();
        }
    }

    @Override
    public CurrencyNode getCurrencyByUuid(final String uuid) {
        return getObjectByUuid(CurrencyNode.class, uuid);
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#getExchangeNode(java.lang.String)
     */
    @Override
    public ExchangeRate getExchangeNode(final String rateId) {
        try {
            emLock.lock();
            ExchangeRate exchangeRate = null;

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ExchangeRate> cq = cb.createQuery(ExchangeRate.class);
            Root<ExchangeRate> root = cq.from(ExchangeRate.class);
            cq.select(root);

            TypedQuery<ExchangeRate> q = em.createQuery(cq);

            for (ExchangeRate rate : q.getResultList()) {
                if (rate.getRateId().equals(rateId)) {
                    exchangeRate = rate;
                    break;
                }
            }
            return exchangeRate;
        } finally {
            emLock.unlock();
        }
    }

    @Override
    public ExchangeRate getExchangeRateByUuid(final String uuid) {
        return getObjectByUuid(ExchangeRate.class, uuid);
    }

    @Override
    public SecurityNode getSecurityByUuid(final String uuid) {
        return getObjectByUuid(SecurityNode.class, uuid);
    }

    /**
     * @see jgnash.engine.dao.CommodityDAO#getSecurities()
     */
    @Override
    public List<SecurityNode> getSecurities() {
        try {
            emLock.lock();
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SecurityNode> cq = cb.createQuery(SecurityNode.class);
            Root<SecurityNode> root = cq.from(SecurityNode.class);
            cq.select(root);

            TypedQuery<SecurityNode> q = em.createQuery(cq);


            return stripMarkedForRemoval(new ArrayList<>(q.getResultList()));
        } finally {
            emLock.unlock();
        }
    }

    @Override
    public List<ExchangeRate> getExchangeRates() {
        try {
            emLock.lock();
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ExchangeRate> cq = cb.createQuery(ExchangeRate.class);
            Root<ExchangeRate> root = cq.from(ExchangeRate.class);
            cq.select(root);

            TypedQuery<ExchangeRate> q = em.createQuery(cq);


            return stripMarkedForRemoval(new ArrayList<>(q.getResultList()));
        } finally {
            emLock.unlock();
        }
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#removeSecurityHistory(jgnash.engine.SecurityNode, jgnash.engine.SecurityHistoryNode)
     */
    @Override
    public boolean removeSecurityHistory(final SecurityNode node, final SecurityHistoryNode hNode) {
        try {
            emLock.lock();
            em.getTransaction().begin();

            em.persist(node);

            em.getTransaction().commit();

            return true;
        } finally {
            emLock.unlock();
        }
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#setExchangeRate(jgnash.engine.ExchangeRate)
     */
    @Override
    public void addExchangeRate(final ExchangeRate eRate) {
        try {
            emLock.lock();
            em.getTransaction().begin();

            em.persist(eRate);

            em.getTransaction().commit();
        } finally {
            emLock.unlock();
        }
    }

    @Override
    public void refreshCommodityNode(final CommodityNode node) {
        try {
            emLock.lock();
            em.getTransaction().begin();

            em.merge(node);

            em.getTransaction().commit();
        } finally {
            emLock.unlock();
        }
    }

    @Override
    public void refreshExchangeRate(final ExchangeRate rate) {
        try {
            emLock.lock();
            em.getTransaction().begin();

            em.merge(rate);

            em.getTransaction().commit();
        } finally {
            emLock.unlock();
        }
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#updateCommodityNode(jgnash.engine.CommodityNode)
     */

    @Override
    public boolean updateCommodityNode(final CommodityNode node) {
        try {
            emLock.lock();
            em.getTransaction().begin();

            em.merge(node);

            em.getTransaction().commit();
            return true;
        } finally {
            emLock.unlock();
        }
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#getActiveAccountCommodities()
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<CurrencyNode> getActiveCurrencies() {
        try {
            emLock.lock();

            Query q = em.createQuery("SELECT a FROM Account a WHERE a.markedForRemoval = false");

            List<Account> accountList = q.getResultList();

            Set<CurrencyNode> currencies = new HashSet<>();

            for (Account account : accountList) {
                currencies.add(account.getCurrencyNode());

                for (SecurityNode node : account.getSecurities()) {
                    currencies.add(node.getReportedCurrencyNode());
                }
            }

            return currencies;
        } finally {
            emLock.unlock();
        }
    }
}
