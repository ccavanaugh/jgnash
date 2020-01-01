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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.util.FileUtils;
import jgnash.resource.util.OS;

/**
 * Attachment handler for a remote database.
 *
 * @author Craig Cavanaugh
 */
public class DistributedAttachmentManager implements AttachmentManager {

    private static final String TEMP_ATTACHMENT_PATH = "jGnashTemp-";

    private static final int TRANSFER_TIMEOUT = 5000;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private final String host;

    private final int port;

    /**
     * Path to temporary attachment cache location.
     */
    private Path tempAttachmentPath;

    private AttachmentTransferClient fileClient;

    public DistributedAttachmentManager(final String host, final int port) {
        this.host = host;
        this.port = port;

        try {
            if (!OS.isSystemWindows()) {
                final EnumSet<PosixFilePermission> permissions = EnumSet.of(PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE);

                final FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(permissions);

                tempAttachmentPath = Files.createTempDirectory(TEMP_ATTACHMENT_PATH, attr);
            } else {    // windows cannot handle posix permissions
                tempAttachmentPath = Files.createTempDirectory(TEMP_ATTACHMENT_PATH);
            }

            fileClient = new AttachmentTransferClient(tempAttachmentPath);
        } catch (final IOException e) {
            Logger.getLogger(DistributedAttachmentManager.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Add a file attachment.
     * When moving a file, it must be copied and then deleted.  Moves can not be done atomically across file systems
     * which is a high probability.
     *
     * @param path Path to the attachment to add
     * @param copy true if only copying the file
     * @return true if successful
     * @throws IOException thrown if a network error occurs
     */
    @Override
    public boolean addAttachment(final Path path, final boolean copy) throws IOException {

        boolean result = false;

        // Transfer the file to the remote location
        final Future<Void> future = fileClient.sendFile(path);

        // Determine the cache location the file needs to go to so it does not have to be requested
        final Path newPath = Paths.get(tempAttachmentPath + FileUtils.SEPARATOR + path.getFileName());

        if (future != null) {   // if null, path was not valid
            try {
                future.get();  // wait for the transfer to complete
            } catch (final InterruptedException | ExecutionException e) {
                Logger.getLogger(DistributedAttachmentManager.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            }

            // Copy or move the file
            if (copy) {
                Files.copy(path, newPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(path, newPath, StandardCopyOption.REPLACE_EXISTING);
                Files.delete(path);
            }

            result = true;
        }

        return result;
    }

    @Override
    public boolean removeAttachment(final String attachment) {
        fileClient.deleteFile(attachment);
        return true;
    }

    @Override
    public Future<Path> getAttachment(final String attachment) {

        return executorService.submit(() -> {
            Path path = Paths.get(tempAttachmentPath + FileUtils.SEPARATOR + Paths.get(attachment).getFileName());

            if (Files.notExists(path)) {
                fileClient.requestFile(Paths.get(attachment));  // Request the file and place in a a temp location

                final LocalDateTime start = LocalDateTime.now();

                while (Duration.between(start, LocalDateTime.now()).toMillis() < TRANSFER_TIMEOUT) {
                    if (Files.exists(path)) {
                        break;
                    }
                }
            }

            if (Files.notExists(path)) {
                path = null;
            }

            return path;
        });
    }

    public boolean connectToServer(final char[] password) {
        return fileClient.connectToServer(host, port, password);
    }

    public void disconnectFromServer() {
        fileClient.disconnectFromServer();

        // Cleanup before exit
        try (final DirectoryStream<Path> ds = Files.newDirectoryStream(tempAttachmentPath)) {
            for (final Path p : ds) {
                Files.delete(p);
            }

            Files.delete(tempAttachmentPath);
        } catch (final IOException e) {
            Logger.getLogger(DistributedAttachmentManager.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
