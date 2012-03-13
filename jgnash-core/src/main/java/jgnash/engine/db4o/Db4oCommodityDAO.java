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

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.query.Predicate;
import com.db4o.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.CommodityNode;
import jgnash.engine.CurrencyNode;
import jgnash.engine.ExchangeRate;
import jgnash.engine.ExchangeRateHistoryNode;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.engine.dao.CommodityDAO;

/**
 * Hides all the db4o commodity code
 *
 * @author Craig Cavanaugh
 * @version $Id: Db4oCommodityDAO.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */

class Db4oCommodityDAO extends AbstractDb4oDAO implements CommodityDAO {

    private final Logger logger = Logger.getLogger(Db4oCommodityDAO.class.getName());

    Db4oCommodityDAO(final ObjectContainer container, final boolean isRemote) {
        super(container, isRemote);
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#addCommodity(jgnash.engine.CommodityNode)
     */
    @Override
    public synchronized boolean addCommodity(CommodityNode node) {

        boolean result = false;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.set(node);
            commit();

            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
            result = true;
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#addSecurityHistory(jgnash.engine.SecurityNode, jgnash.engine.SecurityHistoryNode)
     */
    @Override
    public boolean addSecurityHistory(final SecurityNode node, final SecurityHistoryNode hNode) {

        boolean result = false;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.set(hNode);
            container.set(node);
            commit();
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);

            result = true;
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    @Override
    public boolean addExchangeRateHistory(final ExchangeRate rate, final ExchangeRateHistoryNode hNode) {

        boolean result = false;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {

            container.set(rate);
            container.set(hNode);
            commit();
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);

            result = true;
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    @Override
    public boolean removeExchangeRateHistory(final ExchangeRate rate, final ExchangeRateHistoryNode hNode) {
        boolean result = false;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {

            container.delete(hNode);
            container.ext().purge(hNode);

            container.set(rate);
            commit();

            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);

            result = true;
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#getCurrencies()
     */
    @Override
    @SuppressWarnings("unchecked")
    public synchronized List<CurrencyNode> getCurrencies() {

        List<CurrencyNode> list = Collections.emptyList();

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            Query query = container.query();
            query.constrain(CurrencyNode.class);
            query.descend("markedForRemoval").constrain(Boolean.FALSE);

            list = new ArrayList<CurrencyNode>(query.execute());

            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return list;
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#getExchangeNode(java.lang.String)
     */
    @Override
    public ExchangeRate getExchangeNode(final String rateId) {
        ExchangeRate rate = null;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {

            Query query = container.query();
            query.constrain(ExchangeRate.class);
            query.descend("rateId").constrain(rateId);

            ObjectSet<?> result = query.execute();

            if (result.size() == 1) {
                rate = (ExchangeRate) result.get(0);
            }

            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }
        return rate;
    }

    /**
     * @see jgnash.engine.dao.CommodityDAO#getSecurities()
     */
    @Override
    public synchronized List<SecurityNode> getSecurities() {

        List<SecurityNode> list = Collections.emptyList();

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {

            // Remove any that are marked for removal
            list = stripMarkedForRemoval(new ArrayList<SecurityNode>(container.query(SecurityNode.class)));

            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return list;
    }

    @Override
    public synchronized List<ExchangeRate> getExchangeRates() {
        List<ExchangeRate> list = Collections.emptyList();

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {

            // Remove any that are marked for removal
            list = stripMarkedForRemoval(new ArrayList<ExchangeRate>(container.query(ExchangeRate.class)));

            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return list;
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#removeSecurityHistory(jgnash.engine.SecurityNode, jgnash.engine.SecurityHistoryNode)
     */
    @Override
    public boolean removeSecurityHistory(final SecurityNode node, final SecurityHistoryNode hNode) {

        boolean result = false;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {

            container.delete(hNode);
            container.ext().purge(hNode);

            container.set(node);
            commit();

            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);

            result = true;
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#setExchangeRate(jgnash.engine.ExchangeRate)
     */
    @Override
    public void addExchangeRate(ExchangeRate eRate) {
        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.set(eRate);
            commit();
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }
    }

    @Override
    public void refreshCommodityNode(CommodityNode node) {
        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.ext().refresh(node, 2);
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }
    }

    @Override
    public void refreshExchangeRate(ExchangeRate rate) {
        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.ext().refresh(rate, 2);
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#updateCommodityNode(jgnash.engine.CommodityNode)
     */

    @Override
    public synchronized boolean updateCommodityNode(final CommodityNode node) {
        boolean result = false;

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            container.set(node);
            commit();
            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);
            result = true;
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return result;
    }

    /*
     * @see jgnash.engine.CommodityDAOInterface#getActiveAccountCommodities()
     */

    @Override
    public Set<CurrencyNode> getActiveCurrencies() {
        Set<CurrencyNode> currencies = new HashSet<CurrencyNode>();

        if (container.ext().setSemaphore(GLOBAL_SEMAPHORE, SEMAPHORE_WAIT_TIME)) {
            ActiveAccountCurrencyPredicate accountPredicate = new ActiveAccountCurrencyPredicate();
            ActiveSecurityCurrencyPredicate securityPredicate = new ActiveSecurityCurrencyPredicate();

            container.query(accountPredicate);
            container.query(securityPredicate);

            container.ext().releaseSemaphore(GLOBAL_SEMAPHORE);

            currencies.addAll(accountPredicate.getCurrencyNodes());
            currencies.addAll(securityPredicate.getCurrencyNodes());
        } else {
            logger.severe(SEMAPHORE_WARNING);
        }

        return currencies;
    }

    /**
     * Query predicate that returns a set of active CurrencyNodes used in accounts
     */
    private static class ActiveAccountCurrencyPredicate extends Predicate<Account> {
        private static final long serialVersionUID = -652916106551112316L;

        private final Set<CurrencyNode> set = new HashSet<CurrencyNode>();

        @Override
        public boolean match(Account account) {
            set.add(account.getCurrencyNode());
            return false;
        }

        public Set<CurrencyNode> getCurrencyNodes() {
            return set;
        }
    }

    /**
     * Query predicate that returns a set of active CurrencyNodes used in accounts
     */
    private static class ActiveSecurityCurrencyPredicate extends Predicate<SecurityNode> {
        private static final long serialVersionUID = -652916106551112316L;

        private final Set<CurrencyNode> set = new HashSet<CurrencyNode>();

        @Override
        public boolean match(SecurityNode node) {
            set.add(node.getReportedCurrencyNode());
            return false;
        }

        public Set<CurrencyNode> getCurrencyNodes() {
            return set;
        }
    }
}
