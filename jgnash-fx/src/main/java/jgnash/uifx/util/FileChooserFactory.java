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
package jgnash.uifx.util;

import javafx.stage.FileChooser;

import jgnash.engine.DataStoreType;
import jgnash.resource.util.ResourceUtils;

/**
 * Factory class for FileChoosers
 */
public final class FileChooserFactory {

    /**
     * Returns a {@code FileChooser} configured to filter all known jGnash file types
     * @return a configured FileChooser
     */
    public static FileChooser getDataStoreChooser() {
        return getDataStoreChooser(DataStoreType.values());
    }

    /**
     * Returns a {@code FileChooser} configured to filter the supplied jGnash file types
     *
     * @param types {@code DataStoreType} to filter on
     * @return a configured FileChooser
     */
    public static FileChooser getDataStoreChooser(final DataStoreType... types) {
        final FileChooser fileChooser = new FileChooser();

        final String[] ext = new String[types.length];

        final StringBuilder description = new StringBuilder(ResourceUtils.getString("Label.jGnashFiles") + " (");

        for (int i = 0; i < types.length; i++) {
            ext[i] = "*" + types[i].getDataStore().getFileExt();

            description.append("*");
            description.append(types[i].getDataStore().getFileExt());

            if (i < types.length - 1) {
                description.append(", ");
            }
        }
        description.append(')');

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description.toString(), ext));

        return fileChooser;
    }

    private FileChooserFactory() {
        // Utility class
    }
}
