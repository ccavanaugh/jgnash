/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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

package jgnash.message;

import org.apache.commons.codec.binary.Base64;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * A Simple encryption class based on a supplied user and password
 *
 * @author Craig Cavanaugh
 */
public class EncryptionFilter {

    private Key key;

    private static final String ENCRYPTION_ALGORITHM = "AES";

    public static final String DECRYPTION_ERROR_TAG = "<DecryptError>";

    private static Logger logger = Logger.getLogger(EncryptionFilter.class.getName());

    public EncryptionFilter(final char[] password) {
        byte[] encryptionKey = "fake".getBytes();

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            StringBuilder builder = new StringBuilder(new String(password));

            // generate the encryption key
            encryptionKey = md.digest(builder.toString().getBytes());

            builder.delete(0, builder.length() - 1);
        } catch (final NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        key = new SecretKeySpec(encryptionKey, ENCRYPTION_ALGORITHM);
    }

    public String encrypt(final String plain) {

        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            return Base64.encodeBase64String(cipher.doFinal(plain.getBytes()));

        } catch (final InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return null;
    }

    /**
     * Decrypts the supplied string
     *
     * @param encrypted String to decrypt
     * @return The decrypted string of <code>DECRYPTION_ERROR_TAG</code> if decryption fails
     * @see #DECRYPTION_ERROR_TAG
     */
    public String decrypt(final String encrypted) {

        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);

            return new String(cipher.doFinal(Base64.decodeBase64(encrypted)));
        } catch (final InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
            logger.log(Level.SEVERE, "Invalid password");
            return DECRYPTION_ERROR_TAG;
        }
    }
}
