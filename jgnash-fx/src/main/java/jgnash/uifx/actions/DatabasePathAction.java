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
package jgnash.uifx.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.stage.FileChooser;

import jgnash.engine.DataStoreType;
import jgnash.uifx.views.main.MainView;
import jgnash.resource.util.ResourceUtils;

/**
 * Utility class request database path from the user.
 *
 * @author Craig Cavanaugh
 */
public class DatabasePathAction {

    private static final String LAST_DIR = "LastDir";

    private DatabasePathAction() {
        // utility class
    }

    public static File getFileToSave() {
        final ResourceBundle resources = ResourceUtils.getBundle();

        final FileChooser fileChooser = configureFileChooser();
        fileChooser.setTitle(resources.getString("Title.NewFile"));

        final File file = fileChooser.showSaveDialog(MainView.getPrimaryStage());

        if (file != null) {
            Preferences pref = Preferences.userNodeForPackage(DatabasePathAction.class);
            pref.put(LAST_DIR, file.getParentFile().getAbsolutePath());
        }

        return file;
    }

    public static File getFileToOpen() {
        final ResourceBundle resources = ResourceUtils.getBundle();

        final FileChooser fileChooser = configureFileChooser();
        fileChooser.setTitle(resources.getString("Title.Open"));

        final File file = fileChooser.showOpenDialog(MainView.getPrimaryStage());

        if (file != null) {
            Preferences pref = Preferences.userNodeForPackage(DatabasePathAction.class);
            pref.put(LAST_DIR, file.getParentFile().getAbsolutePath());
        }

        return file;
    }

    private static FileChooser configureFileChooser() {
        final ResourceBundle resources = ResourceUtils.getBundle();

        final Preferences pref = Preferences.userNodeForPackage(DatabasePathAction.class);

        final FileChooser fileChooser = new FileChooser();

        final File initialDirectory = new File(pref.get(LAST_DIR, System.getProperty("user.home")));

        // Protect against an IllegalArgumentException
        if (initialDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        final List<String> types = new ArrayList<>();

        for (final DataStoreType dataStoreType : DataStoreType.values()) {
            types.add("*" + dataStoreType.getDataStore().getFileExt());
        }

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(resources.getString("Label.jGnashFiles"), types)
        );

        for (final DataStoreType dataStoreType : DataStoreType.values()) {
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter(dataStoreType.toString(), "*" + dataStoreType.getDataStore().getFileExt())
            );
        }

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        return fileChooser;
    }
}
