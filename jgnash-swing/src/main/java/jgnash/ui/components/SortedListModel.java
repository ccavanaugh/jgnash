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
package jgnash.ui.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractListModel;

/**
 * A Sorted list model
 * 
 * @author Craig Cavanaugh
 *
 */

public class SortedListModel<E extends Comparable<? super E>> extends AbstractListModel<E> {

    private static final long serialVersionUID = -5321417365496715895L;

    final private List<E> list = new ArrayList<>();

    public SortedListModel() {
    }

    public SortedListModel(final Collection<E> list) {
        add(list);
    }

    /**
     * Returns the number of components in this list.
     * <p/>
     * This method is identical to <tt>size()</tt>, which implements the <tt>List</tt> interface defined in the 1.2
     * Collections framework. This method exists in conjunction with <tt>setSize()</tt> so that "size" is identifiable
     * as a JavaBean property.
     * 
     * @return the number of components in this list.
     * @see ArrayList#size()
     */
    @Override
    public int getSize() {
        return list.size();
    }

    /**
     * Returns the component at the specified index. <blockquote> <b>Note:</b> Although this method is not deprecated,
     * the preferred method to use is <tt>get(int)</tt>, which implements the <tt>List</tt> interface defined in the 1.2
     * Collections framework. </blockquote>
     * 
     * @param index an index into this list.
     * @return the component at the specified index.
     * @throws ArrayIndexOutOfBoundsException if the <tt>index</tt> is negative or not less than the current size of
     *         this list given.
     * @see ArrayList#get(int)
     */
    @Override
    public E getElementAt(final int index) {
        return list.get(index);
    }

    /**
     * @param index index for the object
     * @return object at the given index
     * @see ArrayList#get(int)
     */
    public E get(final int index) {
        return list.get(index);
    }

    /**
     * Add a list of objects, does not fire a notification
     * 
     * @param aList collection of objects to add
     */
    final void add(final Collection<E> aList) {
        synchronized (list) {
            list.addAll(aList);
            Collections.sort(list);
        }
    }

    /**
     * Tests if the specified object is a component in this list.
     * 
     * @param elem an object.
     * @return <code>true</code> if the specified object is the same as a component in this list
     * @see Vector#contains(Object)
     */
    public boolean contains(final E elem) {
        return list.contains(elem);
    }

    /**
     * Searches for the first occurrence of the given argument.
     * 
     * @param elem an object.
     * @return the index of the first occurrence of the argument in this list; returns <code>-1</code> if the object is
     *         not found.
     * @see Vector#indexOf(Object)
     */
    int indexOf(final E elem) {
        return list.indexOf(elem);
    }

    /**
     * Adds the specified component to the end of this list.
     * 
     * @param obj the component to be added.
     * @see Vector#addElement(Object)
     */
    public void addElement(final E obj) {
        int i = Collections.binarySearch(list, obj);
        if (i < 0) {
            int index = -i - 1;
            list.add(index, obj);
            fireIntervalAdded(this, index, index);
        }
    }

    /**
     * Removes the first (lowest-indexed) occurrence of the argument from this list.
     * 
     * @param obj the component to be removed.
     * @return <code>true</code> if the argument was a component of this list; <code>false</code> otherwise.
     * @see ArrayList#remove(Object)
     */
    public boolean removeElement(final E obj) {
        int index = indexOf(obj);
        boolean rv = list.remove(obj);
        if (index >= 0) {
            fireIntervalRemoved(this, index, index);
        }
        return rv;
    }

    /**
     * Returns an array containing all of the elements in this list in the correct order.
     * <p/>
     * Throws an <tt>ArrayStoreException</tt> if the runtime type of the array a is not a supertype of the runtime type
     * of every element in this list.
     * 
     * @return an array containing the elements of the list.
     * @see ArrayList#toArray()
     */
    public Object[] toArray() {
        return list.toArray();
    }

    /**
     * Returns a copy of the list
     * 
     * @return copy of the internal list
     */
    public List<E> asList() {
        return new ArrayList<>(list);
    }

    /**
     * Removes the element at the specified position in this list. Returns the element that was removed from the list.
     * <p/>
     * Throws an <tt>ArrayIndexOutOfBoundsException</tt> if the index is out of range (index &lt; 0 || index &gt;=
     * size()).
     * 
     * @param index the index of the element to removed.
     * @return the object that was removed
     */
    public Object remove(final int index) {
        Object rv = list.get(index);
        list.remove(index);
        fireIntervalRemoved(this, index, index);
        return rv;
    }

    /**
     * Removes all of the elements from this list. The list will be empty after this call returns (unless it throws an
     * exception).
     */
    public void clear() {
        int index1 = list.size() - 1;
        list.clear();
        if (index1 >= 0) {
            fireIntervalRemoved(this, 0, index1);
        }
    }

    /**
     * Resorts the list and fires a notification
     */
    public void fireContentsChanged() {
        Collections.sort(list);
        fireContentsChanged(this, 0, getSize() - 1);
    }
}
