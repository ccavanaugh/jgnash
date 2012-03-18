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
package jgnash.engine;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;


import org.junit.Test;

/**
 * @author Craig Cavanaugh
 *
 */
public class Db4oEngineTest extends EngineTest {

    private static final float DELTA = .001f;

    @Override
    public Engine createEngine() {
        testFile = "db4o-account-test.jdb";

        try {
            File temp = File.createTempFile("test", ".jdb");
            temp.deleteOnExit();
            testFile = temp.getAbsolutePath();
        } catch (IOException e1) {          
            System.err.println(e1.toString());
        }

        EngineFactory.deleteDatabase(testFile);

        return EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, DataStoreType.DB4O);
    }

    @Test
    public void testVersion() {
        final String localTestFile = "db4o-version-test.jdb";

        EngineFactory.deleteDatabase(localTestFile);

        EngineFactory.bootLocalEngine(localTestFile, EngineFactory.DEFAULT, DataStoreType.DB4O);

        EngineFactory.closeEngine(EngineFactory.DEFAULT);

        float version = EngineFactory.getFileVersion(new File(localTestFile));

        System.out.println(version);

        assertEquals(version, Engine.CURRENT_VERSION, DELTA);

        EngineFactory.deleteDatabase("db4o-version-test.jdb");
    }
}
