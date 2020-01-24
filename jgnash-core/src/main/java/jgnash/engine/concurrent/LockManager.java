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

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Lock manger for all engine operations.
 *
 * Locks may be local or distributed depending on connection type
 *
 * @author Craig Cavanaugh
 */
public interface LockManager {

    /**
     * Returns a named {@link ReentrantReadWriteLock}.
     * Locks are cached and reused
     *
     * @param lockId id of the lock
     *
     * @return a new or cached ReentrantReadWriteLock
     */
    ReentrantReadWriteLock getLock(final String lockId);

}
