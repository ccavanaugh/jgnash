/**
 * Copyright (c) 2006-2009, Alexander Potochkin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of the JXLayer project nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jdesktop.jxlayer.plaf.ext;

import javax.swing.JLayer;
import org.jdesktop.jxlayer.plaf.AbstractBufferedLayerUI;
import org.jdesktop.jxlayer.plaf.effect.LayerEffect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * An implementation of the {@code BufferedLayerUI} which provides
 * a lightweight disabling for the content of its {@link JXLayer}.
 * This allows temporarily blocking a part of the interface
 * with all it subcomponents, it is also useful when some kind of action
 * is in progress, e.g. reading data from a database.
 * <p/>
 * When {@code true} is passed to the {@link #setLocked(boolean)},
 * the {@code JXLayer} of this {@code LockableLayerUI} becomes "locked".
 * It sets the "wait" mouse cursor and stops reacting
 * on mouse, keyboard and focus events.
 * <p/>
 * If {@code setLocked(boolean)} is called with {@code false} parameter
 * after that, the {@code JXLayer}, together with all its subcomponents,
 * gets back to live.
 * <p/>
 * Subclasses usually override {@link #paintLayer(Graphics2D,JXLayer)} or
 * {@link #getLayerEffects(JXLayer)} to implement some visual effects
 * when {@code JXLayer} is in locked state.
 * <p/>
 * Here is an example of using {@code LockableLayerUI}:
 * <pre>
 * JComponent myComponent = getMyComponent(); // just any component
 * <p/>
 * LockableLayerUI lockableUI = new LockableLayerUI();
 * JXLayer&lt;JComponent&gt; layer = new JXLayer&lt;JComponent&gt;(myComponent, lockableUI);
 * <p/>
 * // locking the layer, use lockableUI.setLocked(false) to unlock
 * lockableUI.setLocked(true);
 * <p/>
 * // add the layer to a frame or a panel, like any other component
 * frame.add(layer);
 * </pre>
 * The LockableDemo is
 * <a href="https://jxlayer.dev.java.net/source/browse/jxlayer/trunk/src/demo/org/jdesktop/jxlayer/demo/LockableDemo.java?view=markup">available</a>
 */
public class LockableUI extends AbstractBufferedLayerUI<JComponent> {
   
	private static final long serialVersionUID = 1L;
	private boolean isLocked;
    private Component recentFocusOwner;
    private Cursor lockedCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    private LayerEffect[] lockedEffects = new LayerEffect[0];
    private final FocusListener focusListener = new FocusListener() {
        public void focusGained(FocusEvent e) {
            // we don't want extra repaintings
            // when focus comes from another window
            if (e.getOppositeComponent() != null) {
                setDirty(true);
            }
        }

        public void focusLost(FocusEvent e) {
        }
    };

    /**
     * Creates a new instance of LockableUI
     */
    public LockableUI() {
        this((LayerEffect[]) null);
    }

