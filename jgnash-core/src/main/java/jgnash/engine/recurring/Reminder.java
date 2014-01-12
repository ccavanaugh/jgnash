/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.StoredObject;
import jgnash.engine.Transaction;
import jgnash.util.DateUtils;

import javax.persistence.*;

/**
 * This is an abstract class for scheduled reminders.
 *
 * @author Craig Cavanaugh
 */
@Entity
public abstract class Reminder extends StoredObject implements Comparable<Reminder> {

    /**
     * Number of days to notify in advance
     */
    private int daysAdvance;

    /**
     * Create the transaction automatically
     */
    private boolean autoCreate;

    /**
     * Display notice of the automatically created transaction
     */
    @Deprecated
    @SuppressWarnings("unused")
    private boolean autoCreateNotify;

    /**
     * Description for this reminder
     */
    private String description;

    /**
     * Enabled state
     */
    private boolean enabled = true;

    /**
     * The last date the reminder will stop executing, may be null (Bug #2860259)
     */
    @Temporal(TemporalType.DATE)
    private Date endDate = null;

    /**
     * Number of periods to increment between events
     */
    private int increment = 0;

    /**
     * The last date the reminder was executed
     * It should remain an increment of the iterator
     * for correct operation
     */
    @Temporal(TemporalType.DATE)
    private Date lastDate = null;

    /**
     * Notes for this reminder
     */
    private String notes = null;

    /**
     * Delete on completion
     */
    @Deprecated
    @SuppressWarnings("unused")
    private boolean removable = false;

    /**
     * The start date of this reminder
     */
    @Temporal(TemporalType.DATE)
    private Date startDate = new Date();

    @ManyToOne
    private Account account;

    /**
     * Reference to the transaction for this reminder.
     */
    @OneToOne(optional = true, orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private Transaction transaction;

    @Override
    public int compareTo(final Reminder reminder) {

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
     * @return Returns the advanceRemindDays.
     */
    public int getDaysAdvance() {
        return daysAdvance;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return Returns the last date the reminder should execute.
     */
    public Date getEndDate() {
        if (endDate != null) {
            return (Date) endDate.clone();
        }
        return null;
    }

    /**
     * @return Returns the iterator.
     */
    public abstract RecurringIterator getIterator();

    /**
     * @return Returns a clone of the lastDate.
     */
    public Date getLastDate() {
        if (lastDate != null) {
            return (Date) lastDate.clone();
        }
        return null;
    }

    /**
     * @return Returns a clone of the startDate.
     */
    public Date getStartDate() {
        if (startDate != null) {
            return (Date) startDate.clone();
        }
        return null;
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

    /**
     * Overrides the super and returns the object id.
     *
     * @return returns the hashCode;
     */
    @Override
    public final int hashCode() {
        return getUuid().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }      
        
        return obj != null && getClass() == obj.getClass() && this.getUuid().equals(((Reminder) obj).getUuid());
    }

    /**
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
     * @param advanceRemindDays The advanceRemindDays to set.
     */
    public void setDaysAdvance(final int advanceRemindDays) {
        this.daysAdvance = advanceRemindDays;
    }

    /**
     * @param autoCreate The autoCreate to set.
     */
    public void setAutoCreate(final boolean autoCreate) {
        this.autoCreate = autoCreate;
    }

    /**
     * @param description The description to set.
     */
    public void setDescription(final String description) {
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
     * @param endDate The last date the reminder should execute.
     */
    public void setEndDate(final Date endDate) {
        if (endDate != null) {
            this.endDate = (Date) endDate.clone();
        } else {
            this.endDate = null;
        }
    }

    /**
     * @param lastDate The lastDate to set.
     */
    private void setLastDate(final Date lastDate) {
        this.lastDate = (Date) lastDate.clone();
    }

    /**
     * Sets the last date fired to the next iterator date in the series
     */
    public void setLastDate() {
        setLastDate(getIterator().next());
    }

    /**
     * @param startDate The startDate to set.
     */
    public void setStartDate(final Date startDate) {
        this.startDate = DateUtils.trimDate(startDate);
    }

    /**
     * @param transaction The transaction to set.
     */
    public void setTransaction(final Transaction transaction) {
        this.transaction = transaction;
    }

    /**
     * @return Returns the increment.
     */
    public int getIncrement() {
        return increment;
    }

    /**
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
     * @param notes The notes to set.
     */
    public void setNotes(final String notes) {
        this.notes = notes;
    }

    /**
     * @return Returns the notes.
     */
    public String getNotes() {
        return notes;
    }

    /**
     * @param account base account for this Reminder
     */
    public void setAccount(final Account account) {
        this.account = account;
    }

    /**
     * @return Returns the accountId.
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
            r.setTransaction(getTransaction());
        }

        return r;
    }
}
