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
import java.nio.file.Path;

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
    public static boolean createAttachmentDirectory() {
        boolean result = true;

        File attachmentDirectory = getAttachmentDirectory();

        if (attachmentDirectory != null) {
            if (!attachmentDirectory.exists()) {
                result = attachmentDirectory.mkdirs();
            }
        } else {
            result = false;
        }

        return result;
    }

    public static File getAttachmentDirectory() {
        final File baseFile = new File(EngineFactory.getActiveDatabase());

        return getAttachmentDirectory(baseFile);
    }

    /**
     * Returns the default attachment directory for the given base file
     *
     * @param baseFile base file for attachment directory
     * @return directory for all attachments
     */
    public static File getAttachmentDirectory(final File baseFile) {
        if (baseFile.getParent() != null) {
            return new File(baseFile.getParent() + File.separator + ATTACHMENT_BASE);
        }

        return null;
    }

    /**
     * @see java.nio.file.Path#resolve(java.nio.file.Path)
     * @see java.nio.file.Path#normalize()
     */
    public static File resolve(final File baseFile, final String relativePath) {
        Path basePath = baseFile.toPath();

        return basePath.resolve(relativePath).normalize().toFile();
    }

    public static File resolve(final String relativePath) {
        final File baseFile = new File(EngineFactory.getActiveDatabase());

        return resolve(baseFile, relativePath);
    }

    /**
     * @see java.nio.file.Path#relativize(java.nio.file.Path)
     */
    public static File relativize(final File baseFile, final File attachmentFile) {
        Path basePath = baseFile.toPath();
        Path attachmentPath = attachmentFile.toPath();

        Path relative = basePath.relativize(attachmentPath);

        return relative.toFile();
    }
}
