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

package org.jdesktop.jxlayer.plaf;

import javax.swing.JLayer;
import org.jdesktop.jxlayer.plaf.effect.BufferedImageOpEffect;
import org.jdesktop.jxlayer.plaf.effect.LayerEffect;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.SoftReference;

/**
 * The {@code AbstractBufferedLayerUI} paitns the {@link JLayer}
 * to the {@code BufferedImage} and then paints this image to the {@code Graphics}
 * passed to its {@code paint} method.
 * <p/>
 * The main feature provided by {@code AbstractBufferedLayerUI}
 * is the ability to apply various {@link org.jdesktop.jxlayer.plaf.effect.LayerEffect}s to its content,<br/>
 * the most popular effect is the {@link BufferedImageOpEffect}
 * which uses {@code BufferedImageOp} to filter the buffer
 * <p/>
 * This class introduces the {@link #isIncrementalUpdate(JLayer)} method.
 * <p/>
 * If it returns {@code false} and {@link #isDirty()} returns {@code false}
 * and the cached image exists and matches the size of painted {@code JXLayer}
 * then the existing image will be used during the painting.
 * It helps to skip unnecessary painting
 * and save a lot of time, especially if {@link BufferedImageOpEffect}s are used.
 * If {@code isIncrementalUpdate(JXLayer)} returns {@code true}
 * the cache image will be updated on every painting.
 * <p/>
 * For custom painting, override {@link #paintLayer(Graphics2D,JLayer)} as usual.
 * <p/>
 * If you want to apply {@code Effect}s, override  {@link #getLayerEffects(JLayer)} methods
 * or use more flexible {@link BufferedLayerUI}.
 * <p/>
 * <b>Note:</b> The {@code AbstractBufferedLayerUI} is not shareable and
 * can be set to single {@link JLayer} instance.
 * The current {@code JXLayer} can be obtained with {@link #getLayer()} method
 *
 * @see org.jdesktop.jxlayer.plaf.effect.LayerEffect
 * @see BufferedImageOpEffect
 * @see BufferedLayerUI
 * @see #isIncrementalUpdate(JLayer)
 */
