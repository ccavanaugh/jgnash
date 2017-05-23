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
package jgnash.convert.imports;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Craig Cavanaugh
 */

public class FilterTest {

    @Test
    public void simpleTest() {

        ImportFilter importFilter = new ImportFilter("/jgnash/imports/tidy.js");

        assertEquals("fuzzy wuzzy", importFilter.processPayee("Fuzzy Wuzzy"));
        assertEquals("hair cut", importFilter.processMemo("Hair Cut"));
        assertEquals("Tidy Memo and Payee fields", importFilter.getDescription());
    }
}
