/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.ui.recurring;

import javax.swing.table.AbstractTableModel;
import java.text.DateFormat;
import java.util.List;

import jgnash.engine.recurring.PendingReminder;
import jgnash.util.DateUtils;
import jgnash.util.Resource;

/**
 * TableModel for ReminderObjects
 * 
 * @author Craig Cavanaugh
 *
 */
public class ReminderObjectTableModel extends AbstractTableModel {

    private final Resource rb = Resource.get();

    private final String[] cNames = { rb.getString("Column.Approve"), rb.getString("Column.Description"),
                    rb.getString("Column.Date") };

    private List<PendingReminder> reminders = null;

    private final DateFormat dateFormatter = DateUtils.getShortDateFormat();

    private char enabledSymbol = '\u2713';

    ReminderObjectTableModel(final List<PendingReminder> reminders) {
        this.reminders = reminders;
    }

    /**
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    @Override
    public int getColumnCount() {
        return cNames.length;
    }

    /**
     * @see javax.swing.table.TableModel#getRowCount()
     */
    @Override
    public int getRowCount() {
        return reminders.size();
    }

    @Override
    public String getColumnName(final int column) {
        return cNames[column];
    }

    @Override
    public Class<?> getColumnClass(final int column) {
        return String.class;
    }

    /**
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        switch (columnIndex) {
            case 0:
                if (reminders.get(rowIndex).isSelected()) {
                    return enabledSymbol;
                }
                return null;
            case 1:
                return reminders.get(rowIndex).getReminder().getDescription();
            case 2:
                return dateFormatter.format(reminders.get(rowIndex).getCommitDate());
            default:
                return null;
        }
    }

    void toggleSelectedState(final int index) {
        reminders.get(index).setSelected(!reminders.get(index).isSelected());
        fireTableRowsUpdated(index, index);
    }

    void setEnabledSymbol(final char symbol) {
        enabledSymbol = symbol;
    }

    char getEnabledSymbol() {
        return enabledSymbol;
    }
}
