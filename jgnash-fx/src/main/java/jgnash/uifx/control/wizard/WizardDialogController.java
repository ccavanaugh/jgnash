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
package jgnash.uifx.control.wizard;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import jgnash.uifx.util.InjectFXML;

/**
 * Controller for the wizard dialog
 *
 * @author Craig Cavanaugh
 */
public class WizardDialogController<K extends Enum> {

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private TitledPane taskTitlePane;

    @FXML
    protected ResourceBundle resources;

    @FXML
    private ListView<WizardPaneController<K>> taskList;

    @FXML
    private StackPane taskPane;

    @FXML
    private Button backButton;

    @FXML
    private Button nextButton;

    @FXML
    private Button finishButton;

    @FXML
    private Button cancelButton;

    private final Map<K, Object> settings = new HashMap<>();

    private final BooleanProperty validProperty = new SimpleBooleanProperty(false);

    private final IntegerProperty selectedIndex = new SimpleIntegerProperty();

    private final Map<WizardPaneController<K>, Pane> paneMap = new HashMap<>();

    @FXML
    private void initialize() {
        cancelButton.setCancelButton(true);
        nextButton.setDefaultButton(true);

        taskList.setEditable(false);

        // updates the task title
        taskList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            handleTaskChange(newValue);
        });

        selectedIndex.bind(taskList.getSelectionModel().selectedIndexProperty());
    }

    public void addTaskPane(final WizardPaneController<K> wizardPaneController, Pane pane) {
        Objects.requireNonNull(wizardPaneController);
        Objects.requireNonNull(pane);

        taskList.getItems().add(wizardPaneController);
        paneMap.put(wizardPaneController, pane);

        // force selection if this is the first pane
        if (taskList.getItems().size() == 1) {
            taskList.getSelectionModel().select(0);
        }
    }

    public Object getSetting(final K key) {
        return settings.get(key);
    }

    public void setSetting(final K key, final Object value) {
        settings.put(key, value);

        /* New setting. Tell each page to read */
        for (WizardPaneController<K> wizardPaneController : taskList.getItems()) {
            wizardPaneController.getSettings(settings);
        }
    }

    private void handleTaskChange(final WizardPaneController<K> wizardPaneController) {
        taskTitlePane.textProperty().setValue(wizardPaneController.toString());
        updateButtonState();

        taskPane.getChildren().clear();
        taskPane.getChildren().addAll(paneMap.get(wizardPaneController));
    }

    private void updateButtonState() {
        if (selectedIndex.get() >= 0 && selectedIndex.get() < taskList.getItems().size() - 1) {
            nextButton.setDisable(false);
        } else {
            nextButton.setDisable(true);
        }

        if (selectedIndex.get() == taskList.getItems().size() - 1) {
            boolean _valid = true;

            for (WizardPaneController<K> wizardPaneController : taskList.getItems()) {
                if (!wizardPaneController.isPaneValid()) {
                    _valid = false;
                }
            }

            finishButton.setDisable(!_valid);
        } else {
            finishButton.setDisable(true);
        }

        if (selectedIndex.get() == 0) {
            backButton.setDisable(true);
        } else {
            backButton.setDisable(false);
        }
    }

    @FXML
    private void handleCancelAction() {
        validProperty.setValue(false);
        ((Stage) parentProperty.get().getWindow()).close();
    }

    @FXML
    private void handleNextAction() {
        if (selectedIndex.get() < taskList.getItems().size() - 1) {
            // store an setting on the active page. May be necessary for the next page
            taskList.getSelectionModel().getSelectedItem().putSettings(settings);

            // select the next page
            taskList.getSelectionModel().select(selectedIndex.get() + 1);

            // tell the active page to update
            taskList.getSelectionModel().getSelectedItem().getSettings(settings);
        }
    }

    @FXML
    private void handleBackAction() {
        if (selectedIndex.get() > 0) {

            // select the previous page
            taskList.getSelectionModel().select(selectedIndex.get() - 1);
        }
    }

    @FXML
    private void handleFinishAction() {
        validProperty.setValue(true);

        for (WizardPaneController<K> wizardPaneController : taskList.getItems()) {
            if (!wizardPaneController.isPaneValid()) {
                validProperty.setValue(false);
                break;
            }
            wizardPaneController.putSettings(settings);
        }

        if (validProperty.get()) {
            ((Stage) parentProperty.get().getWindow()).close();
        }
    }
}
