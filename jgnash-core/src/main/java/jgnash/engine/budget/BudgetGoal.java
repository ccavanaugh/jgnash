/*
 * jGnash, account personal finance application
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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.engine.budget;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;

import jgnash.engine.MathConstants;

/**
 * Budget Goal Object
 * 
 * 366 days per year are assumed and static for goals. The 366th day will not be used if not a leap year
 * 
 * @author Craig Cavanaugh
 */
public class BudgetGoal implements Cloneable, Serializable {
    
    private static final long serialVersionUID = 1L;

    /** 366 days per year */
    public static final int PERIODS = 366;

    // cache the hash code
    private transient int hash;

    private BigDecimal[] goals;

    private BudgetPeriod budgetPeriod = BudgetPeriod.MONTHLY;

    public BudgetGoal() {
        goals = new BigDecimal[PERIODS];
        Arrays.fill(goals, BigDecimal.ZERO);
    }

    public final BigDecimal[] getGoals() {
        return goals;
    }

    public final void setGoals(final BigDecimal[] goals) {
        if (goals == null) {
            throw new IllegalArgumentException("goals may not be null");
        }

        if (goals.length != PERIODS) {
            throw new IllegalArgumentException("goals must be " + PERIODS + " in length");
        }

        for (int i = 0; i < goals.length; i++) {
            if (goals[i] == null) {
                throw new IllegalArgumentException("goals [" + i + "] may not be null");
            }
        }

        this.goals = goals.clone(); //perform a defensive copy
    }

    /**
     * Returns the entry / display period for this budget goal
     * 
     * @return the BudgetPeriod
     */
    public BudgetPeriod getBudgetPeriod() {
        return budgetPeriod;
    }

    /**
     * Sets the global entry / display period for this budget goal
     * 
     * @param budgetPeriod The budget period
     */
    public void setBudgetPeriod(final BudgetPeriod budgetPeriod) {

        if (budgetPeriod == null) {
            throw new IllegalArgumentException("BudgetPeriod may not be null");
        }

        this.budgetPeriod = budgetPeriod;
    }

    public void setGoal(final int startPeriod, final int endPeriod, final BigDecimal amount) {
        BigDecimal divisor = new BigDecimal(endPeriod - startPeriod + 1);

        BigDecimal portion = amount.divide(divisor, MathConstants.mathContext);

        for (int i = startPeriod; i <= endPeriod; i++) {
            goals[i] = portion;
        }
    }

    public BigDecimal getGoal(final int startPeriod, final int endPeriod) {
        BigDecimal amount = BigDecimal.ZERO;

        // clip to the max number of periods... some locale calendars behave differently
        for (int i = startPeriod; i <= endPeriod && i <= BudgetGoal.PERIODS - 1; i++) {
            amount = amount.add(goals[i]);
        }

        return amount;
    }

    /**
     * Returns a clone of this <code>Budget</code>
     * 
     * @return clone
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        BudgetGoal goal = (BudgetGoal) super.clone();

        // deep copy
        goal.goals = new BigDecimal[PERIODS];
        System.arraycopy(goals, 0, goal.goals, 0, goals.length);

        return goal;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            final int prime = 31;
            h = 1;
            h = prime * h + budgetPeriod.hashCode();
            h = prime * h + Arrays.hashCode(goals);

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

        if (!(obj instanceof BudgetGoal)) {
            return false;
        }

        BudgetGoal other = (BudgetGoal) obj;

        return budgetPeriod == other.budgetPeriod && Arrays.equals(goals, other.goals);
    }
}
