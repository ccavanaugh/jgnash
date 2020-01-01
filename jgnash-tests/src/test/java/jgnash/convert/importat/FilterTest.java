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
package jgnash.convert.importat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Craig Cavanaugh
 */

class FilterTest {

    @Test
    void simpleTest() {

        ImportFilter importFilter = new ImportFilter("/jgnash/convert/scripts/tidy.js");

        ImportTransaction importTransaction = new ImportTransaction();
        importTransaction.setMemo("test");

        importFilter.acceptTransaction(importTransaction);

        assertEquals("Fuzzy Wuzzy", importFilter.processPayee("FuZZy WUzzY"));
        assertEquals("Hair cut", importFilter.processMemo("HaIR cUT"));
        assertEquals("Tidy Memo and Payee fields", importFilter.getDescription());
    }
}
