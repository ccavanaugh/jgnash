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
package jgnash.engine.attachment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.AttachmentUtils;
import jgnash.engine.EngineFactory;

/**
 * Attachment handler for a local database
 */
public class LocalAttachmentManager implements AttachmentManager {

    @Override
    public boolean addAttachment(File file) throws IOException {

        boolean result = false;

        final File baseFile = new File(EngineFactory.getActiveDatabase());
        final File baseDirectory = AttachmentUtils.getAttachmentDirectory(baseFile);

        boolean directoryExists = true;

        if (!baseDirectory.exists()) {
            directoryExists = baseDirectory.mkdir();
        }

        if (directoryExists) {
            Path newPath = new File(baseDirectory.toString() + File.separator + file.getName()).toPath();

            try {
                Files.move(file.toPath(), newPath, StandardCopyOption.ATOMIC_MOVE);
                result = true;
            } catch (final IOException e) {
                Logger.getLogger(LocalAttachmentManager.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                throw new IOException(e);
            }
        }

        return result;
    }

    @Override
    public boolean removeAttachment(final File file) {
        return file.delete();
    }

    @Override
    public File getAttachment(String attachment) {
        return AttachmentUtils.resolve(attachment);
    }
}
