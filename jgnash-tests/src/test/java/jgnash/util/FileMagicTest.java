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
package jgnash.util;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test FileMagic.
 * 
 * @author Craig Cavanaugh
 */
class FileMagicTest {

    @Test
    void testH2Magic() throws URISyntaxException {
        URL url = FileMagicTest.class.getResource("/h2-test.h2.db");

        FileMagic.FileType type = FileMagic.magic(Paths.get(url.toURI()));

        assertEquals(FileMagic.FileType.h2, type);
    }

    /**
     * Test for Ofx version 1 file identification.
     */
    @Test
    void OfxV1Test() {

        URL url = FileMagicTest.class.getResource("/bank1.ofx");

        try {
            assertTrue(FileMagic.isOfxV1(Paths.get(url.toURI())));
        } catch (URISyntaxException ex) {
            Logger.getLogger(FileMagicTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        url = FileMagicTest.class.getResource("/ofx_spec201_stmtrs_example.xml");

        try {
            assertFalse(FileMagic.isOfxV1(Paths.get(url.toURI())));
        } catch (URISyntaxException ex) {
            Logger.getLogger(FileMagicTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Test for Ofx version 1 file encoding.
     */
    @Test
    void OfxV1EncodingTest1() {

        URL url = FileMagicTest.class.getResource("/bank1.ofx");

        try {
            assertEquals("windows-1252", FileMagic.getOfxV1Encoding(Paths.get(url.toURI())));
        } catch (URISyntaxException ex) {
            Logger.getLogger(FileMagicTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Test for Ofx version 1 file encoding.
     */
    @Test
    void OfxV1EncodingTest2() {

        URL url = FileMagicTest.class.getResource("/File_with_Accents.ofx");

        try {
            assertEquals("ISO-8859-1", FileMagic.getOfxV1Encoding(Paths.get(url.toURI())));
        } catch (final URISyntaxException ex) {
            Logger.getLogger(FileMagicTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Test for Ofx version 2 file identification.
     */
    @Test
    void OfxV2Test() {

        URL url = FileMagicTest.class.getResource("/ofx_spec201_stmtrs_example.xml");

        try {
            assertTrue(FileMagic.isOfxV2(Paths.get(url.toURI())));
        } catch (URISyntaxException ex) {
            Logger.getLogger(FileMagicTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        url = FileMagicTest.class.getResource("/bank1.ofx");

        try {
            assertFalse(FileMagic.isOfxV2(Paths.get(url.toURI())));
        } catch (URISyntaxException ex) {
            Logger.getLogger(FileMagicTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
