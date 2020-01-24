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

import java.math.BigDecimal;
import java.util.logging.Logger;

import javax.persistence.Entity;

/**
 * Class for representing currency nodes.
 * 
 * @author Craig Cavanaugh
 */
@Entity
public class CurrencyNode extends CommodityNode {

    private transient ExchangeRateDAO exchangeRateDAO;

    public CurrencyNode() {
    }

    /**
     * Returns the {@code ExchangeRateDAO}.
     *
     * @return the exchangeRateStore
     */
    synchronized private ExchangeRateDAO getExchangeRateDAO() {
        return exchangeRateDAO;
    }

    /**
     * Sets the {@code ExchangeRateDAO}.
     *
     * @param exchangeRateStore the exchangeRateStore to set
     */
    synchronized void setExchangeRateDAO(final ExchangeRateDAO exchangeRateStore) {
        this.exchangeRateDAO = exchangeRateStore;
    }

    /**
     * Returns an exchange rate given a currency to convert to.
     * 
     * @param exchangeCurrency currency to convert to
     * @return exchange rate
     */
    synchronized public BigDecimal getExchangeRate(final CurrencyNode exchangeCurrency) {

        if (exchangeCurrency == null) {
            Logger.getLogger(CurrencyNode.class.getName()).severe("exchangeCurrency was null");
            return BigDecimal.ONE;
        }

        if (exchangeCurrency.equals(this)) {
            return BigDecimal.ONE;
        }

        BigDecimal rate = getExchangeRateDAO().getExchangeRateNode(this, exchangeCurrency).getRate();

        if (getSymbol().compareToIgnoreCase(exchangeCurrency.getSymbol()) < 0) {
            rate = BigDecimal.ONE.divide(rate, MathConstants.mathContext);
        }

        return rate;
    }
}
