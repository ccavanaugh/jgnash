/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.ui.plaf;

import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * Manages the look and feel and metal theme
 * 
 * @author Craig Cavanaugh
 */
public class NimbusUtils {

    private static float growthPercentage = 1f;

    /**
     * Returns the base font size for the Nimbus Look and Feel
     * <p>
     * Assumes that the Nimbus look and feel has been set.
     * 
     * @return base base font size
     */
    @SuppressWarnings("ConstantConditions")
    public static int getBaseFontSize() {

        int baseSize = 12;

        UIDefaults defaults = UIManager.getLookAndFeelDefaults();

        Object object = defaults.get("Label.font");

        if (object != null) {
            if (object instanceof Font) {
                baseSize = ((Font) object).getSize();
            } else if (object instanceof FontUIResource) {
                baseSize = ((FontUIResource) object).getSize();
            }
        }

        return baseSize;
    }

    @SuppressWarnings("ConstantConditions")
    public static void changeFontSize(final int size) {

        // get UI defaults
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();

        { // determine percent reduction for insets, etc.
            float baseSize = getBaseFontSize();

            growthPercentage = size / baseSize;
        }

        // reduce font sizes
        defaults.keySet().stream().filter(keyObj -> keyObj instanceof String).forEach(keyObj -> {
            String key = (String) keyObj;

            if (key.contains("font")) {

                Object object = defaults.get(key);

                if (object instanceof Font) {
                    Font font = (Font) object;

                    Font derived = font.deriveFont((float) size);

                    defaults.put(key, derived);

                } else if (object instanceof FontUIResource) {
                    FontUIResource resource = (FontUIResource) object;

                    FontUIResource derived = new FontUIResource(resource.deriveFont((float) size));
                    defaults.put(key, derived);
                }
            }
        });

        // reduce content Margins
        defaults.keySet().stream().filter(keyObj -> keyObj instanceof String).forEach(keyObj -> {
            String key = (String) keyObj;

            if (key.contains("contentMargins") || key.contains("padding")) {

                Insets derived = (Insets) ((Insets) defaults.get(key)).clone();

                if (derived.left > 0) {
                    derived.left = (int) Math.ceil(derived.left * growthPercentage);
                }

                if (derived.right > 0) {
                    derived.right = (int) Math.ceil(derived.right * growthPercentage);
                }

                if (derived.top > 0) {
                    derived.top = (int) Math.ceil(derived.top * growthPercentage);
                }

                if (derived.bottom > 0) {
                    derived.bottom = (int) Math.ceil(derived.bottom * growthPercentage);
                }

                defaults.put(key, derived);
            }
        });

        // reduce content Margins
        defaults.keySet().stream().filter(keyObj -> keyObj instanceof String).forEach(keyObj -> {
            String key = (String) keyObj;

            if (key.contains("textIconGap") || key.contains("size") || key.contains("thumbWidth") || key.contains("thumbHeight")) {

                Integer integer = (Integer) defaults.get(key);
                Integer derived = (int) Math.ceil((float) integer * growthPercentage);

                defaults.put(key, derived);
            }
        });
    }

    /**
     * Returns a scaled icon based on the reduced font size
     * 
     * @param icon {@code ImageIcon} to scale
     * @return the scaled {@code Icon}
     */
    public static Icon scaleIcon(final ImageIcon icon) {

        int scaledWidth = (int) Math.floor(icon.getIconWidth() * growthPercentage);
        int scaledHeight = (int) Math.floor(icon.getIconHeight() * growthPercentage);

        Image scaledInstance = icon.getImage().getScaledInstance(scaledWidth, scaledHeight, java.awt.Image.SCALE_SMOOTH);

        return new ImageIcon(scaledInstance);
    }

    /**
     * Reduce the margins around the content of a button for the Nimbus look and feel
     * 
     * @param button button to alter
     */
    public static void reduceNimbusButtonMargin(final JButton button) {
        UIDefaults buttonDefaults = new UIDefaults();
        buttonDefaults.put("Button.contentMargins", new Insets(6, 6, 6, 6));

        button.putClientProperty("Nimbus.Overrides", buttonDefaults);
        button.putClientProperty("Nimbus.Overrides.InheritDefaults", Boolean.FALSE);
    }

    private NimbusUtils() {

    }
}
