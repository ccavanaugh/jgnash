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
package jgnash.util;


import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class
 *
 * @author Craig Cavanaugh
 *
 */
public class LogUtils {

    private LogUtils() {
        // utility class
    }

    public static void logStackTrace(final Logger logger, final Level level) {

        StringBuilder trace = new StringBuilder("Stack Trace\n");

        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            trace.append("\tat ").append(element).append('\n');
        }
        
        logger.log(level, trace.toString());
    }
}
