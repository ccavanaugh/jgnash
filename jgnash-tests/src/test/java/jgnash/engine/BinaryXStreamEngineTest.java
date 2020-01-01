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
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;

/**
 * Engine text for Binary XStream.
 *
 * @author Craig Cavanaugh
 */
public class BinaryXStreamEngineTest extends EngineTest {

    private static String tempFile;

    @Override
    public Engine createEngine() {
        try {
            testFile = Files.createTempFile("jgnash-", DataStoreType.BINARY_XSTREAM.getDataStore().getFileExt())
                    .toString();

            tempFile = testFile;

        } catch (final IOException e1) {
            Logger.getLogger(BinaryXStreamEngineTest.class.getName()).log(Level.SEVERE, e1.getLocalizedMessage(), e1);
        }

        EngineFactory.deleteDatabase(testFile);

        return EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                DataStoreType.BINARY_XSTREAM);
    }

    @AfterAll
    static void cleanup() throws IOException {
        Files.deleteIfExists(Paths.get(tempFile));
    }
}
