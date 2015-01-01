/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * EncryptionManager test
 *
 * @author Craig Cavanaugh
 */
public class EncryptionManagerTest {

    public static final String PASSWORD = RandomStringUtils.random(20);

    @Test
    public void test() {
        final EncryptionManager encryptionManager = new EncryptionManager(PASSWORD.toCharArray());

        for (int i = 1; i < 8192; i++) {
            String testString = RandomStringUtils.random(i);

            final String encrypted = encryptionManager.encrypt(testString);

            final String decrypted = encryptionManager.decrypt(encrypted);

            assertEquals(testString, decrypted);
        }
    }
}
