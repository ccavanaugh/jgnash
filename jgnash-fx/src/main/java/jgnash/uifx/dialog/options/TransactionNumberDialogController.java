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
package jgnash.uifx.dialog.options;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.stage.Stage;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;
import jgnash.util.ResourceUtils;

/**
 * Controller for editing the list of available transaction numbers
 *
 * @author Craig Cavanaugh
 */
public class TransactionNumberDialogController {

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private ListView<String> listView;

    private Optional<List<String>> returnValue = Optional.empty();

    @FXML
    private void initialize() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        listView.setEditable(true);
        listView.setCellFactory(TextFieldListCell.forListView());

        listView.getItems().addAll(engine.getTransactionNumberList());
        listView.getItems().add("");    // and an empty string at the end

        listView.setOnEditCommit(event -> {
            listView.getItems().set(event.getIndex(), event.getNewValue());
            processListItems();
        });
    }

    /**
     * Remove all empty items from the list if they are empty except for the last list item.
     * If the last list item is not empty, create a new empty item
     */
    private void processListItems() {
        final List<String> items = listView.getItems();

        items.removeIf(item -> item.trim().isEmpty() && items.indexOf(item) < items.size() - 1);

        if (!items.get(items.size() - 1).isEmpty()) {
            listView.getItems().add("");    // and an empty string at the end
        }
    }

    @FXML
    private void handleUpAction() {
        if (listView.getSelectionModel().getSelectedIndex() >= 1) {
            final int index = listView.getSelectionModel().getSelectedIndex();
            listView.getItems().add(index - 1, listView.getItems().remove(index));
        }
    }

    @FXML
    private void handleDownAction() {
        if (listView.getSelectionModel().getSelectedIndex() < listView.getItems().size() - 1) {
            final int index = listView.getSelectionModel().getSelectedIndex();
            listView.getItems().add(index + 1, listView.getItems().remove(index));
        }
    }

    @FXML
    private void handleOkayAction() {
        final List<String> returnedItems = new ArrayList<>(listView.getItems());

        returnedItems.removeIf(String::isEmpty);    // remove all empty strings

        returnValue = Optional.of(returnedItems);

        ((Stage) parentProperty.get().getWindow()).close();
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parentProperty.get().getWindow()).close();
    }

    public static void showAndWait() {
        final ObjectProperty<TransactionNumberDialogController> controllerObjectProperty = new SimpleObjectProperty<>();

        final URL fxmlUrl = TransactionNumberDialogController.class.getResource("TransactionNumberDialog.fxml");
        final Stage stage = FXMLUtils.loadFXML(fxmlUrl, controllerObjectProperty, ResourceUtils.getBundle());
        stage.setTitle(ResourceUtils.getString("Title.DefTranNum"));

        Platform.runLater(() -> {
            stage.setMinHeight(stage.getHeight());
            stage.setMinWidth(stage.getWidth());
        });

        stage.showAndWait();

        final Optional<List<String>> items = controllerObjectProperty.get().returnValue;

        if (items.isPresent()) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            engine.setTransactionNumberList(items.get());
        }
    }
}
