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
package jgnash.engine.dao;

import java.util.List;
import java.util.Set;

import jgnash.engine.CommodityNode;
import jgnash.engine.CurrencyNode;
import jgnash.engine.ExchangeRate;
import jgnash.engine.ExchangeRateHistoryNode;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;

/**
 * Commodity DAO Interface
 *
 * @author Craig Cavanaugh
 *
 */
public interface CommodityDAO {

    public boolean addCommodity(CommodityNode node);

    public boolean addExchangeRateHistory(final ExchangeRate rate, final ExchangeRateHistoryNode hNode);

    public boolean addSecurityHistory(final SecurityNode node, final SecurityHistoryNode hNode);

    /**
     * Returns the active currencies
     *
     * @return set of active currencies
     */
    public Set<CurrencyNode> getActiveCurrencies();

    public List<CurrencyNode> getCurrencies();

    public ExchangeRate getExchangeNode(final String rateId);

    public List<SecurityNode> getSecurities();

    public void refreshCommodityNode(CommodityNode node);

    public void refreshExchangeRate(ExchangeRate rate);

    public boolean removeExchangeRateHistory(final ExchangeRate rate, final ExchangeRateHistoryNode hNode);

    public boolean removeSecurityHistory(final SecurityNode node, final SecurityHistoryNode hNode);

    public void addExchangeRate(ExchangeRate eRate);

    public boolean updateCommodityNode(final CommodityNode node);

    public List<ExchangeRate> getExchangeRates();
}