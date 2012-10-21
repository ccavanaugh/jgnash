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
package jgnash.engine.recurring;

import java.util.Date;

/**
 * Used to wrap a reminder and it's event date to aid sorting and display
 * 
 * @author Craig Cavanaugh
 */
public class PendingReminder implements Comparable<PendingReminder> {

    private Reminder reminder = null;

    /**
     * The date the event should occur
     */
    private final Date eventDate;

    /**
     * The date for the register if a transaction is generated
     */
    private Date commitDate = null;

    /**
     * Marked state of the reminder
     */
    private boolean selected;

    public PendingReminder(Reminder reminder, Date date) {
        this.reminder = reminder;
        eventDate = (Date) date.clone();
        commitDate = (Date) date.clone();
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(PendingReminder o) {
        if (o.reminder == reminder && o.eventDate.equals(eventDate)) {
            return 0;
        }

        if (o.reminder == reminder) {
            return eventDate.compareTo(o.eventDate);
        }

        return reminder.compareTo(o.reminder);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        
        return o instanceof PendingReminder && ((PendingReminder) o).reminder == reminder && ((PendingReminder) o).eventDate.equals(eventDate);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (reminder != null ? reminder.hashCode() : 0);
        return 79 * hash + (eventDate != null ? eventDate.hashCode() : 0);
    }

    /**
     * @return Returns the selected.
     */
    public synchronized final boolean isSelected() {
        return selected;
    }

    /**
     * @param selected The selected to set.
     */
    public synchronized final void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * @return Returns the commitDate.
     */
    public synchronized final Date getCommitDate() {
        return (Date) commitDate.clone();
    }

    /**
     * @return Returns the reminder.
     */
    public synchronized final Reminder getReminder() {
        return reminder;
    }

    @Override
    public String toString() {
        return reminder.getDescription() + " " + eventDate.toString();
    }
}
