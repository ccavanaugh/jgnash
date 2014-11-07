/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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

import jgnash.engine.jpa.JpaH2DataStore;
import jgnash.engine.jpa.JpaHsqlDataStore;
import jgnash.engine.jpa.SqlUtils;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Craig Cavanaugh
 */
public class JpaHsqlEngineTest extends EngineTest {

    private static final float DELTA = .001f;

    @Override
    public Engine createEngine() throws Exception {
        testFile = "jpa-test." + JpaH2DataStore.FILE_EXT;

        try {
            testFile = Files.createTempFile("jpa-test", "." + JpaHsqlDataStore.FILE_EXT).toString();
        } catch (IOException ex) {
            Logger.getLogger(JpaHsqlEngineTest.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            fail();
        }

        Files.delete(Paths.get(testFile));

        try {
            return EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD, DataStoreType.HSQL_DATABASE);
        } catch (final Exception e) {
            fail(e.getMessage());
            return null;
        }
    }

    /*@After
    public void cleanup() throws IOException {
        JpaHsqlDataStore.deleteDatabase(testFile);
    }*/

    @Test
    public void dumpTableAndColumnNames() {
        EngineFactory.closeEngine(EngineFactory.DEFAULT);

        Set<String> tableNames = SqlUtils.getTableAndColumnNames(testFile, PASSWORD);

        for (final String tableName : tableNames) {
            System.out.println(tableName);
        }
    }

    @Test
    public void testVersion() {
        try {
            RootAccount account = e.getRootAccount();

            Account temp = e.getStoredObjectByUuid(RootAccount.class, account.getUuid());
            assertEquals(account, temp);

            // close and reopen to force check for persistence
            EngineFactory.closeEngine(EngineFactory.DEFAULT);

            float version = EngineFactory.getFileVersion(new File(testFile), PASSWORD);

            System.out.println(version);

            assertEquals(version, Engine.CURRENT_VERSION, DELTA);
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }
}
