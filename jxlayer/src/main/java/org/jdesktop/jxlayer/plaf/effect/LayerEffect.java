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

package org.jdesktop.jxlayer.plaf.effect;

import java.awt.image.BufferedImage;
import java.awt.*;
import java.beans.PropertyChangeListener;

/**
 * This interface represents an abstract type which performes
 * some operations on the {@link BufferedImage}.
 *
 * @see AbstractLayerEffect
 * @see BufferedImageOpEffect
 */
public interface LayerEffect {

    /**
     * Indicates if this {@code LayerItem} is in active state.
     *
     * @return {@code true} if this {@code LayerItem} is in active state
     */
    public boolean isEnabled();
    
    /**
     * Adds a {@code LayerItemListener} to the layer item.
     *
     * @param l the listener to add 
     */
    public void addPropertyChangeListener(PropertyChangeListener l);

    /**
     * Returns an array of all the {@link PropertyChangeListener}s
     * registered on this {@code LayerItem}.

     * @return all of this item's {@code LayerItemListener}s 
     * or an empty array if no item listeners are currently registered
     */
    public PropertyChangeListener[] getPropertyChangeListeners();

    /**
     * Removes a {@link PropertyChangeListener} from this {@code LayerItem}.
     *
     * @param l the listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener l);
    
    /**
     * Performes some operations on the passed {@code BufferedImage}.
     * <p/>
     * The effect may take into account the passed {@code clip} shape
     * to speed the processing up.
     *
     * @param buf the {@code BufferedImage} to be processed
     * @param clip the clip shape or {@code null}
     * if the entire {@code BufferedImage} must be processed
     */
    public void apply(BufferedImage buf, Shape clip);
}
