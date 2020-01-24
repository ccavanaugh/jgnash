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
package jgnash.uifx.dialog.options;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;

import jgnash.net.ConnectionFactory;
import jgnash.uifx.control.IntegerTextField;
import jgnash.uifx.net.NetworkAuthenticator;

/**
 * Controller for Network Options.
 *
 * @author Craig Cavanaugh
 */
public class NetworkTabController {

    @FXML
    private CheckBox useProxyCheckBox;

    @FXML
    private TextField hostTextField;

    @FXML
    private IntegerTextField portTextField;

    @FXML
    private CheckBox requireAuthCheckBox;

    @FXML
    private TextField userNameTextField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Spinner<Integer> timeoutSpinner;

    @FXML
    private void initialize() {

        timeoutSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                ConnectionFactory.MIN_TIMEOUT, ConnectionFactory.MAX_TIMEOUT,
                ConnectionFactory.getConnectionTimeout(), 1));

        hostTextField.disableProperty().bind(useProxyCheckBox.selectedProperty().not());
        portTextField.disableProperty().bind(useProxyCheckBox.selectedProperty().not());

        userNameTextField.disableProperty().bind(requireAuthCheckBox.selectedProperty().not());
        passwordField.disableProperty().bind(requireAuthCheckBox.selectedProperty().not());

        useProxyCheckBox.setSelected(NetworkAuthenticator.isProxyUsed());
        hostTextField.setText(NetworkAuthenticator.getHost());
        portTextField.setInteger(NetworkAuthenticator.getPort());

        requireAuthCheckBox.setSelected(NetworkAuthenticator.isAuthenticationUsed());
        userNameTextField.setText(NetworkAuthenticator.getName());
        passwordField.setText(NetworkAuthenticator.getPassword());

        useProxyCheckBox.selectedProperty().addListener((observable, oldValue, newValue)
                -> NetworkAuthenticator.setUseProxy(newValue));

        hostTextField.textProperty().addListener((observable, oldValue, newValue)
                -> NetworkAuthenticator.setHost(newValue));

        portTextField.textProperty().addListener((observable, oldValue, newValue)
                -> NetworkAuthenticator.setPort(portTextField.getInteger()));

        requireAuthCheckBox.selectedProperty().addListener((observable, oldValue, newValue)
                -> NetworkAuthenticator.setUseAuthentication(newValue));

        userNameTextField.textProperty().addListener((observable, oldValue, newValue)
                -> NetworkAuthenticator.setName(newValue));

        passwordField.textProperty().addListener((observable, oldValue, newValue)
                -> NetworkAuthenticator.setPassword(newValue));

        timeoutSpinner.getValueFactory().valueProperty().addListener((observable, oldValue, newValue)
                -> ConnectionFactory.setConnectionTimeout(newValue));
    }
}
