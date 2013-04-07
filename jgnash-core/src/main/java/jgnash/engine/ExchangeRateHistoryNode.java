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

import jgnash.util.DateUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Exchange rate history node for a <code>ExchangeRate</code>.
 * <code>ExchangeRateHistoryNode</code> objects are immutable.
 *
 * @author Craig Cavanaugh
 */
@Entity
public class ExchangeRateHistoryNode implements Comparable<ExchangeRateHistoryNode>, Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @SuppressWarnings("unused")
    @GeneratedValue(strategy= GenerationType.TABLE)
    private long id;

    @Temporal(TemporalType.DATE)
    private Date date = DateUtils.today();

    @Column(precision = 20, scale = 8)
    private BigDecimal rate = BigDecimal.ZERO;

    /**
     * No argument constructor for reflection purposes.
     * <b>Do not use to create a new instance</b>
     */
    @SuppressWarnings("unused")
    ExchangeRateHistoryNode() {
    }

    /**
     * public constructor
     *
     * @param date date for this history node
     * @param rate exchange rate for the given date
     */
    ExchangeRateHistoryNode(final Date date, final BigDecimal rate) {
        assert date != null && rate != null;

        this.date = DateUtils.trimDate(date);
        this.rate = rate;
    }

    @Override
    public int compareTo(final ExchangeRateHistoryNode node) {
        return getDate().compareTo(node.getDate());
    }

    @Override
    public boolean equals(final Object other) {
        return this == other || other instanceof ExchangeRateHistoryNode && date.equals(((ExchangeRateHistoryNode) other).date);
    }

    public Date getDate() {
        return date;
    }

    public BigDecimal getRate() {
        return rate;
    }

    @Override
    public int hashCode() {
        return date.hashCode();
    }
}