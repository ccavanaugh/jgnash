/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import jgnash.engine.Account;
import jgnash.engine.CommodityNode;
import jgnash.engine.CurrencyNode;
import jgnash.engine.ExchangeRate;
import jgnash.engine.SecurityHistoryEvent;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.engine.dao.CommodityDAO;
import jgnash.util.NotNull;

/**
 * Commodity DAO.
 *
 * @author Craig Cavanaugh
 */
class JpaCommodityDAO extends AbstractJpaDAO implements CommodityDAO {

    private static final Logger logger = Logger.getLogger(JpaCommodityDAO.class.getName());

    JpaCommodityDAO(final EntityManager entityManager, final boolean isRemote) {
        super(entityManager, isRemote);
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#addCurrency(jgnash.engine.CommodityNode)
     */
    @Override
    public boolean addCommodity(final CommodityNode node) {
        return persist(node);
    }

    @Override
    public boolean addSecurityHistory(final SecurityNode node, final SecurityHistoryNode historyNode) {
        return persist(historyNode, node);
    }

    @Override
    public boolean addSecurityHistoryEvent(final SecurityNode node, final SecurityHistoryEvent historyEvent) {
        return persist(historyEvent, node);
    }


    @Override
    public boolean removeSecurityHistory(final SecurityNode node, final SecurityHistoryNode historyNode) {
        return persist(node, historyNode);
    }

    @Override
    public boolean removeSecurityHistoryEvent(final SecurityNode node, final SecurityHistoryEvent historyEvent) {
        return persist(node, historyEvent);
    }

    @Override
    public boolean addExchangeRateHistory(final ExchangeRate rate) {
        return merge(rate) != null;
    }

    @Override
    public boolean removeExchangeRateHistory(final ExchangeRate rate) {
        return merge(rate) != null;
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#getCurrencies()
     */
    @Override
    public List<CurrencyNode> getCurrencies() {
        List<CurrencyNode> currencyNodeList = Collections.emptyList();

        try {
            Future<List<CurrencyNode>> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    final CriteriaBuilder cb = em.getCriteriaBuilder();
                    final CriteriaQuery<CurrencyNode> cq = cb.createQuery(CurrencyNode.class);
                    final Root<CurrencyNode> root = cq.from(CurrencyNode.class);
                    cq.select(root);

                    final TypedQuery<CurrencyNode> q = em.createQuery(cq);

                    return stripMarkedForRemoval(new ArrayList<>(q.getResultList()));
                } finally {
                    emLock.unlock();
                }
            });

            currencyNodeList = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        return currencyNodeList;
    }

    @Override
    public CurrencyNode getCurrencyByUuid(final UUID uuid) {
        return getObjectByUuid(CurrencyNode.class, uuid);
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#getExchangeNode(java.lang.String)
     */
    @Override
    public ExchangeRate getExchangeNode(final String rateId) {
        ExchangeRate exchangeRate = null;

        try {
            Future<ExchangeRate> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    ExchangeRate exchangeRate1 = null;

                    final CriteriaBuilder cb = em.getCriteriaBuilder();
                    final CriteriaQuery<ExchangeRate> cq = cb.createQuery(ExchangeRate.class);
                    final Root<ExchangeRate> root = cq.from(ExchangeRate.class);
                    cq.select(root);

                    final TypedQuery<ExchangeRate> q = em.createQuery(cq);

                    for (final ExchangeRate rate : q.getResultList()) {
                        if (rate.getRateId().equals(rateId)) {
                            exchangeRate1 = rate;
                            break;
                        }
                    }

                    return exchangeRate1;
                } finally {
                    emLock.unlock();
                }
            });

            exchangeRate = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        return exchangeRate;
    }

    @Override
    public ExchangeRate getExchangeRateByUuid(final UUID uuid) {
        return getObjectByUuid(ExchangeRate.class, uuid);
    }

    @Override
    public SecurityNode getSecurityByUuid(final UUID uuid) {
        return getObjectByUuid(SecurityNode.class, uuid);
    }

    /*
     * @see jgnash.engine.dao.CommodityDAO#getSecurities()
     */
    @Override
    @NotNull public List<SecurityNode> getSecurities() {
        List<SecurityNode> securityNodeList = Collections.emptyList();

        try {
            Future<List<SecurityNode>> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    final CriteriaBuilder cb = em.getCriteriaBuilder();
                    final CriteriaQuery<SecurityNode> cq = cb.createQuery(SecurityNode.class);
                    Root<SecurityNode> root = cq.from(SecurityNode.class);
                    cq.select(root);

                    TypedQuery<SecurityNode> q = em.createQuery(cq);

                    return stripMarkedForRemoval(new ArrayList<>(q.getResultList()));
                } finally {
                    emLock.unlock();
                }
            });

            securityNodeList = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        return securityNodeList;
    }

    @Override
    public List<ExchangeRate> getExchangeRates() {
        List<ExchangeRate> exchangeRateList = Collections.emptyList();

        try {
            Future<List<ExchangeRate>> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    final CriteriaBuilder cb = em.getCriteriaBuilder();
                    final CriteriaQuery<ExchangeRate> cq = cb.createQuery(ExchangeRate.class);
                    final Root<ExchangeRate> root = cq.from(ExchangeRate.class);
                    cq.select(root);

                    final TypedQuery<ExchangeRate> q = em.createQuery(cq);

                    return stripMarkedForRemoval(new ArrayList<>(q.getResultList()));
                } finally {
                    emLock.unlock();
                }
            });

            exchangeRateList = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        return exchangeRateList;
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#setExchangeRate(jgnash.engine.ExchangeRate)
     */
    @Override
    public void addExchangeRate(final ExchangeRate eRate) {
        persist(eRate);
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#updateCommodityNode(jgnash.engine.CommodityNode)
     */
    @Override
    public boolean updateCommodityNode(final CommodityNode node) {
        return merge(node) != null;
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#getActiveAccountCommodities()
     */
    @Override
    public Set<CurrencyNode> getActiveCurrencies() {
        Set<CurrencyNode> currencyNodeSet = Collections.emptySet();

        try {
            Future<Set<CurrencyNode>> future = executorService.submit(() -> {
                emLock.lock();

                try {
                    final TypedQuery<Account> q = em.createQuery("SELECT a FROM Account a WHERE a.markedForRemoval = false",
                            Account.class);

                    final List<Account> accountList = q.getResultList();
                    final Set<CurrencyNode> currencies = new HashSet<>();

                    for (final Account account : accountList) {
                        currencies.add(account.getCurrencyNode());

                        currencies.addAll(account.getSecurities().parallelStream()
                                .map(SecurityNode::getReportedCurrencyNode).collect(Collectors.toList()));
                    }

                    return currencies;
                } finally {
                    emLock.unlock();
                }
            });

            currencyNodeSet = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        return currencyNodeSet;
    }
}
