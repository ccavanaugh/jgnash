/*
 * jGnash, a personal finance application
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
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.imports.ofx;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

/**
 * JUnit 4 test class
 * 
 * @author Craig Cavanaugh
 *
 */

public class OfxConvertTest {

    @Test
    public void parseBankOne() {

        InputStream stream = null;
        try {
            stream = new Object().getClass().getResourceAsStream("/bank1.ofx");

            String result = OfxV1ToV2.convertToXML(stream);

            System.out.println(result);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(OfxConvertTest.class.getName()).log(Level.SEVERE, null, ex);
                assertTrue(false);
            }
        }
        assertTrue(true);
    }
}
