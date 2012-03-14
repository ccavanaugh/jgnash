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

import java.awt.image.BufferedImageOp;

import org.jdesktop.jxlayer.plaf.*;

/**
 * This {@code Effect} uses {@link BufferedImageOp} to filter the passed
 * {@code BufferedImageOp}.
 * <p/>
 * This class can be used together with subclasses of {@link AbstractBufferedLayerUI}
 * to create various visual effects like blur, color inversion etc.
 *
 * @see AbstractBufferedLayerUI#getLayerEffects(org.jdesktop.jxlayer.JXLayer)
 * @see BufferedLayerUI#setLayerEffects(LayerEffect[])
 */
public class BufferedImageOpEffect extends AbstractBufferedImageOpEffect {
    private BufferedImageOp[] bufferedImageOps = new BufferedImageOp[0];

    /**
     * Creates an instance of {@code ImageEffect}
     * with the specified array of {@code BufferedImageOp}.
     *
     * @param bufferedImageOps the array of {@code BufferedImageOp}s
     * to be used by this {@code ImageEffect}
     */
    public BufferedImageOpEffect(BufferedImageOp... bufferedImageOps) {
        setBufferedImageOps(bufferedImageOps);
    }

    /**
     * {@inheritDoc}
     */
    public BufferedImageOp[] getBufferedImageOps() {
        BufferedImageOp[] result = new BufferedImageOp[bufferedImageOps.length];
        System.arraycopy(bufferedImageOps, 0, result, 0, result.length);
        return result;
    }

    /**
     * Sets the array of {@code BufferedImageOp}s
     * to be used by this {@code ImageEffect}
     * 
     * @param bufferedImageOps the array of {@code BufferedImageOp}s
     * to be used by this {@code ImageEffect}
     */
    public void setBufferedImageOps(BufferedImageOp... bufferedImageOps) {
        if (bufferedImageOps == null) {
            bufferedImageOps = new BufferedImageOp[0];
        }
        BufferedImageOp[] oldBufferedImageOps = this.bufferedImageOps;
        this.bufferedImageOps = new BufferedImageOp[bufferedImageOps.length];
        System.arraycopy(bufferedImageOps, 0, this.bufferedImageOps, 0, bufferedImageOps.length);
        firePropertyChange("bufferedImageOps", oldBufferedImageOps, bufferedImageOps);
    }
}
