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

import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import jgnash.ui.components.FormattedJTable;

/**
 * JTable for displaying budget results using the specified format
 *
 * @author Craig Cavanaugh
 *
 */
public abstract class AbstractResultsTable extends FormattedJTable {

    AbstractResultsTable(TableModel model) {
        super(model);
    }

    abstract protected  NumberFormat getNumberFormat(final int row);

    @Override
    public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {
        Component c = super.prepareRenderer(renderer, row, column);

        if (column == 2) {
            BigDecimal amount = (BigDecimal) getModel().getValueAt(row, column);

            if (!isRowSelected(row) && amount.signum() < 0) {
                c.setForeground(Color.RED);
            }
        }

        ((JLabel) c).setText(getNumberFormat(row).format(getModel().getValueAt(row, column)));

        return c;
    }
}
