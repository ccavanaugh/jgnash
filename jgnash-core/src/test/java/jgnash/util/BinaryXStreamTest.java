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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;

import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test for binary xstream.
 *
 * @author Craig Cavanaugh
 */
public class BinaryXStreamTest {

    private static Path tempFile;

    @Test
    public void testFile() {

        final List<String> stringData = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            stringData.add(UUID.randomUUID().toString());
        }

        final List<Integer> integerData = stringData.stream().map(String::hashCode).collect(Collectors.toList());

        try {
            tempFile = Files.createTempFile("test", "");
        } catch (IOException e) {
            fail(e.toString());
        }


        try (final OutputStream fos = Files.newOutputStream(tempFile)) {
            BinaryStreamDriver bsd = new BinaryStreamDriver();
            XStream xstream = new XStream(bsd);

            try (ObjectOutputStream out = xstream.createObjectOutputStream(fos)) {
                out.writeObject(stringData);
                out.writeObject(integerData);
            }
        } catch (IOException e) {
            fail(e.toString());
        }

        assertTrue(FileMagic.isBinaryXStreamFile(tempFile));
        assertFalse(FileMagic.isOfxV2(tempFile));

        try (InputStream fis = Files.newInputStream(tempFile)) {
            BinaryStreamDriver bsd = new BinaryStreamDriver();
            XStream xstream = new XStream(bsd);

            try (ObjectInputStream in = xstream.createObjectInputStream(fis)) {
                List<?> strings = (List<?>) in.readObject();
                List<?> integers = (List<?>) in.readObject();

                assertArrayEquals(strings.toArray(), stringData.toArray());
                assertArrayEquals(integers.toArray(), integerData.toArray());
            } catch (ClassNotFoundException e) {
                fail(e.toString());
            }
        } catch (IOException e) {
            fail(e.toString());
        }

    }

    @AfterClass
    public static void cleanup() throws IOException {
        Files.deleteIfExists(tempFile);
    }
}
