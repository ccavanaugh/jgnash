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
package jgnash.uifx.net;

import java.net.PasswordAuthentication;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import jgnash.net.AbstractAuthenticator;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.util.JavaFXUtils;

/**
 * An Authenticator that will pop up a dialog and ask for http authentication
 * info if it has not assigned. This does not make authentication information
 * permanent. That must be done using the options configuration for http connect
 *
 * @author Craig Cavanaugh
 */
public class NetworkAuthenticator extends AbstractAuthenticator {

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        final Preferences auth = Preferences.userRoot().node(NODEHTTP);

        final ResourceBundle resources = ResourceUtils.getBundle();

        final char[][] pass = {null};
        final String[] user = new String[1];

        // get the password
        if (auth.get(HTTPPASS, null) != null && !auth.get(HTTPPASS, null).isEmpty()) {
            pass[0] = auth.get(HTTPPASS, null).toCharArray();
        }

        // get the user
        user[0] = auth.get(HTTPUSER, null);
        if (user[0] != null) {
            if (user[0].length() <= 0) {
                user[0] = null;
            }
        }

        // if either returns null, pop a dialog
        if (user[0] == null || pass[0] == null) {

            final Dialog<Pair<String, String>> dialog = new Dialog<>();
            dialog.setTitle(resources.getString("Title.HTTPProxy"));
            dialog.setHeaderText(resources.getString("Message.EnterNetworkAuth"));

            // Set the button types.
            final ButtonType loginButtonType = new ButtonType(resources.getString("Button.Ok"), ButtonBar.ButtonData.OK_DONE);

            ThemeManager.applyStyleSheets(dialog.getDialogPane());

            dialog.getDialogPane().getStyleClass().addAll("dialog");
            dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

            // Create the username and password labels and fields.
            final GridPane grid = new GridPane();
            grid.getStyleClass().addAll("form");

            final TextField userNameField = new TextField();
            final PasswordField passwordField = new PasswordField();

            grid.add(new Label(resources.getString("Label.UserName")), 0, 0);
            grid.add(userNameField, 1, 0);
            grid.add(new Label(resources.getString("Label.Password")), 0, 1);
            grid.add(passwordField, 1, 1);

            // Enable/Disable login button depending on whether a username was entered.
            final Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
            loginButton.setDisable(true);

            // bind the button, must not be empty
            loginButton.disableProperty().bind(userNameField.textProperty().isEmpty());

            dialog.getDialogPane().setContent(grid);

            // Request focus on the username field by default.
            JavaFXUtils.runLater(userNameField::requestFocus);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == loginButtonType) {
                    return new Pair<>(userNameField.getText(), passwordField.getText());
                }
                return null;
            });

            final Optional<Pair<String, String>> result = dialog.showAndWait();

            result.ifPresent(usernamePassword -> {
                user[0] = usernamePassword.getKey();
                pass[0] = usernamePassword.getValue().toCharArray();
            });
        }
        
        return new PasswordAuthentication(user[0], pass[0] != null ? pass[0] : new char[0]);
    }
}
