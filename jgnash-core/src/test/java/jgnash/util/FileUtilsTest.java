/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * File utilities test.
 *
 * @author Craig Cavanaugh
 */
public class FileUtilsTest {

    private static void checkTestData(final String testdata, final String absolutepath) throws IOException {
        final char[] buffer = new char[testdata.length()];

        try (final Reader reader = Files.newBufferedReader(Paths.get(absolutepath))) {
            final int read = reader.read(buffer);

            assertEquals(testdata.length(), read);
        }

        assertEquals(testdata, new String(buffer));
    }

    private static void writeTestData(final String testdata, final Path tempfile) throws IOException {
        try (final Writer os = Files.newBufferedWriter(tempfile)) {
            os.write(testdata);
        }
    }

    @Test
    public void testFileLock() throws IOException, InterruptedException {
        final Path tempFile = Files.createTempFile("temp", null);

        final Writer writer = Files.newBufferedWriter(tempFile);

        writer.write("test");
        writer.close();

        assertTrue(Files.isWritable(tempFile));
        assertTrue(Files.isReadable(tempFile));

        final FileLocker filelocker = new FileLocker();
        filelocker.acquireLock(tempFile);

        assertTrue(FileUtils.isFileLocked(tempFile.toString()));

        filelocker.release();

        assertFalse(FileUtils.isFileLocked(tempFile.toString()));

        assertTrue(Files.deleteIfExists(tempFile));
    }

    @Test
    public void fileExtensionStripTest() {
        assertTrue(FileUtils.fileHasExtension("text.txt"));
        assertTrue(FileUtils.fileHasExtension("text.txt.txt"));
        assertFalse(FileUtils.fileHasExtension("test"));
    }

    @Test
    public void strip() {
        assertEquals("database", FileUtils.stripFileExtension("database.h2.db"));
        assertEquals("database", FileUtils.stripFileExtension("database.db"));
    }

    @Test
    public void fileExtensionText() {
        assertEquals(FileUtils.getFileExtension("test.txt"), "txt");
    }

    @Test
    public void fileCopyToSelf() throws IOException {
        Path tempFile = Files.createTempFile("jgnash-test", ".jdb");

        String absolutePath = tempFile.toString();
        String testData = "42fd;lgkjsgj;gfj;slfkgj;";

        // Write the data to a file
        writeTestData(testData, tempFile);
        checkTestData(testData, absolutePath);

        // Copy the file to itself: the file should not be emptied :)
        assertFalse(FileUtils.copyFile(Paths.get(absolutePath), Paths.get(absolutePath)));

        Files.delete(tempFile);
    }
}