    /**
     * Creates a new instance of LockableUI,
     * passed lockedEffects will be used for when this UI in the locked state
     *
     * @param lockedEffects effects to be used when this UI is locked
     * @see #setLocked(boolean)
     * @see #setLockedEffects(LayerEffect...)
     */
    public LockableUI(LayerEffect... lockedEffects) {
        setLockedEffects(lockedEffects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        // we need to repaint the layer when it receives the focus
        // otherwise the focused component will have it on the buffer image
        c.addFocusListener(focusListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        c.removeFocusListener(focusListener);
    }

    /**
     * Returns {@code true} if this {@code LockableLayerUI}
     * is in locked state and all {@link JXLayer}'s mouse, keyboard and focuse events
     * are temporarily blocked, otherwise returns {@code false}.
     *
     * @return {@code true} if this {@code LockableLayerUI}
     *         is in locked state and all {@code JXLayer}'s mouse, keyboard and focuse events
     *         are temporarily blocked, otherwise returns {@code false}
     */
    public boolean isLocked() {
        return isLocked;
    }

    /**
     * If {@code isLocked} is {@code true} then all mouse, keyboard and focuse events
     * from the {@link JXLayer} of this {@code LockableLayerUI}
     * will be temporarily blocked.
     *
     * @param isLocked if {@code true} then all mouse, keyboard and focuse events
     *                 from the {@code JXLayer} of this {@code LockableLayerUI} will be temporarily blocked
     */
    public void setLocked(boolean isLocked) {
        if (isLocked != isLocked()) {
            if (getLayer() != null) {
                KeyboardFocusManager kfm 
                        = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                Component focusOwner = kfm.getPermanentFocusOwner();
                boolean isFocusInsideLayer = focusOwner != null
                        && SwingUtilities.isDescendingFrom(focusOwner, getLayer());
                if (isLocked) {
                    if (isFocusInsideLayer && 
                            kfm.getFocusedWindow() == 
                                    SwingUtilities.getWindowAncestor(getLayer())) {
                        recentFocusOwner = focusOwner;
                        // setDirty() will be called from the layer's focusListener
                        // when focus already left layer's view and hiding it
                        // in the paintLayer() won't mess the focus up
                        getLayer().requestFocusInWindow();
                    } else {
                        setDirty(true);
                    }
                    // the mouse cursor is set to the glassPane
                    getLayer().getGlassPane().setCursor(getLockedCursor());
                } else {
                    // show the view again
                    getLayer().getView().setVisible(true);
                    // restore the focus if it is still in the layer
                    if (isFocusInsideLayer && recentFocusOwner != null) {
                        recentFocusOwner.requestFocusInWindow();
                    }
                    recentFocusOwner = null;
                    getLayer().getGlassPane().setCursor(null);
                }
            }
            this.isLocked = isLocked;
            firePropertyChange("locked", !isLocked, isLocked);
        }
    }

    // If it is locked, the buffer image will be updated
    // only if the layer changes its size or setDirty(true) was called
    @Override
    protected boolean isIncrementalUpdate(JLayer<? extends JComponent> l) {
        return !isLocked();
    }

    @Override
    protected void paintLayer(Graphics2D g2, JLayer<? extends JComponent> l) {
        if (isLocked()) {
            // Note: this code will be called only if layer changes its size,
            // or setDirty(true) was called,
            // otherwise the previously created buffer is used
            l.getView().setVisible(true);
            l.paint(g2);
            // hide the layer's view component
            // this is the only way to disable key shortcuts
            // installed on its subcomponents
            l.getView().setVisible(false);
        } 
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);
        // if it is not locked, we need to paint the layer as is
        // otherwise we also need to paint it again;
        // if there are any glassPane's children  
        // they should be shown unfiltered 
        c.paint(g);
    }

    /**
     * Returns the mouse cursor to be used
     * by this {@code LockableLayerUI} when it locked state.
     *
     * @return the mouse cursor to be used
     *         by this {@code LockableLayerUI} when it locked state
     * @see #getLockedCursor()
     * @see #setLocked(boolean)
     */
    public Cursor getLockedCursor() {
        return lockedCursor;
    }

    /**
     * Sets the mouse cursor to be used
     * by this {@code LockableLayerUI} when it locked state.
     *
     * @param lockedCursor the mouse cursor to be used
     *                     by this {@code LockableLayerUI} when it locked state
     */
    public void setLockedCursor(Cursor lockedCursor) {
        Cursor oldCursor = getLockedCursor();
        this.lockedCursor = lockedCursor;
        firePropertyChange("lockedCursor", oldCursor, lockedCursor);
        if (isLocked()) {
            getLayer().getGlassPane().setCursor(lockedCursor);
        }
    }

    /**
     * Returns the effects to be used when this UI is locked.
     *
     * @return the effects to be used when this UI is locked
     * @see #setLocked(boolean)
     */
    public LayerEffect[] getLockedEffects() {
        LayerEffect[] result = new LayerEffect[lockedEffects.length];
        System.arraycopy(lockedEffects, 0, result, 0, result.length);
        return result;
    }

    /* 
    * This method returns the array of {@code LayerEffect}s
    * set using {@link #setLayerEffects(LayerEffect...)}
    * <p/>
    * If a {@code LockableUI} provides more extensive API
    * to support different {@code Effect}s depending on its state
    * or on the state of the passed {@code JXLayer},
    * this method should be overridden.
    *
    * @see #setLockedEffects (LayerEffect...)
    * @see #getLockedEffects()
    */
    protected LayerEffect[] getLockedEffects(JLayer<? extends JComponent> l) {
        return getLockedEffects();
    }

    /**
     * Sets the effects to be used when this UI is locked.
     *
     * @param lockedEffects the effects to be used when this UI is locked
     * @see #setLocked(boolean)
     */
    public void setLockedEffects(LayerEffect... lockedEffects) {
        LayerEffect[] oldEffects = getLockedEffects();
        if (lockedEffects == null) {
            lockedEffects = new LayerEffect[0];
        }
        for (LayerEffect effect : getLockedEffects()) {
            effect.removePropertyChangeListener(this);
        }
        this.lockedEffects = new LayerEffect[lockedEffects.length];
        System.arraycopy(lockedEffects, 0, this.lockedEffects, 0, lockedEffects.length);
        for (LayerEffect lockedEffect : lockedEffects) {
            lockedEffect.addPropertyChangeListener(this);
        }
        firePropertyChange("lockedEffects", oldEffects, lockedEffects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LayerEffect[] getLayerEffects(JLayer<? extends JComponent> l) {
        if (isLocked()) {
            return getLockedEffects(l);
        } else {
            return super.getLayerEffects(l);
        }
    }
}
