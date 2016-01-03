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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import jgnash.util.OS;
import jgnash.util.ResourceUtils;

/**
 * This class provides localized keystrokes, etc.
 *
 * @author Craig Cavanaugh
 */
public class Resource {

    private Resource() {
        // Utility class
    }

    /**
     * Gets a localized keystroke.
     *
     * @param key KeyStroke key
     * @return localized KeyStroke
     */
    public static KeyStroke getKeyStroke(final String key) {
        String value = ResourceUtils.getString(key);

        // if working on an QSX system, use the meta key instead of the control key
        if (value != null && value.contains("control") && OS.isSystemOSX()) {
            value = value.replace("control", "meta");
        }

        final KeyStroke keyStroke = KeyStroke.getKeyStroke(value);
        if (keyStroke == null && value != null && !value.isEmpty()) {
            Logger.getLogger(Resource.class.getName()).log(Level.WARNING,
                    "The value ''{0}'' for key ''{1}'' is not valid.", new Object[]{value, key});
        }
        return keyStroke;
    }

    /**
     * Gets a localized string, which is one character long and will
     * be automatically transformed into a character.
     *
     * @param key mnemonic key
     * @return char for mnemonic
     */
    public static char getMnemonic(final String key) {
        final String value = ResourceUtils.getString(key);
        if (value == null || value.length() != 1) {
            Logger.getLogger(Resource.class.getName()).log(Level.WARNING,
                    "The value ''{0}'' for key ''{1}'' is not valid.", new Object[]{value, key});
            return 0;
        }
        return value.charAt(0);
    }
}
