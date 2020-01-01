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
package jgnash.engine.xstream;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jgnash.engine.Account;
import jgnash.engine.CommodityNode;
import jgnash.engine.CurrencyNode;
import jgnash.engine.ExchangeRate;
import jgnash.engine.SecurityHistoryEvent;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.engine.StoredObject;
import jgnash.engine.dao.CommodityDAO;
import jgnash.util.NotNull;

/**
 * Hides all the db4o commodity code.
 *
 * @author Craig Cavanaugh
 */
class XStreamCommodityDAO extends AbstractXStreamDAO implements CommodityDAO {

    XStreamCommodityDAO(final AbstractXStreamContainer container) {
        super(container);
    }

    @Override
    public boolean addCommodity(final CommodityNode node) {
        boolean result = container.set(node);
        commit();
        return result;
    }

    @Override
    public boolean addExchangeRateHistory(final ExchangeRate rate) {
        commit();
        return true;
    }

    @Override
    public boolean addSecurityHistory(final SecurityNode node, final SecurityHistoryNode historyNode) {
        commit();
        return true;
    }

    @Override
    public boolean addSecurityHistoryEvent(final SecurityNode node, final SecurityHistoryEvent historyEvent) {
        commit();
        return true;
    }

    @Override
    public Set<CurrencyNode> getActiveCurrencies() {
        Set<CurrencyNode> set = stripMarkedForRemoval(container.query(Account.class))
                .parallelStream().map(Account::getCurrencyNode).collect(Collectors.toSet());

        set.addAll(stripMarkedForRemoval(container.query(SecurityNode.class))
                .parallelStream().map(SecurityNode::getReportedCurrencyNode).collect(Collectors.toList()));

        return set;
    }

    @Override
    public List<CurrencyNode> getCurrencies() {
        return stripMarkedForRemoval(container.query(CurrencyNode.class));
    }

    @Override
    public CurrencyNode getCurrencyByUuid(final UUID uuid) {
        return getObjectByUuid(CurrencyNode.class, uuid);
    }

    @Override
    public SecurityNode getSecurityByUuid(final UUID uuid) {
        return getObjectByUuid(SecurityNode.class, uuid);
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
    public ExchangeRate getExchangeRateByUuid(final UUID uuid) {
        ExchangeRate exchangeRate = null;

        StoredObject o = container.get(uuid);

        if (o instanceof ExchangeRate) {
            exchangeRate = (ExchangeRate) o;
        }

        return exchangeRate;
    }

    @Override
    @NotNull public List<SecurityNode> getSecurities() {
        return stripMarkedForRemoval(container.query(SecurityNode.class));
    }

    @Override
    public List<ExchangeRate> getExchangeRates() {
        return stripMarkedForRemoval(container.query(ExchangeRate.class));
    }

    @Override
    public boolean removeExchangeRateHistory(final ExchangeRate rate) {
        commit();
        return true;
    }

    @Override
    public boolean removeSecurityHistory(final SecurityNode node, final SecurityHistoryNode historyNode) {
        commit();
        return true;
    }

    @Override
    public boolean removeSecurityHistoryEvent(final SecurityNode node, final SecurityHistoryEvent historyEvent) {
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
