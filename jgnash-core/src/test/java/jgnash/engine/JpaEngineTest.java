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

import jgnash.engine.jpa.JpaDataStore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Craig Cavanaugh
 */
public class JpaEngineTest extends EngineTest {

    private static final float DELTA = .001f;

    @Override
    public Engine createEngine() throws Exception {
        testFile = "jpa-test." + JpaDataStore.FILE_EXT;

        try {
            File temp = File.createTempFile("jpa-test", "." + JpaDataStore.FILE_EXT);
            temp.deleteOnExit();
            testFile = temp.getAbsolutePath();
        } catch (IOException e1) {
            System.err.println(e1.toString());
        }

        EngineFactory.deleteDatabase(testFile);

        try {
            return EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, USER, PASSWORD, DataStoreType.H2_DATABASE);
        } catch (final Exception e) {
            fail(e.getMessage());
            return null;
        }
    }

    @Test
    public void testVersion() {
        final String localTestFile = "jpa-version-test." + JpaDataStore.FILE_EXT;

        EngineFactory.deleteDatabase(localTestFile);

        try {
            EngineFactory.bootLocalEngine(localTestFile, EngineFactory.DEFAULT, USER, PASSWORD, DataStoreType.H2_DATABASE);

            EngineFactory.closeEngine(EngineFactory.DEFAULT);

            float version = EngineFactory.getFileVersion(new File(localTestFile), "", new char[]{});

            System.out.println(version);

            assertEquals(version, Engine.CURRENT_VERSION, DELTA);

            EngineFactory.deleteDatabase(localTestFile);
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }
}
