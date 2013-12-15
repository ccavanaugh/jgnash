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
package jgnash.ui.budget;

import java.text.NumberFormat;

import javax.swing.table.TableModel;

/**
 * JTable for displaying budget results using the specified format
 *
 * @author Craig Cavanaugh
 */
class BudgetResultsTable extends AbstractResultsTable {

    private final NumberFormat format;

    public BudgetResultsTable(final TableModel model, final NumberFormat format) {
        super(model);
        this.format = format;
    }

    @Override
    protected NumberFormat getNumberFormat(final int row) {
        return format;
    }
}
