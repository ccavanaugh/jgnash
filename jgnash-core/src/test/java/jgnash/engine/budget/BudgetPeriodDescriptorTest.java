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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Craig Cavanaugh
 * @version $Id: BudgetPeriodDescriptorTest.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class BudgetPeriodDescriptorTest {

    @Test
    public void testEncodeToString() {
        BudgetPeriodDescriptor descriptor = new BudgetPeriodDescriptor(2011, BudgetPeriod.MONTHLY, 2);

        assertEquals("2011,MONTHLY,2", BudgetPeriodDescriptor.encodeToString(descriptor));
    }

    @Test
    public void testDecodeFromString() {
        BudgetPeriodDescriptor descriptor = new BudgetPeriodDescriptor(2011, BudgetPeriod.MONTHLY, 2);
        BudgetPeriodDescriptor decoded = BudgetPeriodDescriptor.decodeFromString("2011,MONTHLY,2");

        assertEquals(descriptor, decoded);
    }

}
