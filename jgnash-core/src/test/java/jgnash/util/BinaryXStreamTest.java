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
package jgnash.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit test for binary xstream
 *
 * @author Craig Cavanaugh
 */
public class BinaryXStreamTest {

    @Test
    public void testFile() {

        List<String> stringData = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            stringData.add(UUID.randomUUID().toString());
        }

        List<Integer> integerData = new ArrayList<>();

        for (String uuid : stringData) {
            integerData.add(uuid.hashCode());
        }

        File tempFile = null;

        try {
            tempFile = Files.createTempFile("test", "").toFile();
            tempFile.deleteOnExit();
        } catch (IOException e) {
            fail(e.toString());
        }


        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
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

        try (FileInputStream fis = new FileInputStream(tempFile)) {
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
}
