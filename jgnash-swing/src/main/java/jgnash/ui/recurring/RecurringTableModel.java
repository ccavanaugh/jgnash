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

import java.awt.EventQueue;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.table.AbstractTableModel;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.recurring.Reminder;
import jgnash.util.Resource;

/**
 * Table model for recurring iterators.
 *
 * @author Craig Cavanaugh
 *
 */
public class RecurringTableModel extends AbstractTableModel implements MessageListener {

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

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            reminders = engine.getReminders();
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

    void setEnabledSymbol(char symbol) {
        enabledSymbol = symbol;
    }

    char getEnabledSymbol() {
        return enabledSymbol;
    }

    Reminder getReminderAt(int row) {
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
            EventQueue.invokeLater(RecurringTableModel.super::fireTableDataChanged);
        }
    }

    private void loadReminders() {
        synchronized (lock) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            reminders = engine.getReminders();
        }
    }

    @Override
    public void messagePosted(final Message event) {
        EventQueue.invokeLater(() -> {
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
        });
    }
}