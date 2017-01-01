/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.stage.Stage;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.Options;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;
import jgnash.util.ResourceUtils;

/**
 * Controller for editing the list of available transaction numbers.
 *
 * @author Craig Cavanaugh
 */
public class TransactionNumberDialogController {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private ButtonBar buttonBar;

    @FXML
    private ListView<String> listView;

    private List<String> returnValue = null;

    @FXML
    private void initialize() {
        buttonBar.buttonOrderProperty().bind(Options.buttonOrderProperty());

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

        returnValue = returnedItems;

        ((Stage) parent.get().getWindow()).close();
    }

    private Optional<List<String>> getItems() {
        return Optional.ofNullable(returnValue);
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parent.get().getWindow()).close();
    }

    public static void showAndWait() {
        final FXMLUtils.Pair<TransactionNumberDialogController> pair
                = FXMLUtils.load(TransactionNumberDialogController.class.getResource("TransactionNumberDialog.fxml"),
                ResourceUtils.getString("Title.DefTranNum"));

        pair.getStage().showAndWait();

        final Optional<List<String>> items = pair.getController().getItems();

        if (items.isPresent()) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            engine.setTransactionNumberList(items.get());
        }
    }
}
