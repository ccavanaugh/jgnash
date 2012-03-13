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
package jgnash.ui.util;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * Icon utility class
 * 
 * @author Craig Cavanaugh
 * @version $Id: IconUtils.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public final class IconUtils {

    private IconUtils() {
        // utility class
    }

    /**
     * Creates an <code>ImageIcon</code> from any Icon. Useful for caching complex icon paints into a buffered Icon
     * 
     * @param icon Icon with complex painting operation
     * @return <code>ImageIcon</code> based on a <code>BufferedImage</code>
     */
    public static ImageIcon createImageIcon(final Icon icon) {

        BufferedImage bufferedImage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = bufferedImage.createGraphics();

        // paint the icon into the BufferedImage
        // pass a JLabel; the Metal based icons want a GraphicsConfiguration from the passed component
        icon.paintIcon(new JLabel(), g, 0, 0);

        g.dispose();

        return new ImageIcon(bufferedImage);
    }
}
