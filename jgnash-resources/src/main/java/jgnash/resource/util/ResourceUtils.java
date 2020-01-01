/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
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
package jgnash.resource.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * @author Craig Cavanaugh
 */
public class ResourceUtils {

    /**
     * key for locale preference
     */
    private static final String LOCALE = "locale";

    private static final String DEFAULT_RESOURCE_BUNDLE = "jgnash/resource/resource";

    /**
     * Historical path to the preference root
     */
    private static final String PREFERENCE_NODE = "/jgnash/util/Resource";

    private static ResourceBundle resourceBundle;

    static {
        final Preferences p = Preferences.userRoot().node(PREFERENCE_NODE);
        Locale.setDefault(decodeLocale(p.get(LOCALE, "")));
    }

    private ResourceUtils() {
        // Utility class
    }

    public static synchronized ResourceBundle getBundle() {
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
            }
			return MessageFormat.format(getBundle().getString(key), arguments);
        } catch (final MissingResourceException mre) {
            Logger.getLogger(ResourceUtils.class.getName()).log(Level.WARNING, "Missing resource for: " + key, mre);
            return key;
        }
    }

    /**
     * Sets the new default locale.  This must be called if overridden.
     *
     * @param l The new default locale
     */
    public static void setLocale(final Locale l) {
        Locale.setDefault(l);
        final Preferences p = Preferences.userRoot().node(PREFERENCE_NODE);
        p.put(LOCALE, encodeLocale(l));
        resourceBundle = null;  // force a reload the resource bundle
    }

    private static String encodeLocale(final Locale locale) {
        final StringBuilder buf = new StringBuilder();

        buf.append(locale.getLanguage());
        if (!locale.getCountry().isEmpty()) {
            buf.append('.');
            buf.append(locale.getCountry());
            if (!locale.getVariant().isEmpty()) {
                buf.append('.');
                buf.append(locale.getVariant());
            }
        }
        return buf.toString();
    }

    private static Locale decodeLocale(final String locale) {
        if (locale == null || locale.isEmpty() || locale.equals("null")) {
            return Locale.getDefault();
        } else if (locale.indexOf('.') == -1) {
            return new Locale(locale);
        } else {
            final Pattern pattern = Pattern.compile("\\x2E");

            final String[] array = pattern.split(locale);

            switch (array.length) {
                case 3:
                    return new Locale(array[0], array[1], array[2]);
                case 2:
                    return new Locale(array[0], array[1]);
                default:  // should not happen
                    return Locale.getDefault();
            }
        }
    }
}
