/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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

import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * @author Craig Cavanaugh
 *
 */
public class DataStoreTest {

    public DataStoreTest() {
    }

    @Test
    public void instanceTest() {
        assertTrue(DataStoreType.H2_DATABASE.getDataStore().toString() != null);
        System.out.println(DataStoreType.H2_DATABASE.getDataStore().toString());

        assertTrue(DataStoreType.XML.getDataStore().toString() != null);
        System.out.println(DataStoreType.XML.getDataStore().toString());
    }
}