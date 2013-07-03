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

/**
 * Attachment handler for a remote database
 *
 * @author Craig Cavanaugh
 */
public class DistributedAttachmentManager implements AttachmentManager{
    @Override
    public boolean addAttachment(File file) throws IOException {
        // Transfer the file to the remove location
        // Move the file to the temp location and update the cache.

        return false;
    }

    @Override
    public boolean removeAttachment(File file) {
        // Delete the remote file

        return false;
    }

    @Override
    public File getAttachment(String attachment) {
        // Request the file and place in a a temp location
        // Cache the location so we don't have to transfer across the network, check for stale cache info

        // Set file for delete on exit

        // Return the temp file
        return null;
    }
}
