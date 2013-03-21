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
package jgnash.engine;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.ObjectStreamException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Class for representing currency nodes
 * 
 * @author Craig Cavanaugh
 */
@Entity
public class CurrencyNode extends CommodityNode {

    private static final long serialVersionUID = 1339921229356331512L;

    // unused, but left to keep file compatibility with prior releases
    @SuppressWarnings("unused")
    @Transient
    private Locale locale = Locale.getDefault();

    private transient ExchangeRateDAO exchangeRateDAO;

    public CurrencyNode() {
    }

    /**
     * @return the exchangeRateStore
     */
    synchronized private ExchangeRateDAO getExchangeRateDAO() {
        return exchangeRateDAO;
    }

    /**
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

    /**
     * Work around a db4o java 7 bug with Locale
     * 
     * @return this object with a valid locale
     * @throws ObjectStreamException exception
     */
    protected Object writeReplace() throws ObjectStreamException {
        locale = Locale.getDefault();
        return this;
    }

    /**
     * Work around a db4o java 7 bug with Locale
     * 
     * @return this object with a valid locale
     */
    protected Object readResolve() {
        locale = Locale.getDefault();
        return this;
    }
}
