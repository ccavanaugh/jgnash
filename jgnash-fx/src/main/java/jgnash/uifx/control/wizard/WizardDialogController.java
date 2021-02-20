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
package jgnash.uifx.control.wizard;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import jgnash.uifx.skin.StyleClass;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Controller for the wizard dialog.
 *
 * @author Craig Cavanaugh
 */
public class WizardDialogController<K extends Enum<?>> {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private TitledPane taskTitlePane;

    @FXML
    protected ResourceBundle resources;

    @FXML
    private ListView<WizardDescriptor> taskList;

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

    private final BooleanProperty valid = new SimpleBooleanProperty(false);

    private final IntegerProperty selectedIndex = new SimpleIntegerProperty();

    private final Map<WizardPaneController<K>, Pane> paneMap = new HashMap<>();

    private final DoubleProperty taskListWidth = new SimpleDoubleProperty();

    @FXML
    private void initialize() {
        cancelButton.setCancelButton(true);
        nextButton.setDefaultButton(true);

        taskList.setEditable(false);

        // updates the task title
        taskList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue)
                -> handleTaskChange(oldValue, newValue));

        taskList.setCellFactory(param -> new ControllerListCell());

        // bind the min and max width to a value we control.  ListView does a terrible job of controlling it's width
        taskList.minWidthProperty().bind(taskListWidth);
        taskList.maxWidthProperty().bind(taskListWidth);

        selectedIndex.bind(taskList.getSelectionModel().selectedIndexProperty());
    }

    public ReadOnlyBooleanProperty validProperty() {
        return new ReadOnlyBooleanWrapper(valid.get());
    }

    public void addTaskPane(final WizardPaneController<K> wizardPaneController, final Pane pane) {
        Objects.requireNonNull(wizardPaneController);
        Objects.requireNonNull(pane);

        wizardPaneController.getSettings(settings);
        wizardPaneController.putSettings(settings);

        // Listen for changes to the controller descriptor
        wizardPaneController.descriptorProperty().addListener(observable -> taskList.refresh());

        taskList.getItems().add(wizardPaneController.descriptorProperty().get());
        paneMap.put(wizardPaneController, pane);

        // force selection if this is the first pane
        if (taskList.getItems().size() == 1) {
            taskList.getSelectionModel().select(0);

            taskPane.minWidthProperty().bind(pane.minWidthProperty());
        }

        // update the preferred task list width
        taskListWidth.set(Math.max(getControllerDescriptionWidth(wizardPaneController), taskListWidth.get()));

        updateButtonState();
    }

    public Object getSetting(final K key) {
        return settings.get(key);
    }

    public void setSetting(final K key, final Object value) {
        settings.put(key, value);

        /* New setting. Tell each page to read */
        for (WizardPaneController<K> wizardPaneController : paneMap.keySet()) {
            wizardPaneController.getSettings(settings);
        }

        updateButtonState();
    }

    // handle a task change via the list
    private void handleTaskChange(final WizardDescriptor oldDescriptor, final WizardDescriptor newDescriptor) {

        // store the settings of the prior controller... the user may be skipping around
        if (oldDescriptor != null) {
            getController(oldDescriptor).putSettings(settings);
        }

        getController(newDescriptor).getSettings(settings);

        taskTitlePane.textProperty().set(newDescriptor.getDescription());
        updateButtonState();

        paneMap.keySet().stream().filter(controller -> controller.descriptorProperty().get().equals(newDescriptor))
                .forEach(controller -> {
            taskPane.getChildren().clear();
            taskPane.getChildren().addAll(paneMap.get(controller));
        });
    }

    private WizardPaneController<K> getController(final WizardDescriptor descriptor) {
        for (final WizardPaneController<K> wizardPaneController : paneMap.keySet()) {
            if (wizardPaneController.descriptorProperty().get().equals(descriptor)) {
                return wizardPaneController;
            }
        }

        throw new IllegalArgumentException("Did not find a controller match for the descriptor");
    }

    private void updateButtonState() {
        nextButton.setDisable(selectedIndex.get() < 0 || selectedIndex.get() >= taskList.getItems().size() - 1);

        if (selectedIndex.get() == taskList.getItems().size() - 1) {
            boolean isValid = true;

            for (WizardPaneController<K> wizardPaneController : paneMap.keySet()) {
                if (!wizardPaneController.isPaneValid()) {
                    isValid = false;
                }
            }

            finishButton.setDisable(!isValid);
        } else {
            finishButton.setDisable(true);
        }

        backButton.setDisable(selectedIndex.get() == 0);
    }

    @FXML
    private void handleCancelAction() {
        valid.set(false);
        ((Stage) parent.get().getWindow()).close();
    }

    @FXML
    private void handleNextAction() {
        if (selectedIndex.get() < taskList.getItems().size() - 1) {
            // store an setting on the active page. May be necessary for the next page
            getController(taskList.getSelectionModel().getSelectedItem()).putSettings(settings);

            // select the next page
            taskList.getSelectionModel().select(selectedIndex.get() + 1);

            JavaFXUtils.runLater(() -> {
                // tell the active page to update
                getController(taskList.getSelectionModel().getSelectedItem()).getSettings(settings);
            });
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
        valid.set(true);

        for (WizardPaneController<K> wizardPaneController : paneMap.keySet()) {
            if (!wizardPaneController.isPaneValid()) {
                valid.set(false);
                break;
            }
            wizardPaneController.putSettings(settings);
        }

        if (valid.get()) {
            ((Stage) parent.get().getWindow()).close();
        }
    }

    /**
     * Determines the preferred width of the {@code ListCell} for a better visual width of the task list.
     *
     * @param item controller we need the optimal width off
     * @return preferred width
     */
    private double getControllerDescriptionWidth(final WizardPaneController<K> item) {
        final ControllerListCell cell = new ControllerListCell();
        cell.updateItem(item.descriptorProperty().get(), false);

        return cell.prefWidth(-1);
    }

    /**
     * Custom list cell.  Marks any bad pages with a font change
     */
    private class ControllerListCell extends ListCell<WizardDescriptor> {

        ControllerListCell() {
            super();
            updateListView(taskList);
            setSkin(createDefaultSkin());
        }

        @Override
        protected void updateItem(final WizardDescriptor item, final boolean empty) {
            super.updateItem(item, empty);
            if (!empty) {
                setText(item.getDescription());

                if (!item.isValid()) {
                    setId(StyleClass.NORMAL_NEGATIVE_CELL_ID);
                } else {
                    setId(StyleClass.NORMAL_CELL_ID);
                }
            }
        }
    }
}
