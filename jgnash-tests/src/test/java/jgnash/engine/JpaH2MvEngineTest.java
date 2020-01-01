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
package jgnash.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.jpa.JpaH2MvDataStore;
import jgnash.engine.jpa.SqlUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * H2 Relational database engine test.
 *
 * @author Craig Cavanaugh
 */
public class JpaH2MvEngineTest extends EngineTest {


    @Override
    public Engine createEngine() {
        testFile = "jpa-testmv" + JpaH2MvDataStore.MV_FILE_EXT;

        try {
            testFile = Files.createTempFile("jpa-testmv", JpaH2MvDataStore.MV_FILE_EXT).toString();

        } catch (final IOException ex) {
            Logger.getLogger(JpaH2MvEngineTest.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            fail();
        }

        assertTrue(EngineFactory.deleteDatabase(testFile));

        try {
            return EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                    DataStoreType.H2MV_DATABASE);
        } catch (final Exception e) {
            fail(e.getMessage());
            return null;
        }
    }

    @Test
    void dumpTableAndColumnNames() {
        EngineFactory.closeEngine(EngineFactory.DEFAULT);

        final Set<String> tableNames = SqlUtils.getTableAndColumnNames(testFile);

        tableNames.forEach(System.out::println);
    }
}
