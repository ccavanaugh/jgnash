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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PostLoad;

import jgnash.util.Nullable;

/**
 * Exchange rate object.
 *
 * @author Craig Cavanaugh
 */
@Entity
public class ExchangeRate extends StoredObject {

    @JoinTable
    @OrderBy("date")    //applying a sort order prevents refresh issues
    @OneToMany(cascade = {CascadeType.ALL})
    private final Set<ExchangeRateHistoryNode> historyNodes = new HashSet<>();

    /**
     * Cache the last exchange rate.
     */
    transient private BigDecimal lastRate;

    /**
     * Identifier for the ExchangeRate object.
     */
    private String rateId;

    /**
     * ReadWrite lock.
     */
    private transient ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * No argument constructor for reflection purposes.
     * <p>
     * <b>Do not use to create a new instance</b>
     */
    @SuppressWarnings("unused")
    public ExchangeRate() {
    }

    ExchangeRate(final String rateId) {
        this.rateId = rateId;
    }

    public boolean contains(final ExchangeRateHistoryNode node) {

        lock.readLock().lock();

        try {
            return historyNodes.contains(node);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean contains(final LocalDate localDate) {

        lock.readLock().lock();

        boolean result = false;

        try {
            for (final ExchangeRateHistoryNode node : historyNodes) {
                if (localDate.compareTo(node.getLocalDate()) == 0) {
                    result = true;
                    break;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return result;
    }

    public List<ExchangeRateHistoryNode> getHistory() {
        // return a defensive copy
        List<ExchangeRateHistoryNode> nodes = new ArrayList<>(historyNodes);
        Collections.sort(nodes);

        return nodes;
    }

    boolean addHistoryNode(final ExchangeRateHistoryNode node) {
        boolean result = false;

        lock.writeLock().lock();

        try {
            historyNodes.add(node);

            lastRate = null; // force an update

            result = true;
        } catch (final Exception ex) {
            Logger.getLogger(ExchangeRate.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        } finally {
            lock.writeLock().unlock();
        }

        return result;
    }

    @Nullable
    ExchangeRateHistoryNode getHistory(final LocalDate localDate) {
        ExchangeRateHistoryNode node = null;

        lock.readLock().lock();

        try {
            for (final ExchangeRateHistoryNode historyNode : historyNodes) {
                if (localDate.compareTo(historyNode.getLocalDate()) == 0) {
                    node = historyNode;
                    break;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return node;
    }

    boolean removeHistoryNode(final ExchangeRateHistoryNode hNode) {

        lock.writeLock().lock();

        try {
            final boolean result = historyNodes.remove(hNode);

            if (result) {
                lastRate = null; // force an update
            }

            return result;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getRateId() {
        return rateId;
    }

    public BigDecimal getRate() {
        lock.readLock().lock();

        try {
            if (lastRate == null) {
                if (!historyNodes.isEmpty()) {

                    List<ExchangeRateHistoryNode> nodes = getHistory();

                    lastRate = nodes.get(nodes.size() - 1).getRate();
                } else {
                    lastRate = BigDecimal.ONE;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return lastRate;
    }

    /**
     * Returns the exchange rate for a given {@code LocalDate}.
     * <p>
     * If a rate has not be set, {@code BigDecimal.ZERO} is returned
     *
     * @param localDate {@code LocalDate} for exchange
     * @return the exchange rate if known, otherwise {@code BigDecimal.ZERO}
     */
    BigDecimal getRate(final LocalDate localDate) {
        lock.readLock().lock();

        BigDecimal rate = BigDecimal.ZERO;

        try {
            for (ExchangeRateHistoryNode historyNode : historyNodes) {
                if (localDate.equals(historyNode.getLocalDate())) {
                    rate = historyNode.getRate();
                    break;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return rate;
    }

    @Override
    public boolean equals(final Object other) {
        return this == other || other instanceof ExchangeRate && rateId.equals(((ExchangeRate) other).rateId);
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 67 + rateId.hashCode();
    }

    /**
     * Required by XStream for proper initialization.
     *
     * @return Properly initialized ExchangeRate
     */
    protected Object readResolve() {
        postLoad();
        return this;
    }

    @PostLoad
    private void postLoad() {
        lock = new ReentrantReadWriteLock(true);
    }
}
