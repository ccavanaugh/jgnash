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
package jgnash.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;

import javax.swing.Icon;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * Paints an Icon with vertical text
 *
 * @author Craig Cavanaugh
 *
 */
@SuppressWarnings({"SuspiciousNameCombination"})
public class VerticalTextIcon implements Icon, SwingConstants {
    private Font font;

    private FontMetrics fm;

    private final String text;

    private int width;

    private int height;

    private final boolean clockwise;

    private static final RenderingHints renderHints;

    static {
        renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        renderHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

    public VerticalTextIcon(String text, boolean clockwise) {
        this.text = text;

        calculateSize();

        this.clockwise = clockwise;
    }

    private void calculateSize() {
        font = UIManager.getFont("Button.font");

        FontRenderContext frc = new FontRenderContext(null, true, true);
        GlyphVector glyphs = font.createGlyphVector(frc, text);

        width = (int) glyphs.getLogicalBounds().getWidth();
        height = (int) glyphs.getLogicalBounds().getHeight();
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g;

        RenderingHints oldHints = g2.getRenderingHints();
        Font oldFont = g.getFont();
        Color oldColor = g.getColor();
        AffineTransform oldTransform = g2.getTransform();

        if (fm == null) {
            fm = g.getFontMetrics(font);
        }

        g2.setRenderingHints(renderHints);

        g.setFont(font);
        g.setColor(Color.black);
        if (clockwise) {
            g2.translate(x + getIconWidth(), y);
            g2.rotate(Math.PI / 2);
        } else {
            g2.translate(x, y + getIconHeight());
            g2.rotate(-Math.PI / 2);
        }
        //g.drawString(text, 0, fm.getLeading() + fm.getAscent());
        g.drawString(text, 0, fm.getLeading() + fm.getAscent() - (fm.getDescent() / 2));

        g.setFont(oldFont);
        g.setColor(oldColor);
        g2.setTransform(oldTransform);
        g2.setRenderingHints(oldHints);
    }

    @Override
    public int getIconWidth() {
        return height;
    }

    @Override
    public int getIconHeight() {
        return width;
    }

    public void updateUI() {
        fm = null;
        calculateSize();
    }
}
