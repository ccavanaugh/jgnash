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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Attachment handler for a remote database
 *
 * @author Craig Cavanaugh
 */
public class DistributedAttachmentManager implements AttachmentManager {

    private static final String TEMP_ATTACHMENT_PATH = "jgnash-";
    private final String host;
    private final int port;
    /**
     * Path to temporary attachment cache location
     */
    private Path tempAttachmentPath;
    private AttachmentTransferClient fileClient;

    public DistributedAttachmentManager(final String host, final int port) {
        this.host = host;
        this.port = port;

        try {
            EnumSet<PosixFilePermission> permissions = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE);

            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(permissions);

            tempAttachmentPath = Files.createTempDirectory(TEMP_ATTACHMENT_PATH, attr);

            fileClient = new AttachmentTransferClient(tempAttachmentPath);
        } catch (final IOException e) {
            Logger.getLogger(DistributedAttachmentManager.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    @Override
    public boolean addAttachment(final Path path, boolean copy) throws IOException {

        // Transfer the file to the remote location
        fileClient.sendFile(path.toFile());

        // Determine the cache location the file needs to go to so it does not have to be requested
        Path newPath = Paths.get(tempAttachmentPath + File.separator + path.getFileName());

        // Copy or move the file
        if (copy) {
            Files.copy(path, newPath);
        } else {
            Files.move(path, newPath, StandardCopyOption.ATOMIC_MOVE);
        }

        return true;
    }

    @Override
    public boolean removeAttachment(final Path path) {
        fileClient.deleteFile(path);

        return true;
    }

    @Override
    public Path getAttachment(final String attachment) {

        Path path = Paths.get(tempAttachmentPath + File.separator + Paths.get(attachment).getFileName().toString());

        if (Files.notExists(path)) {
            fileClient.requestFile(Paths.get(attachment));  // Request the file and place in a a temp location

            Date now = new Date();

            while (new Date().getTime() - now.getTime() < 5000) {
                if (Files.exists(path)) {
                    break;
                }

                // TODO, run in background
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Logger.getLogger(DistributedAttachmentManager.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            }
        }

        if (Files.notExists(path)) {
            path = null;
        }

        return path;
    }

    @Override
    public void connectToServer() {
        fileClient.connectToServer(host, port);
    }

    @Override
    public void disconnectFromServer() {
        fileClient.disconnectFromServer();

        // Cleanup before exit
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(tempAttachmentPath)) {
            for (Path p : ds) {
                Files.delete(p);
            }

            Files.delete(tempAttachmentPath);
        } catch (IOException e) {
            Logger.getLogger(DistributedAttachmentManager.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
