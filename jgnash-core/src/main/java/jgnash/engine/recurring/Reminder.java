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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import jgnash.engine.Account;
import jgnash.engine.StoredObject;
import jgnash.engine.Transaction;
import jgnash.util.NotNull;
import jgnash.util.Nullable;

/**
 * This is an abstract class for scheduled reminders.
 *
 * @author Craig Cavanaugh
 */
@Entity
public abstract class Reminder extends StoredObject implements Comparable<Reminder> {

    /**
     * Number of days to notify in advance.
     */
    private int daysAdvance;

    /**
     * Create the transaction automatically.
     */
    private boolean autoCreate;

    /**
     * Description for this reminder.
     */
    private String description;

    /**
     * Enabled state.
     */
    private boolean enabled = true;

    /**
     * The last date the reminder will stop executing, may be null (Bug #2860259).
     */
    private LocalDate endDate = null;

    /**
     * Number of periods to increment between events.
     */
    private int increment = 1;

    /**
     * The last date the reminder was executed.
     * <p>
     * It should remain an increment of the iterator for correct operation.
     */
    private LocalDate lastDate = null;

    /**
     * Notes for this reminder.
     */
    private String notes = null;

    /**
     * The start date of this reminder.
     */
    private LocalDate startDate = LocalDate.now();

    @ManyToOne
    private Account account;

    /**
     * Reference to the transaction for this reminder.
     */
    @OneToOne(orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private Transaction transaction;

    @Override
    public int compareTo(@NotNull final Reminder reminder) {

        int result = description.compareTo(reminder.description);

        if (result != 0) {
            return result;
        }

        result = getReminderType().compareTo(reminder.getReminderType());

        if (result != 0) {
            return result;
        }

        return getUuid().compareTo(reminder.getUuid());
    }

    /**
     * Gets the number of days in advance the reminder should execute.
     *
     * @return Returns the advanceRemindDays.
     */
    public int getDaysAdvance() {
        return daysAdvance;
    }

    /**
     * Gets the description for the reminder.
     *
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the ending date for the reminder.
     *
     * @return Returns the last date the reminder should execute.
     */
    public @Nullable LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Gets the {@code Iterator} for this {@code Reminder}.
     *
     * @return Returns the iterator.
     */
    public abstract RecurringIterator getIterator();

    /**
     * Gets the last recorded date for this {@code Reminder}.
     * @return Returns the last recorded date
     */
    public @Nullable LocalDate getLastDate() {
        return lastDate;
    }

    /**
     * Gets the start date for this {@code Reminder}.
     *
     * @return Returns the start date.
     */
    public @NotNull LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Returns a clone of the transaction.  A clone is returned to prevent
     * accidental insertion of the original into the engine.
     *
     * @return Returns the transaction.
     */
    public Transaction getTransaction() {
        if (transaction != null) {
            try {
                return (Transaction) transaction.clone();
            } catch (CloneNotSupportedException ex) {
                Logger.getLogger(Reminder.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    public abstract ReminderType getReminderType();

    @Override
    public boolean equals(final Object other) {
        return this == other ||
                other != null && getClass() == other.getClass() && this.getUuid().equals(((Reminder) other).getUuid());

    }

    /**
     * Returns true is this {@code Reminder} is executed without user interaction.
     *
     * @return Returns the autoCreate.
     */
    public boolean isAutoCreate() {
        return autoCreate;
    }

    /**
     * Getter for property enabled.
     *
     * @return Value of property enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }


    /**
     * Sets the number of days in advance this {@code Reminder} should execute.
     *
     * @param advanceRemindDays The advanceRemindDays to set.
     */
    public void setDaysAdvance(final int advanceRemindDays) {
        this.daysAdvance = advanceRemindDays;
    }

    /**
     * Controls auto entry of the {@code Reminder}.
     * @param autoCreate The autoCreate to set.
     */
    public void setAutoCreate(final boolean autoCreate) {
        this.autoCreate = autoCreate;
    }

    /**
     * Sets the description for this {@code Reminder}.
     *
     * @param description The description to set.
     */
    public void setDescription(@NotNull final String description) {
        Objects.requireNonNull(description);
        this.description = description;
    }

    /**
     * Setter for property enabled.
     *
     * @param enabled New value of property enabled.
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Sets the last date for the iterator in the series.
     *
     * @param endDate The last date the reminder should execute.
     */
    public void setEndDate(final @Nullable LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * Sets the last date fired to the next iterator date in the series.
     *
     * @param lastDate The lastDate to set.
     */
    private void setLastDate(final @NotNull LocalDate lastDate) {
        this.lastDate = lastDate;
    }

    /**
     * Increment the last date fired to the next iterator date in the series.
     */
    public void setLastDate() {
        setLastDate(getIterator().next());
    }

    /**
     * Set the starting date for the reminder.
     *
     * @param startDate The startDate to set.
     */
    public void setStartDate(final @NotNull LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Assign the transaction to be entered upon acceptable of the reminder.
     *
     * @param transaction The transaction to set.
     */
    public void setTransaction(final Transaction transaction) {
        this.transaction = transaction;
    }

    /**
     * Get the Reminders's increment.
     *
     * @return Returns the increment.
     */
    public int getIncrement() {
        return increment;
    }

    /**
     * Set the Reminders's increment.
     *
     * @param increment The increment to set.
     */
    public void setIncrement(final int increment) {
        this.increment = increment;
    }

    @Override
    public String toString() {
        return description;
    }

    /**
     * Sets the Reminder's notes.
     *
     * @param notes The notes to set.
     */
    public void setNotes(final String notes) {
        this.notes = notes;
    }

    /**
     * Get the Reminder's notes.
     *
     * @return Returns the notes.
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Sets the base Account associated with the Reminder.
     *
     * @param account base account for this Reminder
     */
    public void setAccount(final Account account) {
        this.account = account;
    }

    /**
     * Returns the base Account associated with the Reminder.
     *
     * @return Returns the account
     */
    public Account getAccount() {
        return account;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Reminder r = (Reminder) super.clone();

        // create a deep clone
        r.setEndDate(getEndDate());
        r.setStartDate(getStartDate());

        if (getLastDate() != null) {
            r.setLastDate(getLastDate());
        }

        if (getTransaction() != null) {
            r.setTransaction((Transaction) getTransaction().clone());
        }

        return r;
    }
}
