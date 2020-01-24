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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package jgnash.engine.budget;

import jgnash.resource.util.ResourceUtils;

/**
 * Pattern Enum.
 *
 * @author Craig Cavanaugh
 */
@SuppressWarnings("unused")
public enum Pattern {
    EveryRow(ResourceUtils.getString("Sequence.EveryRow")),
    EveryOtherRow(ResourceUtils.getString("Sequence.EveryOtherRow")),
    EverySecondRow(ResourceUtils.getString("Sequence.EverySecondRow")),
    EveryThirdRow(ResourceUtils.getString("Sequence.EveryThirdRow")),
    EveryForthRow(ResourceUtils.getString("Sequence.EveryForthRow")),
    EveryFifthRow(ResourceUtils.getString("Sequence.EveryFifthRow"));

    private final transient String description;

    Pattern(final String description) {
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
