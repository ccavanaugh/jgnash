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
package jgnash.engine.concurrent;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Immutable priority object that ensures first-in-first-out if tasks have the same priority.
 *
 * Smaller values will have more priority.
 *
 * @author Craig Cavanaugh
 */
public class Priority implements Comparable<Priority> {

    public static final int SYSTEM = 1;

    public static final int BACKGROUND = 100;

    private static final AtomicLong atomicLongSequence = new AtomicLong(0);

    private final long sequence;

    private final int priority;

    Priority(final int priority) {
        sequence = atomicLongSequence.getAndIncrement();
        this.priority = priority;
    }

    int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(final Priority other) {
        int result = Integer.compare(priority, other.priority);

        if (result == 0) {  // ensure fifo if the priority is the same
            result = (sequence < other.sequence ? -1 : 1);
        }

        return result;
    }
}
