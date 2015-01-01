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
package jgnash.uifx.control;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * @author Craig Cavanaugh
 */
public class AlertDialogController implements Initializable {

    @FXML
    private Label message;

    @FXML
    private ButtonBar buttonBar;

    private ButtonType buttonType = ButtonType.CANCEL;  // default is cancelled

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    void setContentText(final String contentText) {
        message.setText(contentText);
    }

    void setGraphic(final Node node) {
        message.setGraphic(node);
    }

    void setButtons(final ButtonType... buttons) {
        for (final ButtonType buttonType : buttons) {
            final Button button = new Button(buttonType.getText());
            ButtonBar.setButtonData(button, buttonType.getButtonData());

            button.setOnAction(event -> {
                AlertDialogController.this.buttonType = buttonType;
                ((Stage)((Node)event.getSource()).getScene().getWindow()).close();
            });

            buttonBar.getButtons().add(button);
        }
    }

    Optional<ButtonType> getButtonType() {
        return Optional.ofNullable(buttonType);
    }
}
