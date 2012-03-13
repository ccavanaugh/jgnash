/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.table.TableColumnModel;

/**
 * Sorted version of the table model.
 *
 * @author Craig Cavanaugh
 * @version $Id: SortedRegisterTable.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class SortedRegisterTable extends RegisterTable implements MouseListener {

    public SortedRegisterTable(final AccountTableModel dm) {
        super(dm);
        if (dm instanceof SortableTableModel) {
            getTableHeader().addMouseListener(this);
            getTableHeader().setDefaultRenderer(new SortableTableHeaderRenderer(getTableHeader().getDefaultRenderer()));
        }
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        TableColumnModel colModel = getColumnModel();
        int column = convertColumnIndexToModel(colModel.getColumnIndexAtX(e.getX()));

        if (e.getClickCount() == 1 && column != -1) {
            SortableTableModel model = (SortableTableModel) getModel();
            if (model.isSortable(column)) {
                boolean ascending = true;
                if (model.getSortedColumn() == column) { // already sorted, reverse
                    ascending = !model.getAscending();
                }
                model.sortColumn(column, ascending);
            }
        }
    }    

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }
}