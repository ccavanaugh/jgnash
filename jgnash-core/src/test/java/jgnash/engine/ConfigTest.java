/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Craig Cavanaugh
 */
public class ConfigTest {

    @Test
    public void test() {
        final float DELTA = 0.0001f;

        Config config = new Config();

        config.setFileVersion(2.01f);

        assertEquals(0.01f, config.getMinorRevision(), DELTA);

        config.setFileVersion(2.02f);
        assertEquals(0.02f, config.getMinorRevision(), DELTA);

        config.setFileVersion(2.03f);
        assertEquals(0.03f, config.getMinorRevision(), DELTA);

        config.setFileVersion(2.04f);
        assertEquals(0.04f, config.getMinorRevision(), DELTA);

        config.setFileVersion(2.1f);
        assertEquals(0.1f, config.getMinorRevision(), DELTA);

        config.setFileVersion(2.2f);
        assertEquals(0.2f, config.getMinorRevision(), DELTA);

        config.setFileVersion(2.3f);
        assertEquals(0.3f, config.getMinorRevision(), DELTA);

        config.setFileVersion(2.14f);
        assertEquals(1.4f, config.getMinorRevision(), DELTA);

        config.setFileVersion(2.17f);
        assertEquals(1.7f, config.getMinorRevision(), DELTA);

        // Avoid 2.2 and 2.20 as file versions until fixed
        config.setFileVersion(2.20f);
        assertEquals(0.2f, config.getMinorRevision(), DELTA);

        config.setFileVersion(2.21f);
        assertEquals(2.1f, config.getMinorRevision(), DELTA);
    }

    @Test
    public void testFileFormat() {
        Config config = new Config();

        assertEquals(Engine.CURRENT_MAJOR_VERSION, config.getMajorFileFormatVersion());

        assertEquals(Engine.CURRENT_MINOR_VERSION, config.getMinorFileFormatVersion());
    }
}
