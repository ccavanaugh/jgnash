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

import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import jgnash.engine.jpa.JpaNetworkServer;
import jgnash.uifx.control.IntegerTextField;
import jgnash.uifx.util.InjectFXML;

/**
 * Utility Dialog/Controller for collection remote database connection information.
 *
 * @author Craig Cavanaugh
 */
public class RemoteConnectionDialogController {

    private static final String LAST_PORT = "lastPort";

    private static final String LAST_HOST = "lastHost";

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private IntegerTextField portTextField;

    @FXML
    private Button okayButton;

    @FXML
    private TextField hostTextField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ResourceBundle resources;

    private boolean result = false;

    @FXML
    void initialize() {
        okayButton.disableProperty().bind(Bindings.isEmpty(hostTextField.textProperty())
                .or(Bindings.isEmpty(portTextField.textProperty())));

        final Preferences preferences = Preferences.userNodeForPackage(RemoteConnectionDialogController.class);

        hostTextField.setText(preferences.get(LAST_HOST, "localhost"));
        portTextField.setInteger(preferences.getInt(LAST_PORT, JpaNetworkServer.DEFAULT_PORT));
    }

    @FXML
    private void handleOkAction() {
        result = true;

        final Preferences preferences = Preferences.userNodeForPackage(RemoteConnectionDialogController.class);
        preferences.put(LAST_HOST, hostTextField.getText());
        preferences.putInt(LAST_PORT, portTextField.getInteger());

        ((Stage) parent.get().getWindow()).close();
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parent.get().getWindow()).close();
    }

    public char[] getPassword() {
        return passwordField.getText().toCharArray();
    }

    public boolean getResult() {
        return result;
    }

    public String getHost() {
        return hostTextField.getText();
    }

    public int getPort() {
        return portTextField.getInteger();
    }
}
