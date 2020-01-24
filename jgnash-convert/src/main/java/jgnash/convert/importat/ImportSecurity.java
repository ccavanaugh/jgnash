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
package jgnash.convert.importat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import jgnash.engine.SecurityNode;

/**
 * Security Import Object
 *
 * @author Craig Cavanaugh
 */
public class ImportSecurity {

    private String ticker;
    private String securityName;
    private BigDecimal unitPrice;
    private LocalDate localDate;
    private String id;
    public String idType;
    private String currency;
    private BigDecimal currencyRate;

    private SecurityNode securityNode;

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append("ticker: ").append(getTicker()).append('\n');
        b.append("securityName: ").append(securityName).append('\n');
        b.append("unitPrice: ").append(unitPrice).append('\n');
        b.append("localDate: ").append(localDate).append('\n');

        if (id != null) {
            b.append("id: ").append(id).append('\n');
        }

        if (idType != null) {
            b.append("idType: ").append(idType).append('\n');
        }

        getCurrency().ifPresent(currency -> b.append("currency: ").append(currency).append('\n'));

        getCurrencyRate().ifPresent(rate -> b.append("currencyRate: ").append(rate).append('\n'));

        return b.toString();
    }

    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    public void setId(String id) {
        this.id = id;
    }

    Optional<String> getSecurityName() {
        return Optional.ofNullable(securityName);
    }

    public void setSecurityName(final String securityName) {
        this.securityName = securityName;
    }

    Optional<BigDecimal> getUnitPrice() {
        return Optional.ofNullable(unitPrice);
    }

    public void setUnitPrice(final BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Optional<LocalDate> getLocalDate() {
        return Optional.ofNullable(localDate);
    }

    public void setLocalDate(final LocalDate localDate) {
        this.localDate = localDate;
    }

    public void setCurrencyRate(final BigDecimal unitPrice) {
        this.currencyRate = unitPrice;
    }

    public Optional<BigDecimal> getCurrencyRate() {
        return Optional.ofNullable(currencyRate);
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public Optional<String> getCurrency() {
        return Optional.ofNullable(currency);
    }

    /**
     * Reference to the security node linked to this imported security node
     */
    public SecurityNode getSecurityNode() {
        return securityNode;
    }

    public void setSecurityNode(SecurityNode securityNode) {
        this.securityNode = securityNode;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(final String ticker) {
        this.ticker = ticker;
    }
}
