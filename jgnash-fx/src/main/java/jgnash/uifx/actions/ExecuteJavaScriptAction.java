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
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javafx.stage.FileChooser;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.views.main.MainView;

/**
 * Utility class to run a javascript file.
 *
 * @author Craig Cavanaugh
 */
public class ExecuteJavaScriptAction {

    private static final String LAST_DIR = "javaScriptDir";

    private ExecuteJavaScriptAction() {
        // Utility class
    }

    public static void showAndWait() {
        final ResourceBundle resources = ResourceUtils.getBundle();

        final FileChooser fileChooser = configureFileChooser();
        fileChooser.setTitle(resources.getString("Title.SelFile"));

        final File file = fileChooser.showOpenDialog(MainView.getPrimaryStage());

        if (file != null) {
            Preferences pref = Preferences.userNodeForPackage(ExecuteJavaScriptAction.class);
            pref.put(LAST_DIR, file.getParentFile().getAbsolutePath());

            JavaFXUtils.runLater(() -> {
                try (final Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                    new ScriptEngineManager().getEngineByName("nashorn").eval(reader);
                } catch (IOException | ScriptException ex) {
                    Logger.getLogger(ExecuteJavaScriptAction.class.getName()).log(Level.SEVERE, ex.toString(), ex);
                }
            });
        }
    }

    private static FileChooser configureFileChooser() {
        final Preferences pref = Preferences.userNodeForPackage(ExecuteJavaScriptAction.class);
        final FileChooser fileChooser = new FileChooser();

        final File initialDirectory = new File(pref.get(LAST_DIR, System.getProperty("user.home")));

        // Protect against an IllegalArgumentException
        if (initialDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JavaScript Files", "*.js")
        );

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        return fileChooser;
    }
}
