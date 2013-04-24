/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
package jgnash.engine.message;

import java.io.Serializable;

/**
 * A Serializable lock state message client must respect
 * @author Craig Cavanaugh
 */
public class LockState implements Serializable {

    private String lockId;

    private boolean locked;

    public LockState(String lockId, boolean locked) {
        this.lockId = lockId;
        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
    }

    public String getLockId() {
        return lockId;
    }

    @Override
    public int hashCode() {
        return lockId.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (other == null || (!(other instanceof LockState))) {
            return false;
        }

        return this.lockId.equals(((LockState) other).getLockId());
    }
}
