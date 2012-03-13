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
package jgnash.ui.recurring;

import java.awt.EventQueue;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import jgnash.engine.EngineFactory;
import jgnash.engine.recurring.Reminder;
import jgnash.message.Message;
import jgnash.message.MessageBus;
import jgnash.message.MessageChannel;
import jgnash.message.MessageListener;
import jgnash.util.Resource;

/**
 * Table model for recurring iterators.
 *
 * @author Craig Cavanaugh
 * @version $Id: RecurringTableModel.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class RecurringTableModel extends AbstractTableModel implements MessageListener {

    private static final long serialVersionUID = -4775197077477737505L;

    private List<Reminder> reminders;

    private final Resource rb = Resource.get();

    private final String[] names = new String[]{rb.getString("Column.Description"), rb.getString("Column.Freq"), rb.getString("Column.Enabled")};

    private final Class<?>[] classes = new Class<?>[]{String.class, String.class, String.class};

    private final Object lock = new Object();

    private char enabledSymbol = '\u2713';

    /**
     * Creates a new instance of RecurringTableModel
     */
    public RecurringTableModel() {
        registerListeners();

        if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {
            reminders = EngineFactory.getEngine(EngineFactory.DEFAULT).getReminders();
        } else {
            reminders = Collections.emptyList();
        }
    }

    private void registerListeners() {
        MessageBus.getInstance().registerListener(this, MessageChannel.REMINDER, MessageChannel.SYSTEM);
    }

    @Override
    public int getColumnCount() {
        synchronized (lock) {
            return names.length;
        }
    }

    protected void setEnabledSymbol(char symbol) {
        enabledSymbol = symbol;
    }

    protected char getEnabledSymbol() {
        return enabledSymbol;
    }

    protected Reminder getReminderAt(int row) {
        return reminders.get(row);
    }

    @Override
    public String getColumnName(int column) {
        return names[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return classes[column];
    }

    @Override
    public int getRowCount() {
        synchronized (lock) {
            return reminders.size();
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        synchronized (lock) {
            switch (columnIndex) {
                case 0:
                    return reminders.get(rowIndex).getDescription();
                case 1:
                    return reminders.get(rowIndex).getReminderType().toString();
                case 2:
                    if (reminders.get(rowIndex).isEnabled()) {
                        return enabledSymbol;
                    }
                    return null;
                default:
                    return "Error";
            }
        }
    }

    @Override
    public void fireTableDataChanged() {
        synchronized (lock) {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    RecurringTableModel.super.fireTableDataChanged();
                }
            });
        }
    }

    private void loadReminders() {
        synchronized (lock) {
            reminders = EngineFactory.getEngine(EngineFactory.DEFAULT).getReminders();
        }
    }

    @Override
    public void messagePosted(final Message event) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                switch (event.getEvent()) {
                    case REMINDER_ADD:
                    case REMINDER_REMOVE:
                    case FILE_LOAD_SUCCESS:
                        loadReminders();
                        fireTableDataChanged();
                        break;
                    case FILE_CLOSING:
                        synchronized (lock) {
                            reminders = Collections.emptyList();
                        }
                        fireTableDataChanged();
                        break;
                    default:
                        break;
                }
            }
        });
    }
}