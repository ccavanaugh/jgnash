/*
 * jGnash, account personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.uifx.dialog.currency;

import java.util.*;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import jgnash.engine.CurrencyNode;
import jgnash.engine.DefaultCurrencies;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.skin.StyleClass;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.ValidationFactory;
import jgnash.util.NotNull;

/**
 * @author Craig Cavanaugh
 */
public class AddRemoveCurrencyController {

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private Button addButton;

    @FXML
    private TextField newCurrencyTextField;

    @FXML
    private ListView<CurrencyNode> availableList;

    @FXML
    private ListView<LockedCurrency> selectedList;

    @FXML
    private ResourceBundle resources;

    @FXML
    void initialize() {
        availableList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        selectedList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        selectedList.setCellFactory(param -> new LockedSecurityListCell());

        addButton.disableProperty().bind(newCurrencyTextField.textProperty().isEmpty());

        loadModel();

    }

    private void loadModel() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        // Defaults
        final Set<CurrencyNode> defaultNodes = DefaultCurrencies.generateCurrencies();

        // Active
        final Set<CurrencyNode> activeNodes = engine.getActiveCurrencies();

        // Available
        final List<CurrencyNode> availNodes = engine.getCurrencies();

        // remove any overlap between the available and the default
        availNodes.forEach(defaultNodes::remove);

        availableList.getItems().addAll(defaultNodes);

        ArrayList<LockedCurrency> list = new ArrayList<>();

        for (final CurrencyNode node : availNodes) {
            if (activeNodes.contains(node)) {
                list.add(new LockedCurrency(node, true));
            } else {
                list.add(new LockedCurrency(node, false));
            }
        }

        selectedList.getItems().addAll(list);

        FXCollections.sort(availableList.getItems());
        FXCollections.sort(selectedList.getItems());
    }

    @FXML
    private void handleAddAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        for (final CurrencyNode currencyNode : availableList.getSelectionModel().getSelectedItems()) {
            engine.addCurrency(currencyNode);

            availableList.getItems().removeAll(currencyNode);
            selectedList.getItems().addAll(new LockedCurrency(currencyNode, false));
        }

        FXCollections.sort(selectedList.getItems());
    }

    @FXML
    private void handleRemoveAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);


        selectedList.getSelectionModel().getSelectedItems().stream()
                .filter(node -> node != null && !node.isLocked()).forEach(node -> {
            selectedList.getItems().removeAll(node);
            availableList.getItems().addAll(node.getCurrencyNode());

            engine.removeCommodity(node.getCurrencyNode());
        });

        FXCollections.sort(availableList.getItems());
    }


    @FXML
    private void handleNewCurrencyAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        if (engine.getCurrency(newCurrencyTextField.getText()) != null) {
            ValidationFactory.showValidationError(newCurrencyTextField, resources.getString("Message.Error.Duplicate"));
        } else {
            final CurrencyNode node = DefaultCurrencies.buildCustomNode(newCurrencyTextField.getText());

            // the add could fail if the commodity symbol is a duplicate
            if (engine.addCurrency(node)) {

                selectedList.getItems().addAll(new LockedCurrency(node, false));
                FXCollections.sort(selectedList.getItems());
                newCurrencyTextField.clear();
            }
        }
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parentProperty.get().getWindow()).close();
    }

    private static class LockedCurrency implements Comparable<LockedCurrency> {
        private final boolean locked;
        private final CurrencyNode currencyNode;

        LockedCurrency(@NotNull final CurrencyNode currencyNode, final boolean locked) {
            this.currencyNode = currencyNode;
            this.locked = locked;
        }

        @Override
        public String toString() {
            return currencyNode.toString();
        }

        public boolean isLocked() {
            return locked;
        }

        @Override
        public int compareTo(@NotNull final LockedCurrency other) {
            return currencyNode.compareTo(other.currencyNode);
        }

        @Override
        public boolean equals(final Object o) {
            return this == o || o instanceof LockedCurrency && currencyNode.equals(((LockedCurrency) o).currencyNode);
        }

        CurrencyNode getCurrencyNode() {
            return currencyNode;
        }

        @Override
        public int hashCode() {
            int hash = currencyNode.hashCode();
            return 13 * hash + (locked ? 1 : 0);
        }
    }

    /**
     * Provides visual feedback that items are locked and may not be moved
     */
    private static class LockedSecurityListCell extends ListCell<LockedCurrency> {

        @Override
        public void updateItem(final LockedCurrency item, final boolean empty) {
            super.updateItem(item, empty);  // required

            if (!empty) {
                if (item.isLocked()) {
                    setId(StyleClass.DISABLED_CELL_ID);
                    setDisable(true);
                } else {
                    setId(StyleClass.ENABLED_CELL_ID);
                    setDisable(false);
                }

                setText(item.toString());
            } else {
                setText("");
            }
        }
    }
}
