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
package jgnash.engine;

import jgnash.engine.dao.CommodityDAO;

/**
 * DAO for exchange rate access.
 *
 * @author Craig Cavanaugh
 *
 */
class ExchangeRateDAO {

    private final CommodityDAO commodityDAO;

    ExchangeRateDAO(final CommodityDAO commodityDAO) {
        this.commodityDAO = commodityDAO;
    }

    ExchangeRate getExchangeRateNode(final CurrencyNode baseCurrency, final CurrencyNode exchangeCurrency) {
        if (baseCurrency.equals(exchangeCurrency)) {
            return null;
        }

        final String rateId = Engine.buildExchangeRateId(baseCurrency, exchangeCurrency);

        ExchangeRate node = commodityDAO.getExchangeNode(rateId);

        if (node == null) {
            node = new ExchangeRate(Engine.buildExchangeRateId(baseCurrency, exchangeCurrency));
            commodityDAO.addExchangeRate(node);
        }

        return node;
    }
}
