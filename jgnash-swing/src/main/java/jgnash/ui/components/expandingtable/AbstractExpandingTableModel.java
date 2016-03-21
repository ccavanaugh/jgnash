/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;


/**
 * An table model for hierarchical objects of the same type that can be expanded and collapsed
 *
 * @author Craig Cavanaugh
 */
public abstract class AbstractExpandingTableModel<E extends Comparable<? super E>> extends AbstractTableModel {

    /*
     * Remembers the account state (expanded)
     */
    private final Map<E, ExpandingTableNode<E>> objects = new HashMap<>();

    private transient List<ExpandingTableNode<E>> visibleObjects = new ArrayList<>();

    /**
     * A list of the key/objects in the model.  It is faster to maintain a list vs return a keySet from the map
     */
    private final transient Set<E> keys = new HashSet<>();

    /**
     * Read write lock for model access
     */
    protected final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock(false);

    /**
     * Expansion state preference key
     */
    private static final String EXPANSION_STATE = "ExpansionState";

    private final Preferences preferences = Preferences.userNodeForPackage(getClass());
    
    private SwingWorker<Void, Void> initWorker;

    protected AbstractExpandingTableModel() {
        initializeModel();
    }

    protected final void initializeModel() {
        initWorker = new InitializeModelWorker();
        initWorker.execute();
    }

    /**
     * Returns a {@code Collection} of objects loaded into the model at the time this method is called.
     *
     * @return {@code Collection} of objects
     */
    public Set<E> getObjects() {
        ReadLock readLock = rwl.readLock();
        readLock.lock();

        try {
            // return a defensive copy
            return new HashSet<>(keys);
        } finally {
            readLock.unlock();
        }
    }

    private String getExpansionState() {
        ReadLock readLock = rwl.readLock();
        readLock.lock();

        try {
            StringBuilder builder = new StringBuilder();

            ArrayList<ExpandingTableNode<E>> values = new ArrayList<>(objects.values());
            Collections.sort(values);

            for (ExpandingTableNode<E> node : values) {
                builder.append(node.isExpanded() ? '1' : '0');
            }

            return builder.toString();
        } finally {
            readLock.unlock();
        }
    }

