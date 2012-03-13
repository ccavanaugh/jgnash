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
package jgnash.engine;

import java.math.BigDecimal;

/**
 * Abstract class for representing a commodity
 * 
 * @author Craig Cavanaugh
 * @version $Id: CommodityNode.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public abstract class CommodityNode extends StoredObject implements Comparable<CommodityNode> {

    private static final long serialVersionUID = -8694706937407227601L;

    private String symbol;

    private byte scale = 2;

    private String prefix = "";

    private String suffix = "";

    private String description;

    public void setScale(byte scale) {
        this.scale = scale;
    }

    public byte getScale() {
        return scale;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }

    @Override
    public String toString() {
        if (description != null) {
            return new StringBuilder(symbol).append(" (").append(description).append(')').toString();
        }
        return symbol;
    }

    @Override
    public int compareTo(CommodityNode node) {
        return symbol.compareTo(node.symbol);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof CommodityNode && getUuid().equals(((CommodityNode) other).getUuid());
    }

    @Override
    public int hashCode() {
        return getUuid().hashCode();
    }

    /**
     * Determines if given node matches this node. The UUID is not used for comparison if equals fails
     * 
     * @param other CurrencyNode to compare against
     * @return true if objects match
     */
    public boolean matches(CommodityNode other) {
        boolean result = equals(other);

        if (!result) {
            result = getSymbol().equals(other.getSymbol());
        }
        return result;
    }

    /**
     * Rounds a supplied double to the correct scale and returns a BigDecimal
     * 
     * @param value double to round
     * @return properly scaled BigDecimal
     */
    public BigDecimal round(double value) {
        return new BigDecimal(value).setScale(scale, BigDecimal.ROUND_HALF_EVEN);
    }
}
