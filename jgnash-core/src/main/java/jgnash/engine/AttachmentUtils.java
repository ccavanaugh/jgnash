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
package jgnash.engine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Support methods for handling attachments
 *
 * @author Craig Cavanaugh
 */
public class AttachmentUtils {

    public static final String ATTACHMENT_BASE = "attachments";

    /**
     * Utility class
     */
    private AttachmentUtils() {
    }

    /**
     * Creates the attachment directory for the active database
     *
     * @return <code>true</code> if and only if the directory was created or if
     *         it already exists; <code>false</code> otherwise
     */
    public static boolean createAttachmentDirectory(final Path baseFile) {
        boolean result = false;

        Path attachmentPath = getAttachmentDirectory(baseFile);

        if (Files.notExists(attachmentPath)) {
            try {
                Files.createDirectories(attachmentPath);
                result = true;
            } catch (IOException e) {
                Logger.getLogger(AttachmentUtils.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
        }

        return result;
    }

    /**
     * Returns the default attachment directory for the given base file
     *
     * @param baseFile base file for attachment directory
     * @return directory for all attachments
     */
    public static Path getAttachmentDirectory(final Path baseFile) {
        if (baseFile.getParent() != null) {
            return Paths.get(baseFile.getParent() + File.separator + ATTACHMENT_BASE);
        }

        return null;
    }

    public static Path getAttachmentPath() {
        return getAttachmentDirectory(Paths.get(EngineFactory.getActiveDatabase()));
    }
}
