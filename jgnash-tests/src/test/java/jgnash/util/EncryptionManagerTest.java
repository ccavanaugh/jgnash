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

import org.apache.commons.text.RandomStringGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * EncryptionManager test.
 *
 * @author Craig Cavanaugh
 */
class EncryptionManagerTest {

    private static final RandomStringGenerator generator = new RandomStringGenerator.Builder().build();

    private static final String PASSWORD = generator.generate(20);

    @Test
    void test() {
        final EncryptionManager encryptionManager = new EncryptionManager(PASSWORD.toCharArray());

        for (int i = 1; i < 8192; i++) {
            String testString = generator.generate(i);

            final String encrypted = encryptionManager.encrypt(testString);

            final String decrypted = encryptionManager.decrypt(encrypted);

            assertEquals(testString, decrypted);
        }
    }
}
