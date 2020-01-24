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
package jgnash.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 
 * @author t-pa
 */
class CashFlowTest {
    
    @Test
    void testPositiveIRR() {
        CashFlow cashFlow = new CashFlow();
        
        LocalDate today = LocalDate.now();
        cashFlow.add(today, BigDecimal.valueOf(-100));
        cashFlow.add(today.plusDays(365), BigDecimal.valueOf(103));
        
        double irr = cashFlow.internalRateOfReturn();
        assertEquals(0.03, irr,1.e-5);
    }

    @Test
    void testNegativeIRR() {
        CashFlow cashFlow = new CashFlow();
        
        LocalDate today = LocalDate.now();
        cashFlow.add(today, BigDecimal.valueOf(97));
        cashFlow.add(today.minusDays(365), BigDecimal.valueOf(-100));
        
        double irr = cashFlow.internalRateOfReturn();
        assertEquals(-0.03, irr,1.e-5);
    }

    @Test
    void testUglyData() {
        CashFlow cashFlow = new CashFlow();

        cashFlow.add(LocalDate.of(2012, Month.JANUARY, 9), new BigDecimal("0.1"));
        cashFlow.add(LocalDate.of(2012, Month.FEBRUARY, 2), new BigDecimal("1.03"));
        cashFlow.add(LocalDate.of(2012, Month.MARCH, 12), new BigDecimal("1.04"));
        cashFlow.add(LocalDate.of(2012, Month.APRIL, 12), new BigDecimal("0.91"));
        cashFlow.add(LocalDate.of(2012, Month.MAY, 10), new BigDecimal("0.87"));
        cashFlow.add(LocalDate.of(2012, Month.JUNE, 12), new BigDecimal("0.9"));
        cashFlow.add(LocalDate.of(2012, Month.JULY, 12), new BigDecimal("0.84"));
        cashFlow.add(LocalDate.of(2012, Month.AUGUST, 12), new BigDecimal("0.83"));
        cashFlow.add(LocalDate.of(2012, Month.SEPTEMBER, 13), new BigDecimal("0.83"));
        cashFlow.add(LocalDate.of(2012, Month.OCTOBER, 10), new BigDecimal("0.79"));
        cashFlow.add(LocalDate.of(2012, Month.NOVEMBER, 12), new BigDecimal("0.92"));
        cashFlow.add(LocalDate.of(2012, Month.DECEMBER, 2), new BigDecimal("8.885624"));

        double irr = cashFlow.internalRateOfReturn();

        assertEquals(Double.NaN, irr);
    }

}
