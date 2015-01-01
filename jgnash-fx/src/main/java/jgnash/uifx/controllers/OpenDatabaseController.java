/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.uifx.controllers;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import jgnash.engine.DataStoreType;
import jgnash.engine.EngineFactory;
import jgnash.uifx.MainApplication;
import jgnash.uifx.control.IntegerTextField;
import jgnash.uifx.tasks.BootEngineTask;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * FXML Controller for opening a file or database
 *
 * @author Craig Cavanaugh
 */
public class OpenDatabaseController implements Initializable {

    private static final String LAST_DIR = "LastDir";

    private ResourceBundle resources;

    @FXML
    protected TextField localDatabaseField;

    @FXML
    protected CheckBox remoteServerCheckBox;

    @FXML
    protected TextField databaseServerField;

    @FXML
    protected IntegerTextField portField;

    @FXML
    protected PasswordField passwordField;

    @FXML
    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.resources = resources;

        updateControlsState();

        localDatabaseField.setText(EngineFactory.getLastDatabase());
        databaseServerField.setText(EngineFactory.getLastHost());
        portField.setInteger(EngineFactory.getLastPort());
        remoteServerCheckBox.setSelected(EngineFactory.getLastRemote());
    }

    @FXML
    private void cancelAction() {
        ((Stage) localDatabaseField.getScene().getWindow()).close();
    }

    @FXML
    private void okAction() {
        ((Stage) localDatabaseField.getScene().getWindow()).close();

        if (remoteServerCheckBox.isSelected() || localDatabaseField.getText().length() > 0) {
            BootEngineTask.initiateBoot(localDatabaseField.getText(), passwordField.getText().toCharArray(),
                    remoteServerCheckBox.isSelected(), databaseServerField.getText(), portField.getInteger());
        }
    }

    @FXML
    protected void handleRemoteAction(final ActionEvent event) {
        updateControlsState();
    }

    @FXML
    protected void handleSelectFileAction(final ActionEvent event) {

        FileChooser fileChooser = new FileChooser();
        configureFileChooser(fileChooser);

        File file = fileChooser.showOpenDialog(MainApplication.getPrimaryStage());

        if (file != null) {
            Preferences pref = Preferences.userNodeForPackage(OpenDatabaseController.class);
            pref.put(LAST_DIR, file.getParentFile().getAbsolutePath());

            localDatabaseField.setText(file.getAbsolutePath());
        }
    }

    private void updateControlsState() {
        localDatabaseField.setDisable(remoteServerCheckBox.isSelected());
        portField.setDisable(!remoteServerCheckBox.isSelected());
        databaseServerField.setDisable(!remoteServerCheckBox.isSelected());
    }

    private void configureFileChooser(final FileChooser fileChooser) {
        Preferences pref = Preferences.userNodeForPackage(OpenDatabaseController.class);

        fileChooser.setTitle(resources.getString("Title.Open"));

        fileChooser.setInitialDirectory(new File(pref.get(LAST_DIR, System.getProperty("user.home"))));

        List<String> types = new ArrayList<>();

        for (DataStoreType type : DataStoreType.values()) {
            types.add("*." + type.getDataStore().getFileExt());
        }

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(resources.getString("Label.jGnashFiles"), types)
        );

        for (DataStoreType type : DataStoreType.values()) {
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter(type.toString(), "*." + type.getDataStore().getFileExt())
            );
        }

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
    }
}
