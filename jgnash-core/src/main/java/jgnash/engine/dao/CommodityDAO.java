/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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

import jgnash.engine.CommodityNode;
import jgnash.engine.CurrencyNode;
import jgnash.engine.ExchangeRate;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;

import java.util.List;
import java.util.Set;

/**
 * Commodity DAO Interface
 *
 * @author Craig Cavanaugh
 *
 */
public interface CommodityDAO {

    public boolean addCommodity(CommodityNode node);

    /**
     * Call after a {@code ExchangeRateHistoryNode} has been added.  This pushes the update
     * to the underlying database
     * @param rate ExchangeRate to update
     *
     * @return true if successful
     */
    public boolean addExchangeRateHistory(final ExchangeRate rate);

    /**
     * Call after a {@code SecurityHistoryNode} has been added.  This pushes the update
     * to the underlying database
     * @param node SecurityHistory to update
     * @param historyNode {@code SecurityHistoryNode to add}
     *
     * @return true if successful
     */
    public boolean addSecurityHistory(final SecurityNode node, final SecurityHistoryNode historyNode);

    /**
     * Returns the active currencies
     *
     * @return set of active currencies
     */
    public Set<CurrencyNode> getActiveCurrencies();

    public List<CurrencyNode> getCurrencies();

    public CurrencyNode getCurrencyByUuid(final String uuid);

    public SecurityNode getSecurityByUuid(final String uuid);

    public ExchangeRate getExchangeNode(final String rateId);

    public ExchangeRate getExchangeRateByUuid(final String uuid);

    public List<SecurityNode> getSecurities();

    /**
     * Call after a {@code ExchangeRateHistoryNode} has been removed.  This pushes the update
     * to the underlying database
     * @param rate ExchangeRate to update
     *
     * @return true if successful
     */
    public boolean removeExchangeRateHistory(final ExchangeRate rate);

    /**
     * Call after a {@code SecurityHistoryNode} has been removed.  This pushes the update
     * to the underlying database
     * @param node SecurityHistory to update
     * @param historyNode {@code SecurityHistoryNode} to remove
     *
     * @return true if successful
     */
    public boolean removeSecurityHistory(final SecurityNode node, final SecurityHistoryNode historyNode);

    public void addExchangeRate(ExchangeRate eRate);

    public boolean updateCommodityNode(final CommodityNode node);

    public List<ExchangeRate> getExchangeRates();
}