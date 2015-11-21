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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;

import java.util.Optional;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import jgnash.resource.font.FontAwesomeLabel;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;
import jgnash.util.ResourceUtils;

/**
 * A better behaved TextInputDialog
 *
 * @author Craig Cavanaugh
 */
public class TextInputDialog {

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private Button okButton;

    @FXML
    private Button cancelButton;

    @FXML
    private TextField textField;

    @FXML
    private Label message;

    private final Stage dialog;

    public TextInputDialog(@NamedArg("defaultValue") final String defaultValue) {
        final ResourceBundle resources = ResourceUtils.getBundle();

        dialog = FXMLUtils.loadFXML(this, "TextInputDialog.fxml", resources);

        Platform.runLater(() -> textField.setText(defaultValue));

        setGraphic(new FontAwesomeLabel(FontAwesomeIcon.QUESTION_CIRCLE,
                ThemeManager.getBaseTextHeight() * Alert.HEIGHT_MULTIPLIER));
    }

    @FXML
    private void initialize() {
        okButton.setOnAction(event -> handleOkayAction());
        cancelButton.setOnAction(event -> handleCancelAction());
        okButton.disableProperty().bind(textField.textProperty().isEmpty());
    }

    public void setTitle(final String title) {
        dialog.setTitle(title);
    }

    /*public void initOwner(final Window window) {
        dialog.initOwner(window);
    }*/

    public void setContentText(final String contentText) {
        message.setText(contentText);
    }

    private void setGraphic(final Node node) {
        message.setGraphic(node);
    }

    public Optional<String> showAndWait() {
        dialog.setResizable(false);
        dialog.showAndWait();
        return Optional.ofNullable(textField.getText());
    }

    private void handleOkayAction() {
        ((Stage)parentProperty.get().getWindow()).close();
    }

    private void handleCancelAction() {
        textField.setText(null);
        ((Stage)parentProperty.get().getWindow()).close();
    }
}
