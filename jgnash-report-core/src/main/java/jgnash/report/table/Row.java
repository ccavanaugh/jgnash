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
package jgnash.report.table;

import jgnash.util.Nullable;

/**
 * Support interface to wrap a row of table data into one object
 *
 * @author Craig Cavanaugh
 */
public abstract class Row<T> {

    private final T object;

    protected Row(@Nullable T object) {
        this.object = object;
    }

    public T getValue() {
        return object;
    }

    /**
     * Returns the value given a column index
     *
     * @param columnIndex column index
     * @return column value
     */
    public abstract Object getValueAt(final int columnIndex);
}
