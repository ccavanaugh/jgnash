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
import org.jdesktop.jxlayer.plaf.effect.LayerEffect;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code BufferedLayerUI} is the mutable implementation of the
 * {@link AbstractBufferedLayerUI} class.
 * <p/>
 * It provides setters for all its graphics properties like clipping shape,
 * array of the {@link org.jdesktop.jxlayer.plaf.effect.LayerEffect}s, etc. It
 * also notifies its {@code LayerItemListener}s when any of its properties or
 * properties of its {@code Effect}s are changed, this leads to the automatic
 * repainting of the {@code JXLayer}, this object is set to.
 * <p/>
 * <b>Note:</b> The {@code BufferedLayerUI} is not shareable and can be set to
 * single {@link JXLayer} instance. The current {@code JXLayer} can be obtained
 * with {@link #getLayer()} method
 * 
 * @see #setComposite(java.awt.Composite)
 * @see #setAlpha(float)
 * @see #setClip(java.awt.Shape)
 * @see #setLayerEffects(org.jdesktop.jxlayer.plaf.effect.LayerEffect[])
 * @see #setRenderingHints(java.util.Map)
 * @see #setTransform(java.awt.geom.AffineTransform)
 */
public class BufferedLayerUI<V extends JComponent> extends
		AbstractBufferedLayerUI<V> {

	private static final long serialVersionUID = 1L;

	private LayerEffect[] effects = new LayerEffect[0];
	private Shape clip;
	private Composite composite;
	private Map<RenderingHints.Key, Object> renderingHints = new HashMap<RenderingHints.Key, Object>(
			0);
	private AffineTransform transform;
	private boolean incrementalUpdate = true;

	/**
	 * Returns the array of the {@link LayerEffect}s to be applied to the buffer
	 * of this {@code BufferedLayerUI}
	 * 
	 * @return the collection of the {@code Effect}s to be applied to the buffer
	 *         of this painter
	 * 
	 * @see #setLayerEffects(LayerEffect[])
	 * @see #getLayerEffects(JXLayer)
	 */
	public LayerEffect[] getLayerEffects() {
		LayerEffect[] result = new LayerEffect[effects.length];
		System.arraycopy(effects, 0, result, 0, result.length);
		return result;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * This implementation returns the array of {@code Effect}s set using
	 * {@link #setLayerEffects(LayerEffect[])}
	 * <p/>
	 * If a {@code BufferedLayerUI} provides more extensive API to support
	 * different {@code Effect}s depending on its state or on the state of the
	 * passed {@code JXLayer}, this method should be overridden.
	 * 
	 * @see #setLayerEffects(LayerEffect[])
	 * @see #getLayerEffects()
	 */
	protected LayerEffect[] getLayerEffects(JLayer<? extends V> l) {
		return getLayerEffects();
	}

	/**
	 * Sets the array of the {@code Effect}s to be applied to the
	 * {@link Graphics2D} during painting of this {@code BufferedLayerUI}
	 * <p/>
	 * This {@code BufferedLayerUI} is set to every {@code Effect} in this array
	 * as a {@link java.beans.PropertyChangeListener} to mark itself as dirty
	 * when any of those {@code Effect}s changes its state.
	 * <p>
	 * This method marks {@code BufferedLayerUI} as dirty which causes
	 * repainting of its {@link JXLayer}.
	 * 
	 * @param effects
	 *            the array of the {@code Effect}s to be applied to the buffer
	 *            of this {@code BufferedLayerUI}
	 * 
	 * @see #getLayerEffects()
	 * @see #getLayerEffects(JXLayer)
	 * @see #setDirty(boolean)
	 */
	public void setLayerEffects(LayerEffect... effects) {
		LayerEffect[] oldEffects = getLayerEffects();
		if (effects == null) {
			effects = new LayerEffect[0];
		}
		for (LayerEffect effect : getLayerEffects()) {
			effect.removePropertyChangeListener(this);
		}
		this.effects = new LayerEffect[effects.length];
		System.arraycopy(effects, 0, this.effects, 0, effects.length);
		for (LayerEffect effect : effects) {
			effect.addPropertyChangeListener(this);
		}
		firePropertyChange("layerEffects", oldEffects, effects);
	}

	/**
	 * Returns the {@code Shape} to be applied to the {@link Graphics2D} during
	 * painting of this {@code BufferedLayerUI}.
	 * 
	 * @return the {@code Shape} to be applied to the {@link Graphics2D} during
	 *         painting of this {@code BufferedLayerUI}
	 * 
	 * @see #setClip(Shape)
	 * @see #getClip(JXLayer)
	 */
	public Shape getClip() {
		return clip;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * This implementation returns the {@code Shape} set using
	 * {@link #setClip(Shape)}
	 * <p/>
	 * If a {@code BufferedLayerUI} provides more extensive API to support
	 * different clipping shapes depending on its state or on the state of the
	 * passed {@code JXLayer}, this method should be overridden.
	 * 
	 * @see #getClip()
	 * @see #setClip(Shape)
	 */
	protected Shape getClip(JLayer<? extends V> l) {
		return getClip();
	}

	/**
	 * Sets the clipping {@code Shape} to be applied to the {@link Graphics2D}
	 * during painting of this {@code BufferedLayerUI}.
	 * <p>
	 * This method marks {@code BufferedLayerUI} as dirty which causes
	 * repainting of its {@link JXLayer}.
	 * 
	 * @param clip
	 *            the {@code Shape} to be used as the clip during painting of
	 *            this {@code BufferedLayerUI}
	 * 
	 * @see #getClip()
	 * @see #getClip(JXLayer)
	 * @see #setDirty(boolean)
	 */
	public void setClip(Shape clip) {
		Shape oldClip = getClip();
		this.clip = clip;
		firePropertyChange("clip", oldClip, clip);
	}

	/**
	 * Returns the {@code Composite} to be applied to the {@link Graphics2D}
	 * during painting of this {@code BufferedLayerUI}.
	 * 
	 * @return the {@code Composite} to be applied to the {@link Graphics2D}
	 *         during painting of this {@code BufferedLayerUI}
	 * 
	 * @see #setComposite(Composite)
	 * @see #getComposite(JXLayer)
	 */
	public Composite getComposite() {
		return composite;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * This implementation returns the {@code Composite} set using
	 * {@link #setComposite(Composite)}
	 * <p/>
	 * If a {@code BufferedLayerUI} provides more extensive API to support
	 * different {@code Composite}s depending on its state or on the state of
	 * the passed {@code JXLayer}, this method should be overridden.
	 * 
	 * @see #setComposite(Composite)
	 * @see #getComposite()
	 */
	protected Composite getComposite(JLayer<? extends V> l) {
		return getComposite();
	}

	/**
	 * Sets the {@code Composite} to be applied to the {@link Graphics2D} during
	 * painting of this {@code BufferedLayerUI}.
	 * <p>
	 * This method marks {@code BufferedLayerUI} as dirty which causes
	 * repainting of its {@link JXLayer}.
	 * 
	 * @param composite
	 *            the {@code Composite} to be applied to the {@link Graphics2D}
	 *            during painting of this {@code BufferedLayerUI}.
	 * 
	 * @see #getComposite()
	 * @see #getComposite(JXLayer)
	 * @see #setDirty(boolean)
	 */
	public void setComposite(Composite composite) {
		Composite oldComposite = getComposite();
		this.composite = composite;
		firePropertyChange("composite", oldComposite, composite);
	}

	/**
	 * If {@link #getComposite()} returns an instanse of {@code AlphaComposite}
	 * then this method returns the value of {@link AlphaComposite#getAlpha()},
	 * otherwise it returns {@code 1}.
	 * 
	 * @return If {@link #getComposite()} returns an instanse of
	 *         {@code AlphaComposite} returns the value of
	 *         {@link AlphaComposite#getAlpha()}, otherwise it returns {@code 1}
	 * 
	 * @see #setAlpha(float)
	 * @see #setComposite(Composite)
	 * @see #getComposite()
	 */
	public float getAlpha() {
		if (composite instanceof AlphaComposite) {
			AlphaComposite ac = (AlphaComposite) composite;
			return ac.getAlpha();
		}
		return 1;
	}

	/**
	 * Sets the {@link AlphaComposite} with the specified alpha value as the
	 * {@code Composite} for this {@code BufferedLayerUI}.
	 * 
	 * @param alpha
	 *            the constant alpha to be multiplied with the alpha of the
	 *            source. {@code alpha} must be a floating point number in the
	 *            inclusive range [0.0,&nbsp;1.0].
	 * 
	 * @throws IllegalArgumentException
	 *             if {@code alpha} is less than 0.0 or greater than 1.0,
	 * 
	 * @see #getAlpha()
	 * @see #setComposite(Composite)
	 */
	public void setAlpha(float alpha) {
		setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
	}

	/**
	 * Returns the map of rendering hints to be applied to the
	 * {@link Graphics2D} during painting of this {@code BufferedLayerUI}.
	 * 
	 * @return the map of rendering hints to be applied to the
	 *         {@link Graphics2D} during painting of this
	 *         {@code BufferedLayerUI}
	 * 
	 * @see #setRenderingHints(Map)
	 * @see #getRenderingHints(JXLayer)
	 */
	public Map<RenderingHints.Key, Object> getRenderingHints() {
		return Collections.unmodifiableMap(renderingHints);
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * This implementation returns the map of rendreing hints set using
	 * {@link #setRenderingHints(java.util.Map)}
	 * <p/>
	 * If a {@code BufferedLayerUI} provides more extensive API to support
	 * different rendering hints depending on its state or on the state of the
	 * passed {@code JXLayer}, this method should be overridden.
	 * 
	 * @see #setRenderingHints(java.util.Map)
	 * @see #getRenderingHints()
	 */
	protected Map<RenderingHints.Key, Object> getRenderingHints(
			JLayer<? extends V> l) {
		return getRenderingHints();
	}

	/**
	 * Sets the map of rendering hints to be applied to the {@link Graphics2D}
	 * during painting of this {@code BufferedLayerUI}.
	 * <p>
	 * This method marks {@code BufferedLayerUI} as dirty which causes
	 * repainting of its {@link JXLayer}.
	 * 
	 * @param renderingHints
	 *            the map of rendering hints to be applied to the
	 *            {@link Graphics2D} during painting of this
	 *            {@code BufferedLayerUI}
	 * 
	 * @see #getRenderingHints()
	 * @see #getRenderingHints(JXLayer)
	 * @see #setDirty(boolean)
	 */
	public void setRenderingHints(Map<RenderingHints.Key, Object> renderingHints) {
		Map<RenderingHints.Key, Object> oldRenderingHints = getRenderingHints();
		if (renderingHints == null) {
			renderingHints = new HashMap<RenderingHints.Key, Object>();
		}
		this.renderingHints = renderingHints;
		firePropertyChange("renderingHints", oldRenderingHints, renderingHints);
	}

	/**
	 * Returns the {@code AffineTransform} to be applied to the
	 * {@link Graphics2D} during painting of this {@code BufferedLayerUI}.
	 * 
	 * @return the {@code AffineTransform} to be applied to the
	 *         {@link Graphics2D} during painting of this
	 *         {@code BufferedLayerUI}
	 * 
	 * @see #setTransform(AffineTransform)
	 * @see #getTransform(JXLayer)
	 */
	public AffineTransform getTransform() {
		return transform;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * This implementation returns the {@code AffineTransform} set using
	 * {@link #setTransform(AffineTransform)}
	 * <p/>
	 * If a {@code BufferedLayerUI} provides more extensive API to support
	 * different {@code AffineTransform} depending on its state or on the state
	 * of the passed {@code JXLayer}, this method should be overridden.
	 * 
	 * @see #setTransform(AffineTransform)
	 * @see #getTransform()
	 */
	protected AffineTransform getTransform(JLayer<? extends V> l) {
		return getTransform();
	}

	/**
	 * Sets the {@code AffineTransform} to be applied to the {@link Graphics2D}
	 * during painting of this {@code BufferedLayerUI}.
	 * <p>
	 * This method marks {@code BufferedLayerUI} as dirty which causes
	 * repainting of its {@link JXLayer}.
	 * 
	 * @param transform
	 *            the {@code AffineTransform} to be applied to the
	 *            {@link Graphics2D} during painting of this
	 *            {@code BufferedLayerUI}
	 * 
	 * @see #getTransform()
	 * @see #getTransform(JXLayer)
	 * @see #setDirty(boolean)
	 */
	public void setTransform(AffineTransform transform) {
		AffineTransform oldTransform = getTransform();
		this.transform = transform;
		firePropertyChange("transform", oldTransform, transform);
	}

	/**
	 * Returns {@code true} if incremental update is enabled for this
	 * {@code BufferedLayerUI} and its cache image is updated on every
	 * repainting, otherwise returns {@code false}.
	 * 
	 * @return {@code true} if incremental update is enabled for this
	 *         {@code BufferedLayerUI} and its cache image is updated on every
	 *         repainting, otherwise returns {@code false}
	 * 
	 * @see #setIncrementalUpdate(boolean)
	 * @see #isIncrementalUpdate(JXLayer)
	 */
	public boolean isIncrementalUpdate() {
		return incrementalUpdate;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * This implementation returns the incremental update flag set using
	 * {@link #setIncrementalUpdate(boolean)}
	 * <p/>
	 * If a {@code BufferedLayerUI} provides more extensive API to support
	 * incremental updated depending on its state or on the state of the passed
	 * {@code JXLayer}, this method should be overridden.
	 * 
	 * @see #setIncrementalUpdate(boolean)
	 * @see #isIncrementalUpdate()
	 */
	protected boolean isIncrementalUpdate(JLayer<? extends V> l) {
		return isIncrementalUpdate();
	}

	/**
	 * Sets the incremental update flag for this {@code BufferedLayerUI}. If it
	 * is {@code true} the cache image will be updated on every repainting,
	 * otherwise the existing image will be painted.
	 * <p>
	 * This method marks {@code BufferedLayerUI} as dirty which causes
	 * repainting of its {@link JXLayer}.
	 * 
	 * @param incrementalUpdate
	 *            {@code true} if incremental update is enabled and cache image
	 *            will be updated on every repainting, otherwise {@code false}
	 * 
	 * @see #isIncrementalUpdate()
	 * @see #isIncrementalUpdate(JXLayer)
	 * @see #setDirty(boolean)
	 */
	public void setIncrementalUpdate(boolean incrementalUpdate) {
		boolean oldIncrementalUpdate = isIncrementalUpdate();
		this.incrementalUpdate = incrementalUpdate;
		firePropertyChange("incrementalUpdate", oldIncrementalUpdate,
				incrementalUpdate);
	}
}
