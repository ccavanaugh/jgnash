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
package jgnash.engine.recurring;

import java.time.LocalDate;

import jgnash.util.NotNull;

/**
 * Used to wrap a reminder and it's event date to aid sorting and display.
 * 
 * @author Craig Cavanaugh
 */
public class PendingReminder implements Comparable<PendingReminder> {

    private final Reminder reminder;

    /**
     * The date for the register if a transaction is generated.
     */
    final private LocalDate commitDate;

    /**
     * Approved state of the reminder.
     */
    private boolean approved;

    public PendingReminder(final @NotNull Reminder reminder, final @NotNull LocalDate date) {
        this.reminder = reminder;
        commitDate = date;
    }

    @Override
    public int compareTo(final @NotNull PendingReminder o) {
        if (o.reminder == reminder && o.commitDate.equals(commitDate)) {
            return 0;
        }

        if (o.reminder == reminder) {
            return commitDate.compareTo(o.commitDate);
        }

        return reminder.compareTo(o.reminder);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        
        return o instanceof PendingReminder && ((PendingReminder) o).reminder == reminder && ((PendingReminder) o).commitDate.equals(commitDate);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (reminder != null ? reminder.hashCode() : 0);
        return 79 * hash + (commitDate != null ? commitDate.hashCode() : 0);
    }

    /**
     * Returns true if approved for execution of the {@code Reminder}.
     *
     * @return Returns the approved.
     */
    public synchronized final boolean isApproved() {
        return approved;
    }

    /**
     * Controls approval state of the {@code Reminder}.
     *
     * @param approved The approved to set.
     */
    public synchronized final void setApproved(final boolean approved) {
        this.approved = approved;
    }

    /**
     * Gets the commit date for the associated {@code Reminder}.
     *
     * @return Returns the commitDate.
     */
    public synchronized final LocalDate getCommitDate() {
        return commitDate;
    }

    /**
     * Gets the {@code Reminder}.
     *
     * @return Returns the reminder.
     */
    public synchronized final Reminder getReminder() {
        return reminder;
    }

    @Override
    public String toString() {
        return reminder.getDescription() + " " + commitDate;
    }
}
