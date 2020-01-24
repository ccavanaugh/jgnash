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

import jgnash.util.NotNull;
import jgnash.util.Nullable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Historical data for a {@code SecurityNode}.
 *
 * @author Craig Cavanaugh
 */
@Entity
@SequenceGenerator(name = "sequence", allocationSize = 10)
public class SecurityHistoryNode implements Comparable<SecurityHistoryNode>, Serializable {

    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(generator = "sequence", strategy = GenerationType.SEQUENCE)
    public long id;

    private LocalDate date = LocalDate.now();

    @Column(precision = 19, scale = 4)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(precision = 19, scale = 4)
    private BigDecimal high = BigDecimal.ZERO;

    @Column(precision = 19, scale = 4)
    private BigDecimal low = BigDecimal.ZERO;

    private long volume = 0;

    private transient BigDecimal adjustedPrice = null;

    /**
     * public no-argument constructor for reflection.
     */
    public SecurityHistoryNode() {
    }

    /**
     * Public constructor for creating a history node.
     *
     * @param date   date
     * @param price  closing price
     * @param volume closing volume
     * @param high   high price for the day
     * @param low    low price for the day
     */
    public SecurityHistoryNode(@NotNull final LocalDate date, @Nullable final BigDecimal price, final long volume,
                               @Nullable final BigDecimal high, @Nullable final BigDecimal low) {
        setDate(date);
        setPrice(price);
        setVolume(volume);
        setHigh(high);
        setLow(low);
    }

    private void setHigh(final BigDecimal high) {
        if (high != null) {
            this.high = high;
        }
    }

    private void setLow(final BigDecimal low) {
        if (low != null) {
            this.low = low;
        }
    }

    private void setVolume(final long volume) {
        this.volume = volume;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public long getVolume() {
        return volume;
    }

    void setDate(final @NotNull LocalDate localDate) {
        Objects.requireNonNull(localDate);
        this.date = localDate;
    }

    public LocalDate getLocalDate() {
        return date;
    }

    void setPrice(final BigDecimal price) {
        if (price != null) {
            this.price = price;
        }
    }

    public BigDecimal getPrice() {
        return price;
    }

    /**
     * The price adjusted for any splits or reverse splits.
     *
     * @return the adjusted price
     */
    public @NotNull
    BigDecimal getAdjustedPrice() {
        if (adjustedPrice != null) {
            return adjustedPrice;
        }
        return price;
    }

    /**
     * Adjusts the historical values given a multiplier.  To be used for handling
     * security splits and reverse splits.
     *
     * @param multiplier multiplier to be used for adjusting prices
     */
    void setAdjustmentMultiplier(@NotNull BigDecimal multiplier) {
        if (price != null) {
            adjustedPrice = price.multiply(multiplier, MathConstants.mathContext);
        }
    }

    /**
     * Compare using only the {@code LocalDate}
     *
     * @param node node to compare
     */
    @Override
    public int compareTo(@NotNull final SecurityHistoryNode node) {
        return getLocalDate().compareTo(node.getLocalDate());
    }

    /**
     * Equality is based on the {@code LocalDate} of the SecurityHistoryNode.
     *
     * @param obj the reference SecurityHistoryNode with which to compare.
     * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise.
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof SecurityHistoryNode && date
                .compareTo(((SecurityHistoryNode) obj).date) == 0;
    }

    @Override
    public int hashCode() {
        return date.hashCode();
    }
}