public class AbstractBufferedLayerUI<V extends JComponent>
        extends AbstractLayerUI<V> implements PropertyChangeListener {

   
	private static final long serialVersionUID = 1L;

	private JLayer<? extends V> layer;

    private transient SoftReference<BufferedImage> cachedBuffer;
    private static final LayerEffect[] emptyEffectArray = new LayerEffect[0];
    
    /**
     * {@inheritDoc}
     * <p/>
     * This implementation saves the passed {@code JXLayer} instance
     * and checks that it set to one layer only
     *
     * @throws IllegalStateException if this {@code BufferedLayerUI}
     * is set to multiple {@code JXLayer}s
     * @see #uninstallUI(JComponent)
     * @see #getLayer()
     */
    @SuppressWarnings("unchecked")
    public void installUI(JComponent c) {
        if (layer != null) {
            throw new IllegalStateException(
                    "BufferedLayerUI can't be shared between multiple layers");
        }
        layer = (JLayer<? extends V>) c;
        c.addPropertyChangeListener(this);
        super.installUI(c);
    }

    /**
     * {@inheritDoc}
     */
    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        c.removePropertyChangeListener(this);
        layer = null;
    }

    /**
     * This method is public as an implementation side effect.
     * {@code AbstractBufferedLayerUI} listens property changes of its {@link JLayer}
     * and marks itself as dirty if the {@code JXLayer}'s view component has been changed.
     *
     * @param evt the PropertyChangeEvent
     *
     * @see JLayer#setView(Component)
     * @see #setDirty(boolean)
     */
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if (evt.getSource() != getLayer() || "view".equals(propertyName)) {
            setDirty(true);
        }
    }

    /**
     * Mark {@code AbstractBufferedLayerUI} as dirty if the LookAndFeel was changed
     *
     * @param l the {@code JXLayer} this {@code AbstractBufferedLayerUI} is set to
     */
    public void updateUI(JLayer<? extends V> l) {
        setDirty(true);
    }

    /**
     * The {@code JXLayer} this {@code AbstractBufferedLayerUI} is set to.
     *
     * @return the {@code JXLayer} this {@code AbstractBufferedLayerUI} is set to
     */
    public JLayer<? extends V> getLayer() {
        return layer;
    }

    /**
     * Returns the current cached image.
     * <p/>
     * The implementation of this LayerUI may use SoftReference to keep
     * this image, so this method may return {@code null} at any time.
     * <p/>
     * However it is guaranteed that is safe to call this method inside
     * {@link #paintLayer(Graphics2D,JLayer)} method, because a strong reference
     * to the buffer is kept during painting process and you'll get the actual
     * BufferedImage which you are free to use withing {@code paintLayer}.
     *
     * @return the current cached image.
     *
     * @see #setBuffer(BufferedImage)
     */
    protected BufferedImage getBuffer() {
        return cachedBuffer == null ? null : cachedBuffer.get();
    }

    /**
     * Sets the current cached image.
     *
     * @param buffer the {@code BufferedImage} to be used as the cache
     *
     * @see #getBuffer()
     */
    protected void setBuffer(BufferedImage buffer) {
        cachedBuffer = new SoftReference<>(buffer);
    }

    /**
     * Returns the array of {@link LayerEffect} to be used during painting of this {@code JXLayer},
     * the default implementation returns constant empty array.
     *
     * @param l the {@code JXLayer} being painted
     *
     * @return the array of {@link LayerEffect} to be used during painting of the {@code JXLayer}
     */
    protected LayerEffect[] getLayerEffects(JLayer<? extends V> l) {
        return emptyEffectArray;
    }

    /**
     * If this method returns {@code false} and {@link #isDirty} returns {@code false}
     * and the cached image exists and matches the size of painted {@code JXLayer}
     * then the existing image will be used during the painting.
     * <p/>
     * It helps to skip unnecessary painting and save a lot of time,
     * especially if {@link BufferedImageOpEffect}s are used.
     * <br/>
     * If this method returns {@code true} the cache image will be updated on every painting.
     * <p/>
     * The default implementation returns {@code true}
     * <p/>
     *
     * @param l the {@code JXLayer} being painted
     *
     * @return {@code true} if the cache image should be updated on every painting,
     *         otherwise returns {@code false}
     *
     * @see #getBuffer()
     */
    protected boolean isIncrementalUpdate(JLayer<? extends V> l) {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method paints the paitns the {@link JLayer} to the {@code BufferedImage}
     * and then paints this image to the passed {@code Graphics}.
     * <p/>It also manages the state of the existing cached image
     * and applies the existing {@link LayerEffect}s to the image.
     *
     * @see #paintLayer(Graphics2D,JLayer)
     * @see #isBufferFormatValid(JLayer)
     * @see #isBufferContentValid(JLayer,Shape)
     * @see #getLayerEffects(JLayer)
     */
    @SuppressWarnings("unchecked")
    public void paint(Graphics g, JComponent c) {
        if (g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g.create();
            JLayer<? extends V> l = (JLayer<? extends V>) c;
            configureGraphics(g2, l);
            Shape clip = g2.getClip();
            // temporary strong reference to prevent the cachedBuffer softReference
            // from being cleaned up by the garbage collector during painting
            BufferedImage buffer = getBuffer();
            boolean isBufferFormatValid = isBufferFormatValid(l);
            if (!isBufferFormatValid || !isBufferContentValid(l, clip)) {
                if (!isBufferFormatValid) {
                    buffer = createBuffer(l.getWidth(), l.getHeight());
                    setBuffer(buffer);
                }
                Graphics2D bufg = buffer.createGraphics();
                if (isIncrementalUpdate(l)) {
                    bufg.setClip(clip);
                }
                paintLayer(bufg, l);
                applyLayerEffects(l, bufg.getClip());
                bufg.dispose();
            }
            g2.drawImage(buffer, 0, 0, null);
            g2.dispose();
            setDirty(false);
        }
    }

    /**
     * Defines if the cached image has the valid format
     * for the current painting painting operation
     * and there is no need to recreate it.
     * <p/>
     * The default implementation returns {@code true}
     * if the cached image is not null and its size matches
     * the size of the {@code JXLayer} being painted,
     * otherwise it returns {@code true}.
     *
     * @param l the {@code JXLayer} being painted
     *
     * @return {@code true} if the format of existing cache image
     *         is valid, otherwise returns {@code false}
     *
     * @see #getBuffer()
     */
    protected boolean isBufferFormatValid(JLayer<? extends V> l) {
        BufferedImage buffer = getBuffer();
        return buffer != null &&
                buffer.getWidth() == l.getWidth() &&
                buffer.getHeight() == l.getHeight();
    }

    /**
     * Defines if the cached image has the valid content
     * for the current painting painting operation
     * and there is no need to repaint it.
     * <p/>
     * The default implementation returns {@code true}
     * if this {@code AbstractBufferedLayerUI} hasn't been marked as dirty
     * and incremental update is disabled.
     *
     * @param l the {@code JXLayer} being painted
     * @param clip the current clipping shape
     *
     * @return {@code true} if the content of existing cache image
     *         is valid, otherwise returns {@code false}
     *
     * @see #isDirty()
     * @see #isIncrementalUpdate(JLayer)
     */
    protected boolean isBufferContentValid(JLayer<? extends V> l, Shape clip) {
        return !isDirty() && !isIncrementalUpdate(l);
    }

    /**
     * Creates the {@code BufferedImage} to be used as the cached image.
     * This method must never return {@code null}.
     *
     * @param width the width of the image
     * @param height the height of the image
     *
     * @return an off-screen {@code BufferedImage},
     *         which can be used for double buffering.
     */
    protected BufferedImage createBuffer(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Iterates through the current array of {@link LayerEffect}s
     * and applies each enabled one to the buffer.
     *
     * @param l the {@code JXLayer} being painted
     * @param clip the current clipping shape
     *
     * @see #getLayerEffects(JLayer)
     * @see org.jdesktop.jxlayer.plaf.effect.LayerEffect#isEnabled()
     */
    protected void applyLayerEffects(JLayer<? extends V> l, Shape clip) {
        if (getBuffer() == null) {
            throw new IllegalStateException("Buffer is null");
        }
        for (LayerEffect e : getLayerEffects(l)) {
            if (e.isEnabled()) {
                e.apply(getBuffer(), clip);
            }
        }
    }
}
