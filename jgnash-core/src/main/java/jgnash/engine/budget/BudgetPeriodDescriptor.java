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
package jgnash.engine.budget;

import java.text.MessageFormat;
import java.util.Date;

import jgnash.util.DateUtils;
import jgnash.util.Resource;

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
    private int startPeriod;

    private int endPeriod;

    private Date startDate;

    private Date endDate;

    private String periodDescription;

    private BudgetPeriod budgetPeriod;

    private int budgetYear;

    BudgetPeriodDescriptor(final int budgetYear, final BudgetPeriod budgetPeriod, final int startPeriod) {

        if (budgetPeriod == null) {
            throw new IllegalArgumentException("BudgetPeriod may not be null");
        }

        this.budgetYear = budgetYear;
        this.budgetPeriod = budgetPeriod;
        this.startPeriod = startPeriod;

        /* Calendar day 1 is 1.  Need to add 1 to get correct dates */
        startDate = DateUtils.getDateOfTheYear(budgetYear, startPeriod + 1);

        switch (budgetPeriod) {
            case DAILY:
                endDate = startDate;
                endPeriod = startPeriod;

                periodDescription = MessageFormat.format(Resource.get().getString("Pattern.NumericDate"), startDate);
                break;
            case WEEKLY:
                endDate = DateUtils.addDays(startDate, ONE_WEEK_INCREMENT);
                endPeriod = startPeriod + ONE_WEEK_INCREMENT;

                periodDescription = MessageFormat.format(Resource.get().getString("Pattern.WeekOfYear"), DateUtils.getWeekOfTheYear(startDate), budgetYear);
                break;
            case BI_WEEKLY:
                if (DateUtils.getWeekOfTheYear(startDate) != LEAP_WEEK) {
                    endDate = DateUtils.addDays(startDate, TWO_WEEK_INCREMENT);
                    endPeriod = startPeriod + TWO_WEEK_INCREMENT;
                } else {
                    endDate = DateUtils.addDays(startDate, ONE_WEEK_INCREMENT);
                    endPeriod = startPeriod + ONE_WEEK_INCREMENT;
                }
                periodDescription = MessageFormat.format(Resource.get().getString("Pattern.DateRangeShort"), startDate, endDate);
                break;
            case MONTHLY:
                int days = DateUtils.getDaysInMonth(startDate);
                endDate = DateUtils.getLastDayOfTheMonth(startDate);
                endPeriod = startPeriod + days - 1;

                periodDescription = MessageFormat.format(Resource.get().getString("Pattern.MonthOfYear"), startDate);
                break;
            case QUARTERLY:
                endDate = DateUtils.getLastDayOfTheQuarter(startDate);
                endPeriod = startPeriod + DateUtils.getDifferenceInDays(startDate, endDate);

                periodDescription = MessageFormat.format(Resource.get().getString("Pattern.QuarterOfYear"), DateUtils.getQuarterNumber(startDate), budgetYear);
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
