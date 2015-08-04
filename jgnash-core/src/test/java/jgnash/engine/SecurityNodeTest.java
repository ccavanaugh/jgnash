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
package jgnash.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Craig Cavanaugh
 */
public class SecurityNodeTest extends AbstractEngineTest {

    @Override
    protected Engine createEngine() {
        database = EngineFactory.getDefaultDatabase() + "-jgnash-test.bxds";
        EngineFactory.deleteDatabase(database);

        return EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, PASSWORD, DataStoreType.BINARY_XSTREAM);
    }

    @Test
    public void TestHistory() {
        BigDecimal securityPrice1 = new BigDecimal("2.00");

        final LocalDate transactionDate1 = LocalDate.of(2009, Month.DECEMBER, 26);

        SecurityHistoryNode history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(securityPrice1);

        assertTrue(e.addSecurityHistory(securityNode1, history));

        assertEquals(1, securityNode1.getHistoryNodes().size());

        // Same date and price, new instance
        history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(securityPrice1);

        assertTrue(e.addSecurityHistory(securityNode1, history));   // should replace
        assertEquals(1, securityNode1.getHistoryNodes().size());

        // Same date, new instance and updated price
        history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(new BigDecimal("2.01"));

        assertTrue(e.addSecurityHistory(securityNode1, history));  // should replace
        assertEquals(1, securityNode1.getHistoryNodes().size());

        final LocalDate transactionDate2 = LocalDate.of(2009, Month.DECEMBER, 27);

        // force a failure by corrupting the date
       /* history.setDate(transactionDate2);
        assertFalse(e.addSecurityHistory(securityNode1, history));  // should fail
        assertEquals(1, securityNode1.getHistoryNodes().size());*/

        history = new SecurityHistoryNode();
        history.setDate(transactionDate2);
        history.setPrice(new BigDecimal("2.02"));
        assertTrue(e.addSecurityHistory(securityNode1, history));  // should be okay
        assertEquals(2, securityNode1.getHistoryNodes().size());
    }
}
