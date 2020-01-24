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
package jgnash.uifx.views.main;

import java.io.File;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

import jgnash.engine.EngineFactory;
import jgnash.uifx.Options;
import jgnash.uifx.actions.DatabasePathAction;
import jgnash.uifx.control.IntegerTextField;
import jgnash.uifx.tasks.BootEngineTask;

/**
 * FXML Controller for opening a file or database.
 *
 * @author Craig Cavanaugh
 */
public class OpenDatabaseController {

    private static final String LAST_DIR = "LastDir";

    @FXML
    private Button selectFileButton;

    @FXML
    protected ButtonBar buttonBar;

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
        buttonBar.buttonOrderProperty().bind(Options.buttonOrderProperty());

        setDatabaseField(EngineFactory.getLastDatabase());
        databaseServerField.setText(EngineFactory.getLastHost());
        portField.setInteger(EngineFactory.getLastPort());
        remoteServerCheckBox.setSelected(EngineFactory.getLastRemote());

        localDatabaseField.disableProperty().bind(remoteServerCheckBox.selectedProperty());
        portField.disableProperty().bind(remoteServerCheckBox.selectedProperty().not());
        databaseServerField.disableProperty().bind(remoteServerCheckBox.selectedProperty().not());
        selectFileButton.disableProperty().bind(remoteServerCheckBox.selectedProperty());
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
    protected void handleSelectFileAction() {
        final File file = DatabasePathAction.getFileToOpen();

        if (file != null) {
            Preferences pref = Preferences.userNodeForPackage(OpenDatabaseController.class);
            pref.put(LAST_DIR, file.getParentFile().getAbsolutePath());

            setDatabaseField(file.getAbsolutePath());
        }
    }
}
