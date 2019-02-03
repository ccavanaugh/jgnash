/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
package jgnash.ui.actions;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * @author Craig Cavanaugh
 */
class DataStoreFilter extends FileFilter {

    private final String description;

    private final String[] fileExtensions;

    DataStoreFilter(final String description, final String... extensions) {
        this.description = description;
        this.fileExtensions = extensions;
    }

    @Override
    public boolean accept(final File f) {
        if (f != null) {

            if (f.isDirectory()) {
                return true;
            }

            final String fileName = f.getName();

            final int i = fileName.indexOf('.');

            if (i > 0 && i < fileName.length() - 1) {
                final String extension = fileName.substring(i);

                for (final String fileExtension : fileExtensions) {
                    if (extension.equalsIgnoreCase(fileExtension)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
