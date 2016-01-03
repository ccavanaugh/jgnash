/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * Icon and Image utility class
 * 
 * @author Craig Cavanaugh
 */
public final class IconUtils {

    private IconUtils() {
        // utility class
    }

    /**
     * Creates an {@code ImageIcon} from any Icon. Useful for caching complex icon paints into a buffered Icon
     * 
     * @param icon Icon with complex painting operation
     * @return {@code ImageIcon} based on a {@code BufferedImage}
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

    /**
     * Creates an ImageIcon using a buffered image to take advantage of hardware
     * rendering if it is available
     *
     * @param icon path to icon
     * @return icon
     */
    public static ImageIcon getIcon(final String icon) {

        ImageIcon imageIcon;

        try {
            imageIcon =  new ImageIcon(IconUtils.class.getResource(icon));
        } catch (NullPointerException e) {
            imageIcon = null;
        }

        return imageIcon;
    }

    /**
     * Creates an Image using the specified file
     *
     * @param icon path to icon
     * @return new Image
     */
    public static Image getImage(final String icon) {
        return getIcon(icon).getImage();
    }
}
