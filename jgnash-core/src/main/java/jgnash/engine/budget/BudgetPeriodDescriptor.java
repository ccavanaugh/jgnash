/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2021 Craig Cavanaugh
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

import java.time.LocalDate;
import java.util.Objects;

import jgnash.resource.util.ResourceUtils;
import jgnash.time.DateUtils;
import jgnash.time.Period;
import jgnash.util.NotNull;

/**
 * Immutable descriptor for a budget period.
 *
 * @author Craig Cavanaugh
 */
public class BudgetPeriodDescriptor implements Comparable<BudgetPeriodDescriptor> {

    private int hash = 0;

    /**
     * The starting period (Day of the year).
     */
    private int startPeriod;

    private final int endPeriod;

    private final LocalDate startDate;

    private final LocalDate endDate;

    private final String periodDescription;

    private final Period budgetPeriod;

    /**
     *
     * @param periodStartDate the user specified start of the budget (fiscal year)
     * @param periodEndDate the starting date for the period
     * @param budgetPeriod the period for the descriptor
     */
    BudgetPeriodDescriptor(final LocalDate periodStartDate, final LocalDate periodEndDate, final Period budgetPeriod) {
        Objects.requireNonNull(budgetPeriod);
        Objects.requireNonNull(periodStartDate);
        Objects.requireNonNull(periodEndDate);

        this.budgetPeriod = budgetPeriod;

        this.startDate = periodStartDate;
        this.endDate = periodEndDate;

        startPeriod = this.startDate.getDayOfYear() - 1;
        endPeriod = this.endDate.getDayOfYear() - 1;


        // correctly handle the roll over of the index
        if (periodEndDate.getYear() > periodStartDate.getYear()) {
            int startYearDays = periodStartDate.lengthOfYear();
            int endYearDays = periodEndDate.lengthOfYear();

            startPeriod += startYearDays - endYearDays + 1; // shift for a leap year/day
        }

        switch (budgetPeriod) {
            case DAILY:
                periodDescription = ResourceUtils.getString("Pattern.NumericDate", DateUtils.asDate(startDate));
                break;
            case WEEKLY:
                periodDescription = ResourceUtils.getString("Pattern.WeekOfYear",
                        DateUtils.getWeekOfTheYear(startDate), startDate.getYear());
                break;
            case BI_WEEKLY:
                periodDescription = ResourceUtils.getString("Pattern.DateRangeShort", DateUtils.asDate(startDate),
                        DateUtils.asDate(endDate));
                break;
            case MONTHLY:
                periodDescription = ResourceUtils.getString("Pattern.MonthOfYear", DateUtils.asDate(startDate));
                break;
            case QUARTERLY:
                periodDescription = ResourceUtils.getString("Pattern.QuarterOfYear",
                        DateUtils.getQuarterNumber(startDate), startDate.getYear());
                break;
            case YEARLY:
                if (periodEndDate.getYear() == periodStartDate.getYear()) {
                    periodDescription = Integer.toString(periodStartDate.getYear());
                } else {
                    periodDescription = ResourceUtils.getString("Pattern.DateRangeShort",
                            DateUtils.asDate(startDate), DateUtils.asDate(endDate));
                }
                break;
            default:
                periodDescription = "";
        }
    }

    public int getStartPeriod() {
        return startPeriod;
    }

    public int getEndPeriod() {
        return endPeriod;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getPeriodDescription() {
        return periodDescription;
    }

    Period getBudgetPeriod() {
        return budgetPeriod;
    }

    /**
     * Determines if the specified date lies within or inclusive of this descriptor period.
     *
     * @param date date to check
     * @return true if between or inclusive
     */
    public boolean isBetween(final LocalDate date) {
        return DateUtils.after(date, startDate) && DateUtils.before(date, endDate);
    }

    @Override
    public String toString() {
        return String.format("BudgetPeriodDescriptor [startDate=%tD, endDate=%tD, periodDescription=%s, budgetPeriod=%s, startPeriod=%3d, endPeriod=%3d]",
                DateUtils.asDate(startDate), DateUtils.asDate(endDate), periodDescription, budgetPeriod, startPeriod, endPeriod);
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            final int prime = 31;

            h = 1;
            h = prime * h + budgetPeriod.hashCode();
            h = prime * h + startPeriod;
            h = prime * h + endPeriod;
            hash = h;
        }
        return h;
    }

    @Override
    public boolean equals(final Object obj) {
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

        return budgetPeriod == other.budgetPeriod && startPeriod == other.startPeriod;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Compares by the start date
     */
    @Override
    public int compareTo(@NotNull final BudgetPeriodDescriptor that) {
        return this.startDate.compareTo(that.getStartDate());
    }
}
