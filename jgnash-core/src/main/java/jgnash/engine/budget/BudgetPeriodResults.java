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
package jgnash.engine.budget;

import java.math.BigDecimal;

/**
 * A simple wrapper for calculated budget results.
 *
 * @author Craig Cavanaugh
 *
 */
public class BudgetPeriodResults {

    private BigDecimal budgeted = BigDecimal.ZERO;

    private BigDecimal change = BigDecimal.ZERO;

    private BigDecimal remaining = BigDecimal.ZERO;

    public BigDecimal getBudgeted() {
        return budgeted;
    }

    public void setBudgeted(final BigDecimal budgeted) {
        this.budgeted = budgeted;
    }

    public BigDecimal getChange() {
        return change;
    }

    public void setChange(final BigDecimal change) {
        this.change = change;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }

    public void setRemaining(final BigDecimal balance) {
        this.remaining = balance;
    }

    @Override
    public String toString() {
        return String.format("Budgeted: %f Change: %f Remaining: %f", budgeted, change, remaining);
    }
}
