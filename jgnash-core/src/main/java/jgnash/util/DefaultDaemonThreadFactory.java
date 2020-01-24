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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The default thread factory to be used for {@code ExecutorService} that
 * forces threads to be daemons
 * <p>
 * Example usage:
 * {@code
 * ExecutorService pool = Executors.newSingleThreadExecutor(new DefaultDaemonThreadFactory());
 * }
 *
 * @author Craig Cavanaugh
 *
 * @see java.util.concurrent.ExecutorService
 */
public class DefaultDaemonThreadFactory implements ThreadFactory {

    private static final AtomicInteger poolNumber = new AtomicInteger(1);

    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private final String namePrefix;

    public DefaultDaemonThreadFactory(final String description) {
        namePrefix = description + "(" + poolNumber.incrementAndGet() + "), Thread ";
    }

    @Override
    public Thread newThread(final @NotNull Runnable r) {
        Thread t = new Thread(r, namePrefix + threadNumber.incrementAndGet());

        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY);

        return t;
    }
}
