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
package jgnash.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Craig Cavanaugh
 */
public class ResourceUtils {

    public static final String DEFAULT_RESOURCE_BUNDLE = "jgnash/resource/resource";

    private static ResourceBundle resourceBundle;

    private ResourceUtils() {
        // Utility class
    }

    public static ResourceBundle getBundle() {
        if (resourceBundle == null) {

            try {
                resourceBundle = ResourceBundle.getBundle(DEFAULT_RESOURCE_BUNDLE);
            } catch (final MissingResourceException e) {
                Logger.getLogger(ResourceUtils.class.getName()).log(Level.WARNING, "Could not find correct resource bundle", e);
                resourceBundle = ResourceBundle.getBundle(DEFAULT_RESOURCE_BUNDLE, Locale.ENGLISH);
            }
        }

        return resourceBundle;
    }

    /**
     * Gets a localized string with arguments
     *
     * @param key The key for the localized string
     * @param arguments arguments to pass the the message formatter
     * @return The localized string
     */
    public static String getString(final String key, final Object... arguments) {
        try {
            if (arguments.length == 0) {
                return getBundle().getString(key);
            } else {
                return MessageFormat.format(getBundle().getString(key), arguments);
            }
        } catch (final MissingResourceException mre) {
            Logger.getLogger(ResourceUtils.class.getName()).log(Level.WARNING, "Missing resource for: " + key, mre);
            return key;
        }
    }
}
