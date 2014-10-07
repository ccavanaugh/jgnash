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
package jgnash.engine;

import jgnash.util.Resource;

/**
 * Reconciled State of a transaction
 *
 * @author Craig Cavanaugh
 */
public enum ReconciledState {
    CLEARED(Resource.get().getString("State.Cleared")),
    NOT_RECONCILED(Resource.get().getString("State.NotReconciled")),
    RECONCILED(Resource.get().getString("State.Reconciled"));
    private final transient String symbol;

    private ReconciledState(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
