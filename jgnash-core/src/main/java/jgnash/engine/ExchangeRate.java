/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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

import java.io.ObjectStreamException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
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

/**
 * Exchange rate object
 *
 * @author Craig Cavanaugh
 */
@Entity
public class ExchangeRate extends StoredObject {

    @JoinTable
    @OrderBy("date")    //applying a sort order prevents refresh issues
    @OneToMany(cascade = {CascadeType.ALL})
    private Set<ExchangeRateHistoryNode> historyNodes = new HashSet<>();

    /**
     * Cache the last exchange rate
     */
    transient private BigDecimal lastRate;

    /**
     * Identifier for the ExchangeRate object
     */
    private String rateId;

    /**
     * ReadWrite lock
     */
    private transient ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * No argument constructor for reflection purposes.
     * <p/>
     * <b>Do not use to create a new instance</b>
     */
    @SuppressWarnings("unused")
    public ExchangeRate() {
    }

    ExchangeRate(final String rateId) {
        this.rateId = rateId;
    }

    private synchronized ReadWriteLock getLock() {
        return lock;
    }

    public boolean contains(final ExchangeRateHistoryNode node) {

        Lock l = getLock().readLock();
        l.lock();

        boolean result = false;

        try {
            result = historyNodes.contains(node);
        } finally {
            l.unlock();
        }

        return result;
    }

    public boolean contains(final Date date) {

        lock.readLock().lock();

        boolean result = false;

        final Date testDate = DateUtils.trimDate(date);

        try {
            for (ExchangeRateHistoryNode node : historyNodes) {
                if (testDate.compareTo(node.getDate()) == 0) {
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

        getLock().writeLock().lock();

        try {
            historyNodes.add(node);

            lastRate = null; // force an update

            result = true;
        } catch (final Exception ex) {
            Logger.getLogger(ExchangeRate.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        } finally {
            getLock().writeLock().unlock();
        }

        return result;
    }

    ExchangeRateHistoryNode getHistory(final Date date) {
        ExchangeRateHistoryNode node = null;

        final Date testDate = DateUtils.trimDate(date);

        lock.readLock().lock();

        try {
            for (ExchangeRateHistoryNode historyNode : historyNodes) {
                if (testDate.compareTo(historyNode.getDate()) == 0) {
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

        boolean result = false;

        Lock l = getLock().writeLock();
        l.lock();

        try {
            result = historyNodes.remove(hNode);

            if (result) {
                lastRate = null; // force an update

            }
        } finally {
            l.unlock();
        }

        return result;
    }

    public String getRateId() {
        return rateId;
    }

    public BigDecimal getRate() {
        getLock().readLock().lock();

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
            getLock().readLock().unlock();
        }

        return lastRate;
    }

    /**
     * Returns the exchange rate for a given date.
     * <p/>
     * If a rate has not be set, <code>BigDecimal.ZERO</code> is returned
     *
     * @param date Date for exchange
     * @return the exchange rate if known, otherwise <code>BigDecimal.ZERO</code>
     */
    public BigDecimal getRate(final Date date) {
        getLock().readLock().lock();

        BigDecimal rate = BigDecimal.ZERO;

        Date exchangeDate = DateUtils.trimDate(date);

        try {
            for (ExchangeRateHistoryNode historyNode : historyNodes) {
                if (exchangeDate.equals(historyNode.getDate())) {
                    rate = historyNode.getRate();
                    break;
                }
            }
        } finally {
            getLock().readLock().unlock();
        }

        return rate;
    }

    @Override
    public boolean equals(final Object other) {
        return this == other || other instanceof ExchangeRate && rateId.equals(((ExchangeRate) other).rateId);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return super.hashCode() * 67 + rateId.hashCode();
    }

    @SuppressWarnings("RedundantThrows")
    private Object readResolve() throws ObjectStreamException {
        postLoad();
        return this;
    }

    @PostLoad
    private void postLoad() {
        lock = new ReentrantReadWriteLock(true);
    }
}
