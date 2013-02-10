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
package jgnash.ui.register.table;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

/**
 * A table header renderer that shows sort column and direction
 *
 * @author Craig Cavanaugh
 *
 */
class SortableTableHeaderRenderer implements TableCellRenderer {

    private final TableCellRenderer tableCellRenderer;

    SortableTableHeaderRenderer(TableCellRenderer tableCellRenderer) {
        this.tableCellRenderer = tableCellRenderer;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = tableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (c instanceof JLabel) {
            JLabel l = (JLabel) c;

            l.setHorizontalTextPosition(SwingConstants.LEADING);

            int index;
            index = ((SortableTableModel) table.getModel()).getSortedColumn();
            index = table.convertColumnIndexToView(index);

            if (index == column) {
                if (((SortableTableModel) table.getModel()).getAscending()) {
                    l.setIcon(UIManager.getLookAndFeelDefaults().getIcon("Table.ascendingSortIcon"));
                } else {
                    l.setIcon(UIManager.getLookAndFeelDefaults().getIcon("Table.descendingSortIcon"));
                }

            } else {
                l.setIcon(UIManager.getLookAndFeelDefaults().getIcon("Table.naturalSortIcon"));
            }
        }

        return c;
    }
}
