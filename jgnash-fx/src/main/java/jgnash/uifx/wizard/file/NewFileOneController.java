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
package jgnash.uifx.wizard.file;

import java.io.File;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import jgnash.engine.DataStoreType;
import jgnash.engine.EngineFactory;
import jgnash.uifx.actions.DatabasePathAction;
import jgnash.uifx.control.DataStoreTypeComboBox;
import jgnash.uifx.control.wizard.AbstractWizardPaneController;
import jgnash.uifx.util.ValidationFactory;
import jgnash.util.FileUtils;
import jgnash.util.TextResource;

/**
 * New file wizard panel
 *
 * @author Craig Cavanaugh
 */
public class NewFileOneController extends AbstractWizardPaneController<NewFileWizard.Settings> {

    @FXML
    private TextArea textArea;

    @FXML
    private DataStoreTypeComboBox storageTypeComboBox;

    @FXML
    private Button fileButton;

    @FXML
    private TextField fileNameField;

    @FXML
    private ResourceBundle resources;

    @FXML
    private void initialize() {
        textArea.setText(TextResource.getString("NewFileOne.txt"));
        storageTypeComboBox.setValue(DataStoreType.H2_DATABASE);

        // push this to the end of the EDT, otherwise the dialog does not display correctly
        Platform.runLater(() -> fileNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            checkForOverwrite();
        }));

        updateDescriptor();
    }

    @Override
    public void putSettings(final Map<NewFileWizard.Settings, Object> map) {
        map.put(NewFileWizard.Settings.DATABASE_NAME, fileNameField.getText());
        map.put(NewFileWizard.Settings.TYPE, storageTypeComboBox.getValue());
        map.put(NewFileWizard.Settings.PASSWORD, "");
    }

    @Override
    public void getSettings(final Map<NewFileWizard.Settings, Object> map) {
        DataStoreType type = (DataStoreType) map.get(NewFileWizard.Settings.TYPE);

        if (type != null) {
            storageTypeComboBox.setValue(type);
        }

        final String fileName = (String) map.get(NewFileWizard.Settings.DATABASE_NAME);

        if (FileUtils.fileHasExtension(fileName)) {
            fileNameField.setText(fileName);
        } else {
            fileNameField.setText(fileName + "." + storageTypeComboBox.getValue().getDataStore().getFileExt());
        }

        updateDescriptor();

        Platform.runLater(this::checkForOverwrite);
    }

    @Override
    public boolean isPaneValid() {
        return !fileNameField.getText().isEmpty();
    }

    @Override
    public String toString() {
        return "1. " + resources.getString("Title.DatabaseCfg");
    }

    @FXML
    private void handleFileButtonAction() {
        final File file = DatabasePathAction.getFileToSave();

        if (file != null) {
            fileNameField.setText(file.getAbsolutePath());
        }

        updateDescriptor();
    }

    @FXML
    private void handleDataStoreTypeAction() {
        if (!fileNameField.getText().isEmpty()) {

            Platform.runLater(() -> {
                String fileName = FileUtils.stripFileExtension(fileNameField.getText());
                fileNameField.setText(fileName + "." + storageTypeComboBox.getValue().getDataStore().getFileExt());
            });
        }
    }

    private void checkForOverwrite() {
        ValidationFactory.removeStickyValidation(fileNameField);

        if (EngineFactory.doesDatabaseExist(fileNameField.getText(), storageTypeComboBox.getValue())) {
            ValidationFactory.showValidationWarning(fileNameField, resources.getString("Message.OverwriteDB"), true);
        } else {
            fileNameField.setTooltip(null);
        }
    }
}
