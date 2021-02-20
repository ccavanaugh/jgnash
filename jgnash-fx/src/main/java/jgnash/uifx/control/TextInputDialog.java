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
package jgnash.uifx.control;

import java.util.Optional;
import java.util.ResourceBundle;

import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.Options;
import jgnash.uifx.resource.font.MaterialDesignLabel;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;

/**
 * A better behaved TextInputDialog.
 *
 * @author Craig Cavanaugh
 */
public class TextInputDialog {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private ButtonBar buttonBar;

    @FXML
    private Button okButton;

    @FXML
    private Button cancelButton;

    @FXML
    private TextFieldEx textField;

    @FXML
    private Label message;

    private final Stage dialog;

    public TextInputDialog(@NamedArg("defaultValue") final String defaultValue) {
        final ResourceBundle resources = ResourceUtils.getBundle();

        dialog = FXMLUtils.loadFXML(this, "TextInputDialog.fxml", resources);

        JavaFXUtils.runLater(() -> textField.setText(defaultValue));

        setGraphic(new MaterialDesignLabel(MaterialDesignLabel.MDIcon.QUESTION_CIRCLE,
                ThemeManager.getBaseTextHeight() * Alert.HEIGHT_MULTIPLIER));
    }

    @FXML
    private void initialize() {
        buttonBar.buttonOrderProperty().bind(Options.buttonOrderProperty());

        okButton.setOnAction(event -> handleOkayAction());
        cancelButton.setOnAction(event -> handleCancelAction());
        okButton.disableProperty().bind(textField.textProperty().isEmpty());
    }

    public void setTitle(final String title) {
        dialog.setTitle(title);
    }

    public void setContentText(final String contentText) {
        message.setText(contentText);
    }

    private void setGraphic(final Node node) {
        message.setGraphic(node);
    }

    public Optional<String> showAndWait() {
        dialog.sizeToScene();
        dialog.setResizable(false);
        dialog.showAndWait();
        return Optional.ofNullable(textField.getText());
    }

    private void handleOkayAction() {
        ((Stage)parent.get().getWindow()).close();
    }

    private void handleCancelAction() {
        textField.setText(null);
        ((Stage)parent.get().getWindow()).close();
    }
}
