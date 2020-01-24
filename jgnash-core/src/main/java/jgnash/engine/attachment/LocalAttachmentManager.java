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
package jgnash.engine.attachment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jgnash.engine.AttachmentUtils;
import jgnash.engine.EngineFactory;
import jgnash.util.FileUtils;

import static jgnash.util.LogUtil.logSevere;

/**
 * Attachment handler for a local database.
 *
 * @author Craig Cavanaugh
 */
public class LocalAttachmentManager implements AttachmentManager {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Add a file attachment.
     * When moving a file, it must be copied and then deleted.  Moves can not be done atomically across file systems
     * which is a high probability.
     *
     * @param path Path to the attachment to add
     * @param copy true if only copying the file
     * @return true if successful
     * @throws IOException thrown if a filesystem error occurs
     */
    @Override
    public boolean addAttachment(final Path path, final boolean copy) throws IOException {

        boolean result = false;

        Path baseFile = Paths.get(EngineFactory.getActiveDatabase());

        if (AttachmentUtils.createAttachmentDirectory(baseFile)) {  // create if needed

            Path newPath = Paths.get(AttachmentUtils.getAttachmentPath() + FileUtils.SEPARATOR + path.getFileName());

            try {
                if (copy) {
                    Files.copy(path, newPath);
                } else {
                    Files.copy(path, newPath);
                    Files.delete(path);
                }
                result = true;
            } catch (final IOException e) {
                logSevere(LocalAttachmentManager.class, e);
                throw new IOException(e);
            }
        }

        return result;
    }

    @Override
    public boolean removeAttachment(final String attachment) {
        boolean result = false;

        Path path = Paths.get(AttachmentUtils.getAttachmentPath() + FileUtils.SEPARATOR + attachment);

        try {
            Files.delete(path);
            result = true;
        } catch (final IOException e) {
            logSevere(LocalAttachmentManager.class, e);
        }

        return result;
    }

    @Override
    public Future<Path> getAttachment(final String attachment) {
        return executorService.submit(() ->
                Paths.get(AttachmentUtils.getAttachmentPath() + FileUtils.SEPARATOR + attachment));
    }
}