    private void restoreExpansionState(final String state) {
        WriteLock writeLock = rwl.writeLock();
        writeLock.lock();

        try {
            if (state != null && state.length() == objects.size()) {

                ArrayList<ExpandingTableNode<E>> values = new ArrayList<>(objects.values());
                Collections.sort(values);

                for (int i = 0; i < state.length(); i++) {
                    values.get(i).setExpanded(state.charAt(i) == '1');
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void toggleExpansion(final E object) {
        WriteLock writeLock = rwl.writeLock();
        writeLock.lock();

        try {
            if (isParent(object)) {
                ExpandingTableNode<E> node = getNode(object);
                node.setExpanded(!node.isExpanded());

                preferences.put(EXPANSION_STATE, getExpansionState());

                fireNodeChanged();
            }
        } finally {
            writeLock.unlock();
        }
    }

    private ExpandingTableNode<E> getNode(final E object) {
        ReadLock readLock = rwl.readLock();
        readLock.lock();

        try {
            return objects.get(object);
        } finally {
            readLock.unlock();
        }
    }

    protected void clear() {
        WriteLock writeLock = rwl.writeLock();
        writeLock.lock();

        try {
            visibleObjects.clear();
            objects.clear();
            keys.clear();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Returns the index of the first visible occurrence of the specified element in this model,
     * or -1 if this list does not contain the element.
     *
     * @param object element to search for
     * @return the index of the first visible occurrence of the specified element in this list,
     *         or -1 if this list does not contain the element
     */
    public int indexOf(final E object) {
        ReadLock readLock = rwl.readLock();
        readLock.lock();

        try {
            int index = -1;

            for (ExpandingTableNode<E> n : visibleObjects) {
                if (n.getObject().equals(object)) {
                    index = visibleObjects.indexOf(n);
                    break;
                }
            }
            return index;
        } finally {
            readLock.unlock();
        }
    }

    public boolean isExpanded(final E object) {
        return getNode(object).isExpanded();
    }

    /**
     * @see javax.swing.table.TableModel#getRowCount()
     */
    @Override
    public int getRowCount() {
        return size();
    }

    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        throw new UnsupportedOperationException();
    }

    protected void addNode(final E object) {
        WriteLock writeLock = rwl.writeLock();
        writeLock.lock();

        try {
            objects.put(object, new ExpandingTableNode<>(object));
            keys.add(object);

            buildVisibleModel();

            fireTableDataChanged();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Override the default implementation to push the change onto the EDT
     *
     * @see AbstractTableModel#fireTableDataChanged()
     */
    @Override
    public void fireTableDataChanged() {

        // push the notification onto the end of the EDT
        EventQueue.invokeLater(AbstractExpandingTableModel.super::fireTableDataChanged);
    }

    /**
     * Force update of the visible model and notify listeners
     */
    protected final void fireNodeChanged() {
        WriteLock writeLock = rwl.writeLock();
        writeLock.lock();

        try {
            buildVisibleModel();

            fireTableDataChanged();
        } finally {
            writeLock.unlock();
        }
    }

    protected void removeNode(final E object) {
        WriteLock writeLock = rwl.writeLock();
        writeLock.lock();

        try {
            ExpandingTableNode<E> node = getNode(object);

            if (node != null) {
                objects.remove(object);
                visibleObjects.remove(node);
                keys.remove(object);
                fireTableDataChanged();
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Returns the object for the visible row.
     *
     * @param rowIndex visible row index
     * @return object
     */
    public E get(final int rowIndex) {
        return getExpandingTableNodeAt(rowIndex).getObject();
    }

    /**
     * Returns the number of visible objects in this model.
     *
     * @return the number of visible objects in this model
     * @see List#size()
     */
    private int size() {
        ReadLock readLock = rwl.readLock();
        readLock.lock();

        try {
            return visibleObjects.size();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns the encapsulating object wrapper for the visible row.
     *
     * @param rowIndex visible row index
     * @return node
     */
    protected ExpandingTableNode<E> getExpandingTableNodeAt(final int rowIndex) {
        // wait for the worker to complete
        try {                   
            initWorker.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(AbstractExpandingTableModel.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        ReadLock readLock = rwl.readLock();
        readLock.lock();

        try {            
            return visibleObjects.get(rowIndex);
        } finally {
            readLock.unlock();
        }
    }

    private void loadModel() {
        WriteLock writeLock = rwl.writeLock();
        writeLock.lock();

        try {
            keys.clear();
            objects.clear();
            visibleObjects.clear();

            for (E object : getModelObjects()) {
                objects.put(object, new ExpandingTableNode<>(object));
                keys.add(object);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private void loadVisibleModel(final E object, final List<ExpandingTableNode<E>> model) {
        WriteLock writeLock = rwl.writeLock();
        writeLock.lock();

        try {
            if (isVisible(object)) {

                ExpandingTableNode<E> node = getNode(object);

                // protect against a null node if the model does not contain a requested visible object
                if (node != null) {
                    model.add(node);
                }

                if (isParent(object)) {
                    for (E child : getChildren(object)) {
                        loadVisibleModel(child, model);
                    }
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    private synchronized void buildVisibleModel(final E root) {
        WriteLock writeLock = rwl.writeLock();
        writeLock.lock();

        try {
            List<ExpandingTableNode<E>> model = new ArrayList<>();

            for (E child : getChildren(root)) {
                loadVisibleModel(child, model);
            }

            visibleObjects = model;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Generates the visible object model.
     * <p>
     * If overridden, the overriding method must call the base method for the model to update correctly
     */
    private void buildVisibleModel() {
        buildVisibleModel(getRootObject());
    }

    /**
     * Determines the visible depth of the object in the hierarchical structure
     *
     * @param object child object
     * @return depth in the hierarchical structure
     */
    public abstract int getVisibleDepth(E object);

    /**
     * Determines if a given object has children
     *
     * @param object potential parent object
     * @return true if the object is a parent
     */
    public abstract boolean isParent(E object);

    /**
     * Returns a collection objects that are children of a supplied object. If the object does not have children, an
     * empty {@code Collection} should be returned instead of null.
     *
     * @param object parent object
     * @return collection of objects.
     */
    protected abstract Collection<E> getChildren(E object);

    /**
     * Returns the parent of the given object. May return null if the object does not have a parent
     *
     * @param object parent object
     * @return the parent of the given object.
     */
    protected abstract E getParent(E object);

    /**
     * Determines if the object should be visible. This is used to filter the displayed objects. The default
     * implementation checks for visibility based on default expansion state. Overriding implementations should return
     * false if the super returns false.
     *
     * @param object object to check
     * @return true if it should be visible
     */
    protected boolean isVisible(final E object) {
        E parent = getParent(object);

        return !(parent != null && getNode(parent) != null) || getNode(parent).isExpanded();
    }

    /**
     * Returns a collection of all objects that should be loaded into the model.
     * <p>
     * This method can be expensive as it is intended for constructing the model,
     * so usage should be minimal.
     *
     * @return collection of objects
     * @see AbstractExpandingTableModel#getObjects()
     */
    protected abstract Collection<E> getModelObjects();

    /**
     * Returns a search string for the given object. Used for keyboard selection and search of the hierarchical
     * structure. Typically, this will be the string returned for the first column.
     *
     * @param Object object to get search string
     * @return a search string
     */
    public abstract String getSearchString(E Object);

    /**
     * Returns the top level object for the hierarchical structure
     *
     * @return root object
     */
    protected abstract E getRootObject();

    private final class InitializeModelWorker extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() throws Exception {
            loadModel();
            buildVisibleModel();
            restoreExpansionState(preferences.get(EXPANSION_STATE, null));
            return null;
        }

        @Override
        protected void done() {
            fireNodeChanged();
        }
    }
}
