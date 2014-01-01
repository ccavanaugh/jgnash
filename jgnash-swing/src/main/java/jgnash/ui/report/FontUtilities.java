/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.ui.report;

import static java.lang.Math.ceil;
import ar.com.fdvs.dj.domain.Style;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

/**
 * Utility class to help calculate font sizes for reporting
 * 
 * @author Craig Cavanaugh
 *
 */
public class FontUtilities {

    /**
     * Allow the <code>FontRenderContext</code> to be garbage collected as needed
     */
    private static Reference<FontRenderContext> contextReference = new SoftReference<>(null);

    private FontUtilities() {
    }

    /**
     * Calculates the width of the specified <code>String</code>.
     * 
     * @param text the text to be weighted.
     * @param font the font to be weighted
     * @return the width of the given string in 1/72" dpi.
     */
    private static int getStringWidth(final String text, final Font font) {
        final FontRenderContext frc = getFontRenderContext();

        // text is padded by one space
        final Rectangle2D bounds = font.getStringBounds(text + " ", frc);

        return (int) ceil(bounds.getWidth()) + 5;
    }

    /**
     * Calculates the width of the specified <code>String</code>.
     * 
     * @param text the text to be weighted.
     * @param style the style to be weighted
     * @return the width of the given string in 1/72" dpi.
     */
    public static int getStringWidth(final String text, final Style style) {
        Font font = new Font(style.getFont().getFontName(), Font.PLAIN, style.getFont().getFontSize());

        if (style.getFont().isBold()) {
            font = font.deriveFont(Font.BOLD);
        }

        if (style.getFont().isItalic()) {
            font = font.deriveFont(Font.ITALIC);
        }

        return getStringWidth(text, font);
    }

    private synchronized static FontRenderContext getFontRenderContext() {
        FontRenderContext context = contextReference.get();

        if (context == null) {
            context = new FontRenderContext(null, true, true);
            contextReference = new SoftReference<>(context);
        }

        return context;
    }

}
