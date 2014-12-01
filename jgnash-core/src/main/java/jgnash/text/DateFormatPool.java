/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

import jgnash.util.Callable;
import jgnash.util.DateUtils;
import jgnash.util.ObjectPool;

/**
 * DateFormat is not thread safe.  Provides a object pool for reusable DateFormats
 *
 * @author Craig Cavanaugh
 */
public class DateFormatPool {

    private static final ObjectPool<DateFormat> simpleDatePool;

    static {
        simpleDatePool = new ObjectPool<>();
        simpleDatePool.setInstanceCallable(new Callable<DateFormat>() {
            @Override
            public synchronized DateFormat call() {
                return DateUtils.getShortDateFormat();
            }
        });
    }

    private DateFormatPool() {
        // Utility class
    }

    public static DateFormat takeShortDateFormat() {
        return simpleDatePool.take();
    }

    /**
     * {@code DateFormat} must be returned after a take
     *
     * @param dateFormat short {@code DateFormat} to return to the pool
     */
    public static void putShortDateFormat(final DateFormat dateFormat) {
        simpleDatePool.put(dateFormat);
    }
}
