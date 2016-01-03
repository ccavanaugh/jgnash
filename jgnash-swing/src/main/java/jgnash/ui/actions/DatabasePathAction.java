/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import java.awt.Component;
import java.io.File;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import jgnash.engine.DataStoreType;
import jgnash.util.NotNull;
import jgnash.util.ResourceUtils;

/**
 * UI Action to request database path from the user
 *
 * @author Craig Cavanaugh
 */
public class DatabasePathAction {

    public enum Type {
        OPEN,
        NEW
    }

    private static final String LAST_DIR = "LastDir";

    private DatabasePathAction() {
    }

    @SuppressWarnings("SameParameterValue")
    public static String databaseNameAction(final Component parent, final Type type) {
        return databaseNameAction(parent, type, DataStoreType.values());
    }

    public static String databaseNameAction(final Component parent, final Type type, @NotNull final DataStoreType... dataStoreTypes) {

        final String[] ext = new String[dataStoreTypes.length];

        for (int i = 0; i < dataStoreTypes.length; i++) {
            ext[i] = dataStoreTypes[i].getDataStore().getFileExt();
        }

        final ResourceBundle rb = ResourceUtils.getBundle();

        final StringBuilder description = new StringBuilder(rb.getString("Label.jGnashFiles") + " (");

        for (int i = 0; i < dataStoreTypes.length; i++) {
            description.append("*.");
            description.append(dataStoreTypes[i].getDataStore().getFileExt());

            if (i < dataStoreTypes.length - 1) {
                description.append(", ");
            }
        }

        description.append(')');

        Preferences pref = Preferences.userNodeForPackage(DatabasePathAction.class);

        JFileChooser chooser = new JFileChooser(pref.get(LAST_DIR, null));

        FileFilter filter = new DataStoreFilter(description.toString(), ext);
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        chooser.setMultiSelectionEnabled(false);

        chooser.setApproveButtonText(rb.getString("Button.Ok"));

        if (type == Type.OPEN) {
            chooser.setDialogTitle(rb.getString("Title.Open"));
        } else {
            chooser.setDialogTitle(rb.getString("Title.NewFile"));
        }

        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            pref.put(LAST_DIR, chooser.getCurrentDirectory().getAbsolutePath());
            try {
                return chooser.getSelectedFile().getAbsolutePath();
            } catch (Exception e) {
                Logger.getAnonymousLogger().warning(e.toString());
            }
        }
        return "";
    }

    private static class DataStoreFilter extends FileFilter {

        private final String description;

        private final String[] fileExtensions;

        public DataStoreFilter(final String description, final String... extensions) {
            this.description = description;
            this.fileExtensions = extensions;
        }

        @Override
        public boolean accept(final File f) {
            if (f != null) {

                if (f.isDirectory()) {
                    return true;
                }

                String fileName = f.getName();

                final int i = fileName.indexOf('.');

                if (i > 0 && i < fileName.length() - 1) {
                    final String extension = fileName.substring(i + 1);

                    for (String fileExtension : fileExtensions) {
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
}
