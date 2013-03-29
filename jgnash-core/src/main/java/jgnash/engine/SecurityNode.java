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

import java.io.ObjectStreamException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.persistence.*;

/**
 * Security Node
 * <p/>
 * The last market price is cached to improve performance
 *
 * @author Craig Cavanaugh
 */
@Entity
public class SecurityNode extends CommodityNode {

    private static final long serialVersionUID = -8377663762619941498L;

    @ManyToOne
    private CurrencyNode reportedCurrency;

    /**
     * The currency that security values are reported in
     */
    @Enumerated(EnumType.STRING)
    private QuoteSource quoteSource = QuoteSource.NONE;

    /**
     * ISIN or CUSIP.  Used for OFX and quote downloads
     */
    private String isin;

    @ElementCollection
    private List<SecurityHistoryNode> historyNodes = new ArrayList<>();

    private transient ReadWriteLock lock;

    public SecurityNode() {
        lock = new ReentrantReadWriteLock(true);
    }

    public SecurityNode(final CurrencyNode node) {
        this();
        setReportedCurrencyNode(node);
    }

    private ReadWriteLock getLock() {
        return lock;
    }

    /**
     * Prefix is deferred to the reported currency
     *
     * @return prefix of the reported currency
     */
    @Override
    public String getPrefix() {
        return reportedCurrency.getPrefix();
    }

    @Override
    public void setPrefix(final String ignored) {
    }

    /**
     * Suffix is deferred to the reported currency
     *
     * @return suffix of the reported currency
     */
    @Override
    public String getSuffix() {
        return reportedCurrency.getSuffix();
    }

    @Override
    public void setSuffix(final String ignored) {
    }

    /**
     * Returns the quote download source
     *
     * @return quote download source
     */
    public QuoteSource getQuoteSource() {
        return quoteSource;
    }

    /**
     * Sets the quote download source
     *
     * @param source QuoteSource to use
     */
    public void setQuoteSource(final QuoteSource source) {
        quoteSource = source;
    }

    public String getISIN() {
        return isin;
    }

    public void setISIN(final String isin) {
        this.isin = isin;
    }

    /**
     * Set the CurrencyNode that security histories are reported in
     *
     * @param node reported CurrencyNode
     */
    public void setReportedCurrencyNode(final CurrencyNode node) {
        reportedCurrency = node;
    }

    /**
     * Returns the CurrencyNode that security histories are reported in
     *
     * @return reported CurrencyNode
     */
    public CurrencyNode getReportedCurrencyNode() {
        return reportedCurrency;
    }

    boolean addHistoryNode(final SecurityHistoryNode node) {

        boolean result = false;

        Lock l = getLock().writeLock();
        l.lock();

        try {
            int index = Collections.binarySearch(historyNodes, node);

            if (index < 0) {
                historyNodes.add(-index - 1, node);
            } else {
                historyNodes.set(index, node);
            }

            result = true;
        } finally {
            l.unlock();
        }

        return result;
    }

    boolean removeHistoryNode(final SecurityHistoryNode hNode) {

        Lock l = getLock().writeLock();
        l.lock();

        boolean result = false;

        try {
            result = historyNodes.remove(hNode);
        } finally {
            l.unlock();
        }

        return result;
    }

    private SecurityHistoryNode getLastHistoryNode() {
        getLock().readLock().lock();

        try {
            SecurityHistoryNode node = null;

            if (!historyNodes.isEmpty()) {
                node = historyNodes.get(historyNodes.size() - 1);
            }

            return node;
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * Get a copy of SecurityHistoryNodes for this security
     *
     * @return Returns a shallow copy of the history nodes to protect against
     *         modification
     */
    public List<SecurityHistoryNode> getHistoryNodes() {
        return new ArrayList<>(historyNodes);
    }

    SecurityHistoryNode getHistoryNode(final Date date) {
        Date testDate = DateUtils.trimDate(date);

        getLock().readLock().lock();

        try {

            SecurityHistoryNode hNode = null;

            for (int i = historyNodes.size() - 1; i >= 0; i--) {
                SecurityHistoryNode node = historyNodes.get(i);

                if (testDate.compareTo(node.getDate()) >= 0) {
                    hNode = node;
                    break;
                }
            }

            if (hNode == null) {
                hNode = getLastHistoryNode();
            }

            return hNode;
        } finally {
            getLock().readLock().unlock();
        }
    }

    BigDecimal getMarketPrice(final Date date) {
        BigDecimal marketPrice = BigDecimal.ZERO;

        Date testDate = DateUtils.trimDate(date);

        getLock().readLock().lock();

        try {

            for (SecurityHistoryNode node : historyNodes) {
                if (node.getDate().getTime() <= testDate.getTime()) {
                    marketPrice = node.getPrice();
                } else {
                    break;
                }
            }

            return marketPrice;
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * Returns the latest market price exchanged to the specified currency
     *
     * @param date date to find closest matching rate without exceeding
     * @param node currency to exchange to
     * @return latest market price
     */
    public BigDecimal getMarketPrice(final Date date, final CurrencyNode node) {
        return getMarketPrice(date).multiply(getReportedCurrencyNode().getExchangeRate(node));
    }

    /**
     * Return a clone of this security node Security history is not cloned
     *
     * @return clone of this SecurityNode with history nodes
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        SecurityNode node = (SecurityNode) super.clone();
        node.historyNodes = new ArrayList<>();
        node.lock = new ReentrantReadWriteLock(true);

        return node;
    }

    private Object readResolve() throws ObjectStreamException {
        lock = new ReentrantReadWriteLock(true);
        return this;
    }

    @PostLoad
    @SuppressWarnings("unused")
    private void postLoad() {
        lock = new ReentrantReadWriteLock(true);
    }
}
