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
package jgnash.engine.budget;

import java.util.Date;
import java.util.Objects;

import jgnash.util.DateUtils;
import jgnash.util.ResourceUtils;

/**
 * Immutable descriptor for a budget period
 * 
 * @author Craig Cavanaugh
 */
public class BudgetPeriodDescriptor {

    private int hash = 0;

    private static final int ONE_WEEK_INCREMENT = 6;

    private static final int TWO_WEEK_INCREMENT = 13;

    private static final int LEAP_WEEK = 53;

    /**
     * The starting period (Day of the year)
     */
    final private int startPeriod;

    private int endPeriod;

    final private Date startDate;

    private Date endDate;

    final private String periodDescription;

    final private BudgetPeriod budgetPeriod;

    final private int budgetYear;

    BudgetPeriodDescriptor(final int budgetYear, final BudgetPeriod budgetPeriod, final int startPeriod) {
        Objects.requireNonNull(budgetPeriod);

        this.budgetYear = budgetYear;
        this.budgetPeriod = budgetPeriod;
        this.startPeriod = startPeriod;

        /* Calendar day 1 is 1.  Need to add 1 to get correct dates */
        startDate = DateUtils.getDateOfTheYear(budgetYear, startPeriod + 1);

        switch (budgetPeriod) {
            case DAILY:
                endDate = startDate;
                endPeriod = startPeriod;

                periodDescription = ResourceUtils.getString("Pattern.NumericDate", startDate);
                break;
            case WEEKLY:
                endDate = DateUtils.addDays(startDate, ONE_WEEK_INCREMENT);
                endPeriod = startPeriod + ONE_WEEK_INCREMENT;

                periodDescription = ResourceUtils.getString("Pattern.WeekOfYear", DateUtils.getWeekOfTheYear(startDate), budgetYear);
                break;
            case BI_WEEKLY:
                if (DateUtils.getWeekOfTheYear(startDate) != LEAP_WEEK) {
                    endDate = DateUtils.addDays(startDate, TWO_WEEK_INCREMENT);
                    endPeriod = startPeriod + TWO_WEEK_INCREMENT;
                } else {
                    endDate = DateUtils.addDays(startDate, ONE_WEEK_INCREMENT);
                    endPeriod = startPeriod + ONE_WEEK_INCREMENT;
                }
                periodDescription = ResourceUtils.getString("Pattern.DateRangeShort", startDate, endDate);
                break;
            case MONTHLY:
                int days = DateUtils.getDaysInMonth(startDate);
                endDate = DateUtils.getLastDayOfTheMonth(startDate);
                endPeriod = startPeriod + days - 1;

                periodDescription = ResourceUtils.getString("Pattern.MonthOfYear", startDate);
                break;
            case QUARTERLY:
                endDate = DateUtils.getLastDayOfTheQuarter(startDate);
                endPeriod = startPeriod + DateUtils.getDifferenceInDays(startDate, endDate);

                periodDescription = ResourceUtils.getString("Pattern.QuarterOfYear", DateUtils.getQuarterNumber(startDate), budgetYear);
                break;
            case YEARLY:
                endDate = DateUtils.getLastDayOfTheYear(startDate);
                endPeriod = startPeriod + DateUtils.getDifferenceInDays(startDate, endDate);

                periodDescription = Integer.toString(budgetYear);                             
                break;    
            default:
                endPeriod = startPeriod;
                endDate = new Date();
                periodDescription = "";
        }

        // Periods especially bi-weekly can get weird, correct ending period if needed.
        if (endPeriod > BudgetGoal.PERIODS) {
            endPeriod = BudgetGoal.PERIODS - 1;
            endDate = DateUtils.getLastDayOfTheYear(startDate);
        }
    }

    public int getStartPeriod() {
        return startPeriod;
    }

    public int getEndPeriod() {
        return endPeriod;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public String getPeriodDescription() {
        return periodDescription;
    }

    public BudgetPeriod getBudgetPeriod() {
        return budgetPeriod;
    }

    /**
     * Determines if the specified date lies within or inclusive of this descriptor period
     * 
     * @param date date to check
     * @return true if between or inclusive
     */
    public boolean isBetween(final Date date) {
        boolean result = false;

        if (DateUtils.after(date, startDate) && DateUtils.before(date, endDate)) {
            result = true;
        }

        return result;
    }

    @Override
    public String toString() {
        return String.format("BudgetPeriodDescriptor [startDate=%tD, endDate=%tD, periodDescription=%s, budgetPeriod=%s]", startDate, endDate, periodDescription, budgetPeriod);
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            final int prime = 31;

            h = 1;
            h = prime * h + budgetPeriod.hashCode();
            h = prime * h + budgetYear;
            h = prime * h + startPeriod;
            hash = h;
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        
        if (obj == null) {
            return false;
        }
        
        if (!(obj instanceof BudgetPeriodDescriptor)) {
            return false;
        }
        
        BudgetPeriodDescriptor other = (BudgetPeriodDescriptor) obj;

        return budgetPeriod == other.budgetPeriod && budgetYear == other.budgetYear && startPeriod == other.startPeriod;
    }
}
