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
package jgnash.uifx.dialog.options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.stage.Stage;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.Options;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;
import jgnash.resource.util.ResourceUtils;

/**
 * Controller for editing the list of available transaction numbers.
 *
 * @author Craig Cavanaugh
 */
public class TransactionNumberDialogController {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private Button upButton;

    @FXML
    private Button downButton;

    @FXML
    private ButtonBar buttonBar;

    @FXML
    private ListView<String> listView;

    private List<String> returnValue = null;

    final private IntegerProperty countProperty = new SimpleIntegerProperty();

    final private IntegerProperty selectedIndexProperty = new SimpleIntegerProperty();

    @FXML
    private void initialize() {

        // simplify binding
        countProperty.bind(Bindings.size(listView.getItems()));
        selectedIndexProperty.bind(listView.selectionModelProperty().get().selectedIndexProperty());

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

        upButton.disableProperty().bind(countProperty.lessThan(1)
                .or(selectedIndexProperty.lessThan(1)));

        downButton.disableProperty().bind(countProperty.lessThan(1)
                .or(selectedIndexProperty.isEqualTo(countProperty.subtract(1)))
                .or(selectedIndexProperty.lessThan(0)));
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
        Collections.swap(listView.getItems(), selectedIndexProperty.get(), selectedIndexProperty.get() - 1);

    }

    @FXML
    private void handleDownAction() {
        Collections.swap(listView.getItems(), selectedIndexProperty.get(), selectedIndexProperty.get() + 1);
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

        final Optional<List<String>> optional = pair.getController().getItems();

        optional.ifPresent(strings -> {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            engine.setTransactionNumberList(strings);
        });
    }
}
