/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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
package jgnash.engine;

import static java.lang.Math.abs;
import java.math.BigDecimal;
import java.time.LocalDate;
import static java.time.temporal.ChronoUnit.DAYS;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores a history of cash flow items and calculates their internal rate of
 * return. It assumes 365 days per year (Actual/365 Fixed day count convention)
 * and uses a simple iterative solver.
 * 
 * @author t-pa
 */
public class CashFlow {
    
    private static final double DAYS_PER_YEAR = 365;
    private static final int MAX_ITERATIONS = 1000;
    
    private static final Logger logger = Logger.getLogger(CashFlow.class.getName());
    
    private static class CashFlowItem {
        final LocalDate date;
        final BigDecimal amount;

        CashFlowItem(final LocalDate date, final BigDecimal amount) {
            this.date = date;
            this.amount = amount;
        }
        
        @Override
        public String toString() {
            return String.format("[%s, %f]", date.toString(), amount);
        }
    }
    
    private final List<CashFlowItem> cashFlows = new ArrayList<>();
    
    /**
     * Add an item to the history of cash flows.
     * 
     * @param date  the date of the cash flow
     * @param amount  the amount; negative for an investment, positive for a payout
     */
    public void add(final LocalDate date, final BigDecimal amount) {
        cashFlows.add(new CashFlowItem(date, amount));
    }
    
    /**
     * Calculate the internal rate of return of the cash flow. If the iterative
     * solution does not converge, NaN is returned.
     * 
     * @return an approximation of the (annualized) internal rate of return
     */
    public double internalRateOfReturn() {
        if (cashFlows.isEmpty()) {
            return 0.0;
        }
        
        // the reference date is arbitrary, but for better numerical accuracy,
        // use one of the actual dates in the cash flow history
        LocalDate referenceDate = cashFlows.get(0).date;
        
        double lastRate = 0.0;
        double lastNPV = netPresentValue(referenceDate, lastRate);

        double rate = (lastNPV > 0) ? 0.05 : -0.05;
        
        // iteratively calculate the IRR with the secant method
        int i = 0;
        boolean hasConverged;
        do {
            double npv = netPresentValue(referenceDate, rate);
            double newRate = rate - npv * (rate - lastRate) / (npv - lastNPV);
            
            lastRate = rate;
            lastNPV = npv;
            rate = newRate;
            
            i++;
            if (rate != 0 || lastRate != 0) {
                hasConverged = abs(rate-lastRate)/(abs(rate)+abs(lastRate)) < 1.e-5;
            } else {
                hasConverged = true;
            }
        } while (!hasConverged && i < MAX_ITERATIONS);
        
        if (!hasConverged) {
            rate = Double.NaN;
            logger.log(Level.INFO, "IRR calculation did not converge. Data: {0}", cashFlows);
        }
        
        return rate;
    }
    
    /**
     * Calculate the net present value of the cash flow.
     * 
     * @param referenceDate  the NPV is relative to this date
     * @param rate  the discount rate
     * @return the net present value
     */
    private double netPresentValue(final LocalDate referenceDate, final double rate) {
        double npv = 0;
        
        for (final CashFlowItem item : cashFlows) {
            double timeDifference = referenceDate.until(item.date, DAYS) / DAYS_PER_YEAR;
            npv += item.amount.doubleValue() / Math.pow(1+rate, timeDifference);
        }
        
        return npv;
    }
}
