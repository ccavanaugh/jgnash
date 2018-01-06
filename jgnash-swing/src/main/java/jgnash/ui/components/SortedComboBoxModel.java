/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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

import javax.swing.AbstractListModel;
import javax.swing.MutableComboBoxModel;

/**
 * A ComboBoxModel that maintains the list in a sorted order
 * 
 * @author Craig Cavanaugh
 *
 * @param <E> Model type
 */
public class SortedComboBoxModel<E extends Comparable<? super E>> extends AbstractListModel<E> implements MutableComboBoxModel<E> {

    private Object selectedObject;

    private List<E> list = new ArrayList<>();

    public SortedComboBoxModel() {
    }

    /**
     * Constructs a SortedComboBoxModel object initialized with an Collection of objects.
     * 
     * @param items an array of Object objects
     */
    public SortedComboBoxModel(final Collection<E> items) {
        list = new ArrayList<>(items);
        Collections.sort(list);

        if (getSize() > 0) {
            selectedObject = getElementAt(0);
        }
    }

    /*public void clear() {
        int size = list.size();
        list.clear();
        fireIntervalRemoved(this, 0, size - 1);
    }*/

    public void addAll(final Collection<E> items) {
        list.addAll(items);
        Collections.sort(list);
        fireContentsChanged(this, 0, list.size() - 1);
    }

    /**
     * Set the value of the selected item. The selected item may be null.
     * 
     * @param anObject The combo box value or null for no selection.
     */
    @Override
    public void setSelectedItem(final Object anObject) {
        if (selectedObject != null && !selectedObject.equals(anObject) || selectedObject == null && anObject != null) {
            selectedObject = anObject;
            fireContentsChanged(this, -1, -1);
        }
    }

    // implements javax.swing.ComboBoxModel
    @Override
    public Object getSelectedItem() {
        return selectedObject;
    }

    // implements javax.swing.ListModel
    @Override
    public final int getSize() {
        return list.size();
    }

    // implements javax.swing.ListModel
    @Override
    public final E getElementAt(final int index) {
        if (index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return null;
    }

    /**
     * Returns the index-position of the specified object in the list.
     * 
     * @param anObject object to find index for
     * @return an int representing the index position, where 0 is the first position
     */
    private int getIndexOf(final E anObject) {
        return list.indexOf(anObject);
    }

    void updateElement(final E anObject) {
        int index = getIndexOf(anObject);
        if (index >= 0) {
            fireContentsChanged(this, index, index);
        }
    }

    // adds and object in sorted order, does not fire a change
    private int add(final E anObject) {
        if (anObject != null) {
            list.add(anObject);
            Collections.sort(list);

            return list.indexOf(anObject);
        }
        return -1;
    }

    // implements javax.swing.MutableComboBoxModel
    @Override
    public void removeElementAt(final int index) {
        if (getElementAt(index) == selectedObject) {
            if (index == 0) {
                setSelectedItem(getSize() == 1 ? null : getElementAt(index + 1));
            } else {
                setSelectedItem(getElementAt(index - 1));
            }
        }

        list.remove(index);
        fireIntervalRemoved(this, index, index);
    }

    // implements javax.swing.MutableComboBoxModel
    @SuppressWarnings("unchecked")
    @Override
    public void removeElement(final Object anObject) {

        E object = (E) anObject;

        int index = list.indexOf(object);

        if (index >= 0) {
            list.remove(object);
            fireIntervalRemoved(this, index, index);
        }

        if (getSize() == 0) {
            selectedObject = null;
        }
    }

    /**
     * Empties the list.
     */
    void removeAllElements() {
        if (!list.isEmpty()) {
            int firstIndex = 0;
            int lastIndex = list.size() - 1;
            list.clear();
            selectedObject = null;
            fireIntervalRemoved(this, firstIndex, lastIndex);
        }
    }

    @Override   
    public void addElement(final E obj) {
        int index = add(obj);
        fireIntervalAdded(this, index, index);

        if (list.size() == 1 && selectedObject == null && obj != null) {
            setSelectedItem(obj);
        }
    }
        
    @Override    
    public void insertElementAt(final E obj, final int index) {
        list.add(obj);
        Collections.sort(list);

        fireContentsChanged(this, 0, list.size() - 1);
    }
}
