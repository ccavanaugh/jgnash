/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.uifx.views.register;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.StackPane;

/**
 * Register pane
 *
 * @author Craig Cavanaugh
 */
public class RegisterPaneController implements Initializable {

    //private ResourceBundle resources;

    @FXML
    public StackPane register;

    @FXML
    public ButtonBar buttonBar;

    @FXML
    public StackPane forms;

    /**
     * Active account for the pane
     */
    private ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private RegisterTableController registerTableController;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        //this.resources = resources;

        final Button newButton = new Button(resources.getString("Button.New"));
        final Button duplicateButton = new Button(resources.getString("Button.Duplicate"));
        final Button jumpButton = new Button(resources.getString("Button.Jump"));
        final Button deleteButton = new Button(resources.getString("Button.Delete"));

        ButtonBar.setButtonData(newButton, ButtonBar.ButtonData.OTHER);
        ButtonBar.setButtonData(duplicateButton, ButtonBar.ButtonData.OTHER);
        ButtonBar.setButtonData(jumpButton, ButtonBar.ButtonData.OTHER);
        ButtonBar.setButtonData(deleteButton, ButtonBar.ButtonData.OTHER);

        buttonBar.getButtons().addAll(newButton, duplicateButton, jumpButton, deleteButton);

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("RegisterTable.fxml"), resources);
            register.getChildren().add(fxmlLoader.load());
            registerTableController = fxmlLoader.getController();

            // Bind  the register pane to this account property
            registerTableController.getAccountProperty().bind(accountProperty);
        } catch (final IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }
}
