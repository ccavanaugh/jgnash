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
package jgnash.util.prefs;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

/**
 * Map based Preferences implementation.  Preferences must be persisted using the
 * <code>exportSubtree(OutputStream os)</code> and <code>Preferences.importPreferences(InputStream)</code>
 * methods.
 *
 * @author Craig Cavanaugh
 * @version $Id: MapBasedPreferences.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
class MapBasedPreferences extends AbstractPreferences {

    private final boolean isUserNode;

    private Map<String, String> map = new HashMap<String, String>();

    protected MapBasedPreferences(final MapBasedPreferences parent, final String name, final boolean isUserNode) {
        super(parent, name);
        this.isUserNode = isUserNode;
        newNode = true;
    }

    /**
     * Returns true if this node was created as a user node.
     */
    @Override
    public boolean isUserNode() {
        return isUserNode;
    }

    /**
     * Put the given key-value association into this preference node.  It is
     * guaranteed that <tt>key</tt> and <tt>value</tt> are non-null and of
     * legal length.  Also, it is guaranteed that this node has not been
     * removed.  (The implementor needn't check for any of these things.)
     *
     * <p>This method is invoked with the lock on this node held.
     */
    @Override
    protected void putSpi(final String key, final String value) {
        map.put(key, value);
    }

    /**
     * Return the value associated with the specified key at this preference
     * node, or <tt>null</tt> if there is no association for this key, or the
     * association cannot be determined at this time.  It is guaranteed that
     * <tt>key</tt> is non-null.  Also, it is guaranteed that this node has
     * not been removed.  (The implementor needn't check for either of these
     * things.)
     *
     * <p> Generally speaking, this method should not throw an exception
     * under any circumstances.  If, however, if it does throw an exception,
     * the exception will be intercepted and treated as a <tt>null</tt>
     * return value.
     *
     * <p>This method is invoked with the lock on this node held.
     *
     * @return the value associated with the specified key at this preference
     *         node, or <tt>null</tt> if there is no association for this
     *         key, or the association cannot be determined at this time.
     */
    @Override
    protected String getSpi(final String key) {
        return map.get(key);
    }

    /**
     * Remove the association (if any) for the specified key at this
     * preference node.  It is guaranteed that <tt>key</tt> is non-null.
     * Also, it is guaranteed that this node has not been removed.
     * (The implementor needn't check for either of these things.)
     *
     * <p>This method is invoked with the lock on this node held.
     */
    @Override
    protected void removeSpi(final String key) {
        map.remove(key);
    }

    /**
     * Removes this preference node, invalidating it and any preferences that
     * it contains.  The named child will have no descendants at the time this
     * invocation is made (i.e., the {@link java.util.prefs.Preferences#removeNode()} method
     * invokes this method repeatedly in a bottom-up fashion, removing each of
     * a node's descendants before removing the node itself).
     *
     * <p>This method is invoked with the lock held on this node and its
     * parent (and all ancestors that are being removed as a
     * result of a single invocation to {@link java.util.prefs.Preferences#removeNode()}).
     *
     * <p>The removal of a node needn't become persistent until the
     * <tt>flush</tt> method is invoked on this node (or an ancestor).
     *
     * <p>If this node throws a <tt>BackingStoreException</tt>, the exception
     * will propagate out beyond the enclosing {@link #removeNode()}
     * invocation.
     *
     * @throws java.util.prefs.BackingStoreException
     *          if this operation cannot be completed
     *          due to a failure in the backing store, or inability to
     *          communicate with it.
     */
    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        map.clear();
    }

    /**
     * Returns all of the keys that have an associated value in this
     * preference node.  (The returned array will be of size zero if
     * this node has no preferences.)  It is guaranteed that this node has not
     * been removed.
     *
     * <p>This method is invoked with the lock on this node held.
     *
     * <p>If this node throws a <tt>BackingStoreException</tt>, the exception
     * will propagate out beyond the enclosing {@link #keys()} invocation.
     *
     * @return an array of the keys that have an associated value in this
     *         preference node.
     * @throws java.util.prefs.BackingStoreException
     *          if this operation cannot be completed
     *          due to a failure in the backing store, or inability to
     *          communicate with it.
     */
    @Override
    protected String[] keysSpi() throws BackingStoreException {
        return map.keySet().toArray(new String[map.size()]);
    }

    /**
     * Returns the names of the children of this preference node.  (The
     * returned array will be of size zero if this node has no children.)
     * This method need not return the names of any nodes already cached,
     * but may do so without harm.
     *
     * <p>This method is invoked with the lock on this node held.
     *
     * <p>If this node throws a <tt>BackingStoreException</tt>, the exception
     * will propagate out beyond the enclosing {@link #childrenNames()}
     * invocation.
     *
     * @return an array containing the names of the children of this
     *         preference node.
     * @throws java.util.prefs.BackingStoreException
     *          if this operation cannot be completed
     *          due to a failure in the backing store, or inability to
     *          communicate with it.
     */
    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        return new String[0];
    }

    /**
     * Returns the named child of this preference node, creating it if it does
     * not already exist.  It is guaranteed that <tt>name</tt> is non-null,
     * non-empty, does not contain the slash character ('/'), and is no longer
     * than {@link #MAX_NAME_LENGTH} characters.  Also, it is guaranteed that
     * this node has not been removed.  (The implementor needn't check for any
     * of these things.)
     *
     * <p>Finally, it is guaranteed that the named node has not been returned
     * by a previous invocation of this method or {@link #getChild(String)}
     * after the last time that it was removed.  In other words, a cached
     * value will always be used in preference to invoking this method.
     * Subclasses need not maintain their own cache of previously returned
     * children.
     *
     * <p>The implementer must ensure that the returned node has not been
     * removed.  If a like-named child of this node was previously removed, the
     * implementer must return a newly constructed <tt>AbstractPreferences</tt>
     * node; once removed, an <tt>AbstractPreferences</tt> node
     * cannot be "resuscitated."
     *
     * <p>If this method causes a node to be created, this node is not
     * guaranteed to be persistent until the <tt>flush</tt> method is
     * invoked on this node or one of its ancestors (or descendants).
     *
     * <p>This method is invoked with the lock on this node held.
     *
     * @param name The name of the child node to return, relative to
     *             this preference node.
     * @return The named child node.
     */
    @Override
    protected AbstractPreferences childSpi(final String name) {
        return new MapBasedPreferences(this, name, isUserNode);
    }

    /**
     * This method is invoked with this node locked.  The contract of this
     * method is to synchronize any cached preferences stored at this node
     * with any stored in the backing store.  (It is perfectly possible that
     * this node does not exist on the backing store, either because it has
     * been deleted by another VM, or because it has not yet been created.)
     * Note that this method should <i>not</i> synchronize the preferences in
     * any subnodes of this node.  If the backing store naturally syncs an
     * entire subtree at once, the implementer is encouraged to override
     * sync(), rather than merely overriding this method.
     *
     * <p>If this node throws a <tt>BackingStoreException</tt>, the exception
     * will propagate out beyond the enclosing {@link #sync()} invocation.
     *
     * @throws java.util.prefs.BackingStoreException
     *          if this operation cannot be completed
     *          due to a failure in the backing store, or inability to
     *          communicate with it.
     */
    @Override
    protected void syncSpi() throws BackingStoreException {

    }

    /**
     * This method is invoked with this node locked.  The contract of this
     * method is to force any cached changes in the contents of this
     * preference node to the backing store, guaranteeing their persistence.
     * (It is perfectly possible that this node does not exist on the backing
     * store, either because it has been deleted by another VM, or because it
     * has not yet been created.)  Note that this method should <i>not</i>
     * flush the preferences in any subnodes of this node.  If the backing
     * store naturally flushes an entire subtree at once, the implementer is
     * encouraged to override flush(), rather than merely overriding this
     * method.
     *
     * <p>If this node throws a <tt>BackingStoreException</tt>, the exception
     * will propagate out beyond the enclosing {@link #flush()} invocation.
     *
     * @throws java.util.prefs.BackingStoreException
     *          if this operation cannot be completed
     *          due to a failure in the backing store, or inability to
     *          communicate with it.
     */
    @Override
    protected void flushSpi() throws BackingStoreException {

    }
}
