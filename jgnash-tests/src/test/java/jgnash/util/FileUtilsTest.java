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

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * File utilities test.
 *
 * @author Craig Cavanaugh
 */
class FileUtilsTest {

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
    void testFileLock() throws IOException {
        final Path tempFile = Files.createTempFile("temp", null);

        try (final Writer writer = Files.newBufferedWriter(tempFile)) {
            writer.write("test");
        } catch (Exception e) {
            fail();
        }

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
    void testWaitForFileRemoval() throws IOException {
        final Path tempFile = Files.createTempFile("temp", null);

        try (final Writer writer = Files.newBufferedWriter(tempFile)) {
            writer.write("test");
        } catch (Exception e) {
            fail();
        }

        assertTrue(Files.isWritable(tempFile));
        assertTrue(Files.isReadable(tempFile));

        new Thread(() -> {
            try {
                Thread.sleep(500);
                Files.delete(tempFile);
            } catch (final InterruptedException | IOException e) {
                e.printStackTrace();
                fail();
            }
        }).start();

        assertTrue(FileUtils.waitForFileRemoval(tempFile.toString(), 1000));

        Path tempFile2 = Files.createTempFile("temp", null);

        try (final Writer writer = Files.newBufferedWriter(tempFile)) {
            writer.write("test");
        } catch (Exception e) {
            fail();
        }

        assertTrue(Files.isWritable(tempFile2));
        assertTrue(Files.isReadable(tempFile2));

        AtomicBoolean lock = new AtomicBoolean(false);

        new Thread(() -> {
            try {
                Thread.sleep(1500);
                Files.delete(tempFile2);
                lock.set(true);
            } catch (final InterruptedException | IOException e) {
                e.printStackTrace();
                fail();
            }
        }).start();

        assertFalse(FileUtils.waitForFileRemoval(tempFile2.toString(), 750));

        Awaitility.waitAtMost(2000, TimeUnit.MILLISECONDS).untilTrue(lock);
    }

    @Test
    void fileExtensionStripTest() {
        assertTrue(FileUtils.fileHasExtension("text.txt"));
        assertTrue(FileUtils.fileHasExtension("text.txt.txt"));
        assertFalse(FileUtils.fileHasExtension("test"));
    }

    @Test
    void strip() {
        assertEquals("database", FileUtils.stripFileExtension("database.h2.db"));
        assertEquals("database", FileUtils.stripFileExtension("database.xml"));

        assertEquals("database", FileUtils.stripFileExtension("database.xyz"));

        assertEquals("/home/craig/test.test@xyz.com/jgnash files/craig",
                FileUtils.stripFileExtension("/home/craig/test.test@xyz.com/jgnash files/craig.xml"));

        assertEquals("/home/craig/test.test@xyz.com/jgnash files/craig",
                FileUtils.stripFileExtension("/home/craig/test.test@xyz.com/jgnash files/craig.h2.db"));
    }

    @Test
    void fileExtensionText() {
        assertEquals("txt", FileUtils.getFileExtension("test.txt"));

        assertEquals("h2.db",
                FileUtils.getFileExtension("/home/craig/test.test@xyz.com/jgnash files/craig.h2.db"));

        assertEquals("xml", FileUtils.getFileExtension("database.xml"));
    }

    @Test
    void fileCopyToSelf() throws IOException {
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