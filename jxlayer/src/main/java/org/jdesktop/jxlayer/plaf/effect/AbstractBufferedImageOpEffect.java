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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.lang.ref.SoftReference;

/**
 * The base class for {@link org.jdesktop.jxlayer.plaf.effect.BufferedImageOpEffect}
 */
public class AbstractBufferedImageOpEffect extends AbstractLayerEffect {
    private transient SoftReference<BufferedImage> cachedSubImage;
    private static final BufferedImageOp[] emptyOpsArray = new BufferedImageOp[0];

    /**
     * {@inheritDoc}
     * <p/>
     * Filters the passed image using {@code clip} and 
     * {@code BufferedImageOp}s provided by {@link #getBufferedImageOps()}
     */
    public void apply(BufferedImage buffer, Shape clip) {
        if (buffer == null) {
            throw new IllegalArgumentException("BufferedImage is null");
        }
        Rectangle bufferSize = new Rectangle(buffer.getWidth(), buffer.getHeight());

        if (clip == null) {
            clip = bufferSize;
        }
        Rectangle clipBounds = clip.getBounds().intersection(bufferSize);

        if (clipBounds.isEmpty() ||
                buffer.getWidth() <= clipBounds.x ||
                buffer.getHeight() <= clipBounds.y) {
            return;
        }

        int x = clipBounds.x;
        int y = clipBounds.y;
        int width = clipBounds.width;
        int height = clipBounds.height;

        if (buffer.getWidth() < x + width) {
            width = buffer.getWidth() - x;
        }
        if (buffer.getHeight() < y + height) {
            height = buffer.getHeight() - y;
        }

        BufferedImage subImage = cachedSubImage == null ? null : cachedSubImage.get();

        if (subImage == null ||
                subImage.getWidth() != width ||
                subImage.getHeight() != height) {
            subImage = new BufferedImage(width, height, buffer.getType());
            cachedSubImage = new SoftReference<>(subImage);
        }

        Graphics2D bufg = buffer.createGraphics();
        bufg.setClip(clip);
        Graphics2D subg = subImage.createGraphics();
        for (BufferedImageOp op : getBufferedImageOps()) {
            subg.drawImage(buffer, 0, 0, width, height, x, y, x + width, y + height, null);
            bufg.drawImage(subImage, op, x, y);
        }
        subg.dispose();
        bufg.dispose();
    }

    /**
     * Returns the array of {@code BufferedImageOp}s 
     * specified for this {@code LayerEffect}.
     *
     * @return the array of {@code BufferedImageOp}s 
     * specified for this {@code LayerEffect}
     */
    protected BufferedImageOp[] getBufferedImageOps() {
        return emptyOpsArray;
    }
}
