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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import jgnash.engine.DataStoreType;
import jgnash.engine.EngineFactory;
import jgnash.uifx.MainApplication;
import jgnash.uifx.control.IntegerTextField;
import jgnash.uifx.tasks.BootEngineTask;

/**
 * FXML Controller for opening a file or database
 *
 * @author Craig Cavanaugh
 */
public class OpenDatabaseController {

    private static final String LAST_DIR = "LastDir";

    @FXML
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
    private void initialize() {        
        updateControlsState();

        setDatabaseField(EngineFactory.getLastDatabase());
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

    private void setDatabaseField(final String database) {
        localDatabaseField.setText(database);
        localDatabaseField.setTooltip(new Tooltip(database));
    }

    @FXML
    protected void handleRemoteAction(final ActionEvent event) {
        updateControlsState();
    }

    @FXML
    protected void handleSelectFileAction(final ActionEvent event) {

        FileChooser fileChooser = new FileChooser();
        configureFileChooser(fileChooser);

        File file = fileChooser.showOpenDialog(MainApplication.getInstance().getPrimaryStage());

        if (file != null) {
            Preferences pref = Preferences.userNodeForPackage(OpenDatabaseController.class);
            pref.put(LAST_DIR, file.getParentFile().getAbsolutePath());

            setDatabaseField(file.getAbsolutePath());
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
