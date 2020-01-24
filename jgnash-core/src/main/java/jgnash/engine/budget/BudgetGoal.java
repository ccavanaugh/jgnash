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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.engine.budget;

import jgnash.engine.MathConstants;
import jgnash.time.Period;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Budget Goal Object
 * <p>
 * 366 days per year are assumed and static for goals. The 366th day will not be used if not a leap year
 *
 * @author Craig Cavanaugh
 */
@Entity
@SequenceGenerator(name = "sequence", allocationSize = 10)
public class BudgetGoal implements Cloneable, Serializable {

    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(generator = "sequence", strategy = GenerationType.SEQUENCE)
    public long id;

    /**
     * 366 days per year.
     */
    public static final int PERIODS = 366;

    // cache the hash code
    private transient int hash;

    @SuppressWarnings("UnusedAssignment")
    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn(name = "INDEX")
    private List<BigDecimal> budgetGoals = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "BUDGETPERIOD")
    private Period budgetPeriod = Period.MONTHLY;

    public BudgetGoal() {
        budgetGoals = new ArrayList<>(Collections.nCopies(PERIODS, BigDecimal.ZERO));
    }

    public final BigDecimal[] getGoals() {
        return budgetGoals.toArray(BigDecimal[]::new);
    }

    public final void setGoals(final BigDecimal[] goals) {
        Objects.requireNonNull(goals);

        if (goals.length != PERIODS) {
            throw new IllegalArgumentException("goals must be " + PERIODS + " in length");
        }

        for (int i = 0; i < goals.length; i++) {
            if (goals[i] == null) {
                throw new IllegalArgumentException("goals [" + i + "] may not be null");
            }
        }

        for (int i = 0; i < goals.length; i++) {
            budgetGoals.set(i, goals[i]);
        }
    }

    /**
     * Returns the entry / display period for this budget goal.
     *
     * @return the BudgetPeriod
     */
    public Period getBudgetPeriod() {
        return budgetPeriod;
    }

    /**
     * Sets the global entry / display period for this budget goal.
     *
     * @param budgetPeriod The budget period
     */
    public void setBudgetPeriod(final Period budgetPeriod) {
        this.budgetPeriod = Objects.requireNonNull(budgetPeriod);
    }

    public void setGoal(final int startPeriod, final int endPeriod, final BigDecimal amount, final boolean leapYear) {
        if (startPeriod <= endPeriod) {

            final BigDecimal divisor = new BigDecimal(endPeriod - startPeriod + 1);
            final BigDecimal portion = amount.divide(divisor, MathConstants.budgetMathContext);

            for (int i = startPeriod; i <= endPeriod && i < BudgetGoal.PERIODS; i++) {
                budgetGoals.set(i, portion);
            }
        } else {    // wrap around the array, need to handle a leap year
            final BigDecimal divisor = new BigDecimal(BudgetGoal.PERIODS - startPeriod + endPeriod - (leapYear ? 1 : 0));
            final BigDecimal portion = amount.divide(divisor, MathConstants.budgetMathContext);

            //System.out.println("divisor: " + divisor+ ", start: " + startPeriod + ", end: " + endPeriod + ", leapYear: " + leapYear);

            for (int i = startPeriod; i < BudgetGoal.PERIODS - (leapYear ? 0 : 1); i++) {
                budgetGoals.set(i, portion);
            }

            for (int i = 0; i <= endPeriod && i < BudgetGoal.PERIODS; i++) {
                budgetGoals.set(i, portion);
            }
        }
    }

    public BigDecimal getGoal(final int startPeriod, final int endPeriod, final boolean leapYear) {
        BigDecimal amount = BigDecimal.ZERO;


        if (startPeriod <= endPeriod) {
            // clip to the max number of periods... some locale calendars behave differently
            for (int i = startPeriod; i <= endPeriod && i < BudgetGoal.PERIODS; i++) {
                amount = amount.add(budgetGoals.get(i));
            }
        } else {    // wrap around the array, need to handle a leap year
            //int days = 0;

            for (int i = startPeriod; i < BudgetGoal.PERIODS - (leapYear ? 0 : 1); i++) {
                //days++;
                amount = amount.add(budgetGoals.get(i));
            }

            for (int i = 0; i <= endPeriod && i < BudgetGoal.PERIODS; i++) {
                //days++;
                amount = amount.add(budgetGoals.get(i));
            }

            //System.out.println("days: " + days+ ", start: " + startPeriod + ", end: " + endPeriod + ", leapYear: " + leapYear);

        }

        return amount;
    }

    /**
     * {@inheritDoc}
     * <p>
     * A deep copy of the goals is performed
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        final BudgetGoal goal = (BudgetGoal) super.clone();

        goal.id = 0;    // clones id must be reset for JPA

        // deep copy
        goal.budgetGoals = new ArrayList<>(Collections.nCopies(PERIODS, BigDecimal.ZERO));

        for (int i = 0; i < PERIODS; i++) {
            goal.budgetGoals.set(i, budgetGoals.get(i));
        }

        return goal;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            final int prime = 31;
            h = 1;
            h = prime * h + budgetPeriod.hashCode();
            h = prime * h + budgetGoals.hashCode();

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

        if (!(obj instanceof BudgetGoal)) {
            return false;
        }

        final BudgetGoal other = (BudgetGoal) obj;

        return budgetPeriod == other.budgetPeriod && budgetGoals.equals(other.budgetGoals);
    }
}
