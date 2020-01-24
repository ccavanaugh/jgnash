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

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * A Simple encryption class based on a supplied user and password.
 *
 * @author Craig Cavanaugh
 */
public class EncryptionManager {

    private final Key key;

    //public static final String ENCRYPTION_FLAG = "encrypt";

    private static final String ENCRYPTION_ALGORITHM = "AES";

    public static final String DECRYPTION_ERROR_TAG = "<DecryptError>";

    private static final Logger logger = Logger.getLogger(EncryptionManager.class.getName());

    public EncryptionManager(final char[] password) {
        byte[] encryptionKey = "fake".getBytes(StandardCharsets.UTF_8);

        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");

            final StringBuilder builder = new StringBuilder(new String(password));

            // generate the encryption key
            encryptionKey = md.digest(builder.toString().getBytes(StandardCharsets.UTF_8));

            builder.delete(0, builder.length() - 1);
        } catch (final NoSuchAlgorithmException e) {
            LogUtil.logSevere(EncryptionManager.class, e);
        }

        key = new SecretKeySpec(encryptionKey, ENCRYPTION_ALGORITHM);
    }

    /**
     * Encrypts the supplied string.
     *
     * @param plain String to encrypt
     * @return the encrypted string
     */
    public String encrypt(final String plain) {

        try {
            final Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);

            cipher.init(Cipher.ENCRYPT_MODE, key);

            return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));

        } catch (final InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException
                | IllegalBlockSizeException e) {
            LogUtil.logSevere(EncryptionManager.class, e);
        }

        return null;
    }

    /**
     * Decrypts the supplied string.
     *
     * @param encrypted String to decrypt
     * @return The decrypted string of {@code DECRYPTION_ERROR_TAG} if decryption fails
     * @see #DECRYPTION_ERROR_TAG
     */
    public String decrypt(final String encrypted) {

        try {
            final Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);

            cipher.init(Cipher.DECRYPT_MODE, key);

            return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)), StandardCharsets.UTF_8);
        } catch (final InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException
                | IllegalBlockSizeException e) {
            logger.log(Level.SEVERE, "Invalid password");
            return DECRYPTION_ERROR_TAG;
        }
    }
}
