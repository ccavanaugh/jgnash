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
package jgnash.ui.components.expandingtable;

/**
 * An object used to manage and persist objects with a hierarchical structure
 * 
 * @author Craig Cavanaugh
 *
 */
public class ExpandingTableNode<E extends Comparable<? super E>> implements Comparable<ExpandingTableNode<E>> {

    /**
     * Wrapped object
     */
    private E object;

    /*
     * By default, the object will be expanded
     */
    private boolean expanded = true;

    /**
     * Package protected constructor 
     * 
     * @param object Object to wrap
     */
    ExpandingTableNode(final E object) {
        if (object == null) {
            throw new IllegalArgumentException("The object may not be null");
        }

        this.object = object;
    }

    public E getObject() {
        return object;
    }

    public void setExpanded(final boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isExpanded() {
        return expanded;
    }

    /**
     * @see Comparable#compareTo(java.lang.Object) 
     */
    @Override
    public int compareTo(final ExpandingTableNode<E> o) {
        return object.compareTo(o.getObject());
    }

    /**
     * @see Object#hashCode() 
     */
    @Override
    public int hashCode() {
        return object.hashCode();
    }

    /**
     * @see Object#equals(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
	@Override
    public boolean equals(final Object other) {
        if (other instanceof ExpandingTableNode) {
            return object.equals(((ExpandingTableNode<E>) other).object);
        }

        return false;
    }
}
