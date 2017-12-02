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

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author t-pa
 */
public class CashFlowTest {
    
    @Test
    public void testPositiveIRR() {
        CashFlow cashFlow = new CashFlow();
        
        LocalDate today = LocalDate.now();
        cashFlow.add(today, BigDecimal.valueOf(-100));
        cashFlow.add(today.plusDays(365), BigDecimal.valueOf(103));
        
        double irr = cashFlow.internalRateOfReturn();
        Assert.assertEquals(irr, 0.03, 1.e-5);
    }

    @Test
    public void testNegativeIRR() {
        CashFlow cashFlow = new CashFlow();
        
        LocalDate today = LocalDate.now();
        cashFlow.add(today, BigDecimal.valueOf(97));
        cashFlow.add(today.minusDays(365), BigDecimal.valueOf(-100));
        
        double irr = cashFlow.internalRateOfReturn();
        Assert.assertEquals(irr, -0.03, 1.e-5);
    }

}
