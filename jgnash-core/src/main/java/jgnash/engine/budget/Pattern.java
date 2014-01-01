/*
 * jGnash, account personal finance application
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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package jgnash.engine.budget;

import jgnash.util.Resource;

/**
 * Pattern Enum
 *
 * @author Craig Cavanaugh
 */
@SuppressWarnings("unused")
public enum Pattern {
    EveryRow(Resource.get().getString("Sequence.EveryRow")),
    EveryOtherRow(Resource.get().getString("Sequence.EveryOtherRow")),
    EverySecondRow(Resource.get().getString("Sequence.EverySecondRow")),
    EveryThirdRow(Resource.get().getString("Sequence.EveryThirdRow")),
    EveryForthRow(Resource.get().getString("Sequence.EveryForthRow")),
    EveryFifthRow(Resource.get().getString("Sequence.EveryFifthRow"));

    private final transient String description;

    private Pattern(final String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

    public int getIncrement() {
        return this.ordinal() + 1;
    }
}
