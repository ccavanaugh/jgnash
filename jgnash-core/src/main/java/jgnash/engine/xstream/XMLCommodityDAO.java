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
package jgnash.engine.xstream;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 */
public class XMLCommodityDAO extends AbstractXMLDAO implements CommodityDAO {

    XMLCommodityDAO(final AbstractXStreamContainer container) {
        super(container);
    }

    @Override
    public boolean addCommodity(final CommodityNode node) {
        boolean result = container.set(node);
        commit();
        return result;
    }

    @Override
    public boolean addExchangeRateHistory(final ExchangeRate rate, final ExchangeRateHistoryNode hNode) {
        commit();
        return true;
    }

    @Override
    public boolean addSecurityHistory(final SecurityNode node, final SecurityHistoryNode hNode) {
        commit();
        return true;
    }

    @Override
    public Set<CurrencyNode> getActiveCurrencies() {
        Set<CurrencyNode> set = new HashSet<>();

        for (Account a : stripMarkedForRemoval(container.query(Account.class))) {
            set.add(a.getCurrencyNode());
        }

        for (SecurityNode node : stripMarkedForRemoval(container.query(SecurityNode.class))) {
            set.add(node.getReportedCurrencyNode());
        }

        return set;
    }

    @Override
    public List<CurrencyNode> getCurrencies() {
        return stripMarkedForRemoval(container.query(CurrencyNode.class));
    }

    @Override
    public ExchangeRate getExchangeNode(final String rateId) {
        ExchangeRate rate = null;

        for (ExchangeRate r : stripMarkedForRemoval(container.query(ExchangeRate.class))) {
            if (r.getRateId().equals(rateId)) {
                rate = r;
                break;
            }
        }

        return rate;
    }

    @Override
    public List<SecurityNode> getSecurities() {
        return stripMarkedForRemoval(container.query(SecurityNode.class));
    }

    @Override
    public List<ExchangeRate> getExchangeRates() {
        return stripMarkedForRemoval(container.query(ExchangeRate.class));
    }

    @Override
    public void refreshCommodityNode(final CommodityNode node) {
        // do nothing
    }

    @Override
    public void refreshExchangeRate(final ExchangeRate rate) {
        // do nothing
    }

    @Override
    public boolean removeExchangeRateHistory(final ExchangeRate rate, final ExchangeRateHistoryNode hNode) {
        commit();
        return true;
    }

    @Override
    public boolean removeSecurityHistory(final SecurityNode node, final SecurityHistoryNode hNode) {
        commit();
        return true;
    }

    @Override
    public void addExchangeRate(final ExchangeRate eRate) {
        container.set(eRate);
        commit();
    }

    @Override
    public boolean updateCommodityNode(final CommodityNode node) {
        commit();
        return true;
    }
}
