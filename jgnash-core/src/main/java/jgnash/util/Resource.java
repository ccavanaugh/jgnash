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

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class controls the application locale and provides localized
 * strings, keystrokes, graphics, etc.
 *
 * @author Craig Cavanaugh
 */
public class Resource {

    /**
     * stores the loaded resource bundle
     */
    private ResourceBundle resourceBundle;

    private static final Logger logger = Logger.getLogger(Resource.class.getName());

    /**
     * The Resource singleton
     */
    private final static Resource resource;

    static {
        resource = new Resource();
    }

    /**
     * Protected constructor for specifying the resource bundle to use
     */
    private Resource() {
        loadBundle();
    }

    private void loadBundle() {
        resourceBundle = ResourceUtils.getBundle();
    }

    /**
     * Returns an instance of a subclass, Override this method
     *
     * @return An instance of a subclass
     */
    public static Resource get() {
        return resource;
    }

    /**
     * Gets a localized string
     *
     * @param key The key for the localized string
     * @return The localized string
     */
    public String getString(final String key) {
        try {
            return resourceBundle.getString(key);
        } catch (final MissingResourceException mre) {
            logger.log(Level.WARNING, "Missing resource for: " + key, mre);
            return key;
        }
    }

}
