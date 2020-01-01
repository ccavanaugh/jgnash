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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.util;

import jgnash.engine.CommodityNode;

/**
 * Decorator around a CommodityNode to indicate it is in a locked/unlocked state.
 *
 * @author Craig Cavanaugh
 */
public class LockedCommodityNode<T extends CommodityNode> implements Comparable<LockedCommodityNode<T>> {

    private final boolean locked;
    private final T t;

    public LockedCommodityNode(@NotNull final T t, final boolean locked) {
        this.t = t;
        this.locked = locked;
    }

    @Override
    public String toString() {
        return t.toString();
    }

    public boolean isLocked() {
        return locked;
    }

    @Override
    public int compareTo(@NotNull final LockedCommodityNode<T> other) {
        return t.compareTo(other.t);
    }

    @SuppressWarnings("rawtypes")
	@Override
    public boolean equals(final Object o) {
        return this == o || o instanceof LockedCommodityNode && t.equals(((LockedCommodityNode) o).t);
    }

    public T getNode() {
        return t;
    }

    @Override
    public int hashCode() {
        int hash = t.hashCode();
        return 13 * hash + (locked ? 1 : 0);
    }
}
