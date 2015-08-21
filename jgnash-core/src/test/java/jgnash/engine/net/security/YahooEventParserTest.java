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
package jgnash.engine.net.security;

import jgnash.engine.SecurityHistoryEvent;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.net.security.YahooEventParser;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Set;

import static org.junit.Assert.assertNotNull;

/**
 * @author Craig Cavanaugh
 */
public class YahooEventParserTest {

    @Test
    public void testParser() {

        final SecurityNode ibm = new SecurityNode();
        ibm.setSymbol("IBM");
        ibm.setScale((byte) 2);

        SecurityHistoryNode historyNode = new SecurityHistoryNode(LocalDate.of(1962, Month.JANUARY, 1),
                BigDecimal.TEN, 1000, BigDecimal.TEN, BigDecimal.TEN);

        //ibm.

        Set<SecurityHistoryEvent> events = YahooEventParser.retrieveNew(ibm);

        assertNotNull(events);

    }
}
