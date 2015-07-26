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

package jgnash.ui;

import java.awt.AWTEvent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.plaf.LayerUI;

/**
 * The {@code AbstractLayerUI} provided default implementation for most of the
 * abstract methods in the {@link LayerUI} class. It takes care of the
 * management of {@code LayerItemListener}s and defines the hook method to
 * configure the {@code Graphics2D} instance specified in the
 * {@link #paint(Graphics, JComponent)} method. It also provides convenient
 * methods named {@code process<eventType>Event} to process the given class of
 * event.
 * <p/>
 * If state of the {@code AbstractLayerUI} is changed, call
 * {@link #setDirty(boolean)} with {@code true} as the parameter, it will
 * repaint all {@code JXLayer}s connected with this {@code AbstractLayerUI}
 *
 * @see JLayer#setUI(LayerUI)
 */
@SuppressWarnings("UnusedParameters")
public class AbstractLayerUI<V extends JComponent> extends LayerUI<V> {

    private boolean isDirty;

    @Override
    @SuppressWarnings("rawtypes")
    public void installUI(JComponent c) {
        super.installUI(c);
        ((JLayer) c).setLayerEventMask(getLayerEventMask());
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        ((JLayer) c).setLayerEventMask(0);
    }

    /**
     * Sets the "dirty bit". If {@code isDirty} is {@code true}, then the
     * {@code AbstractLayerUI} is considered dirty and it triggers the
     * repainting of the {@link JLayer}s this {@code AbstractLayerUI} it is set
     * to.
     *
     * @param isDirty whether this {@code AbstractLayerUI} is dirty or not.
     */
    private void setDirty(boolean isDirty) {
        boolean oldDirty = this.isDirty;
        this.isDirty = isDirty;
        firePropertyChange("dirty", oldDirty, isDirty);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <b>Note:</b> It is rarely necessary to override this method, for custom
     * painting override {@link #paintLayer(Graphics2D, JLayer)} instead
     * <p/>
     * This method calls {@code paintLayer(Graphics2D, JXLayer)} and resets the "dirty bit" at the end.
     *
     * @see #paintLayer(Graphics2D, JLayer)
     * @see #setDirty(boolean)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void paint(Graphics g, JComponent c) {
        if (g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g.create();
            JLayer<V> l = (JLayer<V>) c;
            paintLayer(g2, l);
            g2.dispose();
            setDirty(false);
        }
    }

    /**
     * Subclasses should implement this method and perform custom painting
     * operations here.
     * <p/>
     * The default implementation paints the passed {@code JXLayer} as is.
     *
     * @param g2 the {@code Graphics2D} context in which to paint
     * @param l  the {@code JXLayer} being painted
     */
    protected void paintLayer(Graphics2D g2, JLayer<? extends V> l) {
        l.paint(g2);
    }

    /**
     * By default only mouse, mouse motion, mouse wheel, keyboard and focus
     * events are supported, if you need to catch any other type of events,
     * override this method to return the different mask
     *
     * @see JLayer#setLayerEventMask(long)
     */
    private long getLayerEventMask() {
        return AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
                | AWTEvent.MOUSE_WHEEL_EVENT_MASK | AWTEvent.KEY_EVENT_MASK
                | AWTEvent.FOCUS_EVENT_MASK;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method calls the appropriate {@code process<eventType>Event} method
     * for the given class of event.
     */
    @Override
    public void eventDispatched(AWTEvent e, JLayer<? extends V> l) {
        if (e instanceof FocusEvent) {
            processFocusEvent((FocusEvent) e, l);
        } else if (e instanceof MouseEvent) {
            switch (e.getID()) {
                case MouseEvent.MOUSE_PRESSED:
                case MouseEvent.MOUSE_RELEASED:
                case MouseEvent.MOUSE_CLICKED:
                case MouseEvent.MOUSE_ENTERED:
                case MouseEvent.MOUSE_EXITED:
                    processMouseEvent((MouseEvent) e, l);
                    break;
                case MouseEvent.MOUSE_MOVED:
                case MouseEvent.MOUSE_DRAGGED:
                    processMouseMotionEvent((MouseEvent) e, l);
                    break;
                case MouseEvent.MOUSE_WHEEL:
                    processMouseWheelEvent((MouseWheelEvent) e, l);
                    break;
                default:
                    break;
            }
        } else if (e instanceof KeyEvent) {
            processKeyEvent((KeyEvent) e, l);
        }
    }

    /**
     * Processes {@code FocusEvent} occurring on the {@link JLayer} or any of
     * its subcomponents.
     *
     * @param e the {@code FocusEvent} to be processed
     * @param l the layer this LayerUI is set to
     */
    @Override
    protected void processFocusEvent(FocusEvent e, JLayer<? extends V> l) {
    }

    /**
     * Processes {@code MouseEvent} occurring on the {@link JLayer} or any of
     * its subcomponents.
     *
     * @param e the {@code MouseEvent} to be processed
     * @param l the layer this LayerUI is set to
     */
    @Override
    protected void processMouseEvent(MouseEvent e, JLayer<? extends V> l) {
    }

    /**
     * Processes mouse motion events occurring on the {@link JLayer} or any of
     * its subcomponents.
     *
     * @param e the {@code MouseEvent} to be processed
     * @param l the layer this LayerUI is set to
     */
    @Override
    protected void processMouseMotionEvent(MouseEvent e, JLayer<? extends V> l) {
    }

    /**
     * Processes {@code MouseWheelEvent} occurring on the {@link JLayer} or any
     * of its subcomponents.
     *
     * @param e the {@code MouseWheelEvent} to be processed
     * @param l the layer this LayerUI is set to
     */
    @Override
    protected void processMouseWheelEvent(MouseWheelEvent e,
                                          JLayer<? extends V> l) {
    }

    /**
     * Processes {@code KeyEvent} occurring on the {@link JLayer} or any of its
     * subcomponents.
     *
     * @param e the {@code KeyEvent} to be processed
     * @param l the layer this LayerUI is set to
     */
    @Override
    protected void processKeyEvent(KeyEvent e, JLayer<? extends V> l) {
    }
}
