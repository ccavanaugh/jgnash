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

import java.util.Collection;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
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
 * A ChoiceDialog with a consistent application appearance
 *
 * @param <T> The type of the items to show to the user, and the type that is returned
 *            via {@link #showAndWait()} when the dialog is dismissed.
 *
 * @author Craig Cavanaugh
 */
public class ChoiceDialog<T> {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private  ButtonBar buttonBar;

    @FXML
    private  Button okButton;

    @FXML
    private  Button cancelButton;

    @FXML
    private Label message;

    @FXML
    private  ComboBox<T> comboBox;

    private final Stage dialog;

    /**
     * Creates a new ChoiceDialog instance with the first argument specifying the
     * default choice that should be shown to the user, and the second argument
     * specifying a collection of all available choices for the user. It is
     * expected that the defaultChoice be one of the elements in the choices
     * collection. If this is not true, then defaultChoice will be set to null and the
     * dialog will show with the initial choice set to the first item in the list
     * of choices.
     *
     * @param defaultChoice The item to display as the pre-selected choice in the dialog.
     *        This item must be contained within the choices varargs array.
     * @param choices All possible choices to present to the user.
     */
    public ChoiceDialog(final T defaultChoice, final Collection<T> choices) {

        final ResourceBundle resources = ResourceUtils.getBundle();

        dialog = FXMLUtils.loadFXML(this, "ChoiceDialog.fxml", resources);

        setGraphic(new MaterialDesignLabel(MaterialDesignLabel.MDIcon.QUESTION_CIRCLE,
                ThemeManager.getBaseTextHeight() * Alert.HEIGHT_MULTIPLIER));

        // block until the combo box is completed loaded to prevent a race condition
        JavaFXUtils.runAndWait(() -> {
            comboBox.getItems().setAll(choices);
            setSelectedItem(defaultChoice);
        });
    }

    @FXML
    private void initialize() {
        buttonBar.buttonOrderProperty().bind(Options.buttonOrderProperty());

        okButton.setOnAction(event -> handleOkayAction());
        cancelButton.setOnAction(event -> handleCancelAction());
        okButton.disableProperty().bind(comboBox.valueProperty().isNull());
    }

    public void setTitle(final String title) {
        dialog.setTitle(title);
    }

    public void setContentText(final String contentText) {
        message.setText(contentText);
        dialog.sizeToScene();
    }

    private void setGraphic(final Node node) {
        message.setGraphic(node);
    }

    /**
     * Returns the currently selected item in the dialog.
     * @return the currently selected item
     */
    private T getSelectedItem() {
        return comboBox.getSelectionModel().getSelectedItem();
    }

    /**
     * Sets the currently selected item in the dialog.
     * @param item The item to select in the dialog.
     */
    private void setSelectedItem(T item) {
        comboBox.getSelectionModel().select(item);   }

    public Optional<T> showAndWait() {
        dialog.sizeToScene();
        dialog.setResizable(false);
        dialog.showAndWait();
        return Optional.ofNullable(getSelectedItem());
    }

    private void handleOkayAction() {
        ((Stage)parent.get().getWindow()).close();
    }

    private void handleCancelAction() {
        comboBox.setValue(null);
        ((Stage)parent.get().getWindow()).close();
    }
}
