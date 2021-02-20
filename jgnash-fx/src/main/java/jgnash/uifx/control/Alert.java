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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;

import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.resource.font.MaterialDesignLabel;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;
import jgnash.util.NotNull;

/**
 * A Better behaved Alert class.
 *
 * @author Craig Cavanaugh
 */
public class Alert {

    static final int HEIGHT_MULTIPLIER = 3;

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private Label message;

    @FXML
    private ButtonBar buttonBar;

    private ButtonType buttonType = ButtonType.CANCEL;  // default is cancelled

    public enum AlertType {
        ERROR,
        WARNING,
        INFORMATION,
        YES_NO
    }

    private final Stage dialog;

    public Alert(@NotNull final AlertType alertType, final String contentText) {
        final ResourceBundle resources = ResourceUtils.getBundle();

        dialog = FXMLUtils.loadFXML(this, "AlertDialog.fxml", resources);

        setContentText(contentText);

        switch (alertType) {
            case ERROR:
                setGraphic(new MaterialDesignLabel(MaterialDesignLabel.MDIcon.EXCLAMATION_TRIANGLE,
                        ThemeManager.getBaseTextHeight() * HEIGHT_MULTIPLIER, Color.DARKRED));
                setButtons(new ButtonType(resources.getString("Button.Close"), ButtonBar.ButtonData.CANCEL_CLOSE));
                break;
            case WARNING:
                setGraphic(new MaterialDesignLabel(MaterialDesignLabel.MDIcon.EXCLAMATION_CIRCLE,
                        ThemeManager.getBaseTextHeight() * HEIGHT_MULTIPLIER, Color.DARKGOLDENROD));
                setButtons(new ButtonType(resources.getString("Button.Close"), ButtonBar.ButtonData.CANCEL_CLOSE));
                break;
            case INFORMATION:
                setGraphic(new MaterialDesignLabel(MaterialDesignLabel.MDIcon.INFO_CIRCLE,
                        ThemeManager.getBaseTextHeight() * HEIGHT_MULTIPLIER, Color.DARKGOLDENROD));
                setButtons(new ButtonType(resources.getString("Button.Close"), ButtonBar.ButtonData.CANCEL_CLOSE));
                break;
            case YES_NO:
                setGraphic(new MaterialDesignLabel(MaterialDesignLabel.MDIcon.QUESTION_CIRCLE,
                        ThemeManager.getBaseTextHeight() * HEIGHT_MULTIPLIER));
                ButtonType buttonTypeYes = new ButtonType(resources.getString("Button.Yes"), ButtonBar.ButtonData.YES);
                ButtonType buttonTypeNo = new ButtonType(resources.getString("Button.No"), ButtonBar.ButtonData.NO);
                setButtons(buttonTypeYes, buttonTypeNo);
                break;
            default:
        }
    }

    public void setTitle(final String title) {
        dialog.setTitle(title);
    }

    public void initOwner(final Window window) {
        dialog.initOwner(window);
    }

    private void setContentText(final String contentText) {
        message.setText(contentText);
    }

    private void setGraphic(final Node node) {
        message.setGraphic(node);
    }

    private void setButtons(final ButtonType... buttons) {
        for (final ButtonType buttonType : buttons) {
            final Button button = new Button(buttonType.getText());
            ButtonBar.setButtonData(button, buttonType.getButtonData());

            button.setOnAction(event -> {
                Alert.this.buttonType = buttonType;
                ((Stage) parent.get().getWindow()).close();
            });

            buttonBar.getButtons().add(button);
        }
    }

    private Optional<ButtonType> getButtonType() {
        return Optional.ofNullable(buttonType);
    }

    public Optional<ButtonType> showAndWait() {
        dialog.sizeToScene();
        dialog.setResizable(false);
        dialog.showAndWait();

        return getButtonType();
    }
}
