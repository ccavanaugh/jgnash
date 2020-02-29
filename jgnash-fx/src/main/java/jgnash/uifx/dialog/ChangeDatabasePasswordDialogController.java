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
package jgnash.uifx.dialog;

import java.io.File;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import jgnash.engine.DataStoreType;
import jgnash.engine.jpa.SqlUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.util.FileChooserFactory;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.views.main.MainView;

/**
 * Utility Dialog/Controller for changing the password of a relational database.
 *
 * @author Craig Cavanaugh
 */
public class ChangeDatabasePasswordDialogController {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private Button okayButton;

    @FXML
    private TextField databaseTextField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField verifyPasswordField;

    @FXML
    private ResourceBundle resources;

    @FXML
    void initialize() {
        okayButton.disableProperty().bind(Bindings.notEqual(newPasswordField.textProperty(),
                verifyPasswordField.textProperty()).or(Bindings.isEmpty(databaseTextField.textProperty())));
    }

    @FXML
    private void handleOkAction() {
        ((Stage) parent.get().getWindow()).close();

        final boolean result = SqlUtils.changePassword(databaseTextField.getText(),
                passwordField.getText().toCharArray(), newPasswordField.getText().toCharArray());

        JavaFXUtils.runLater(() -> {
            if (result) {
                StaticUIMethods.displayMessage(resources.getString("Message.CredentialChange"));
            } else {
                StaticUIMethods.displayError(resources.getString("Message.Error.CredentialChange"));
            }
        });
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parent.get().getWindow()).close();
    }

    @FXML
    private void handleDatabaseButtonAction() {
        final FileChooser fileChooser = FileChooserFactory.getDataStoreChooser(DataStoreType.H2_DATABASE,
                DataStoreType.HSQL_DATABASE);

        fileChooser.setTitle(resources.getString("Title.SelFile"));

        final File file = fileChooser.showOpenDialog(MainView.getPrimaryStage());

        if (file != null && file.exists()) {
            databaseTextField.setText(file.getAbsolutePath());
        }
    }
}
