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
package jgnash.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Utility class to reduce logging code
 * <p>
 * A fair amount of byte code is generated to log a throwable.
 * <p>
 * This class provides a static method which produces much less byte code when used.
 *
 * @author Craig Cavanaugh
 */
public class LogUtil {

    static {

        try (final InputStream stream = LogUtil.class.getClassLoader()
                .getResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(stream);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private LogUtil() {
        // utility class
    }

    public static void configureLogging() {
        Logger.getLogger(LogUtil.class.getName()).info("Logging configured");
    }

    /**
     * Logs a throwable at Level.SEVERE.
     *
     * @param clazz     calling class
     * @param throwable Throwable to log
     */
    public static void logSevere(final Class<?> clazz, final Throwable throwable) {
        Logger.getLogger(clazz.getName()).log(Level.SEVERE, throwable.getLocalizedMessage(), throwable);
    }

    /**
     * Logs a throwable at Level.SEVERE.
     *
     * @param clazz   calling class
     * @param message error message
     */
    public static void logSevere(final Class<?> clazz, final String message) {
        Logger.getLogger(clazz.getName()).log(Level.SEVERE, message);
    }
}
