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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.uifx.dialog.currency;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import jgnash.engine.CurrencyNode;
import jgnash.engine.DefaultCurrencies;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.control.LockedCommodityListCell;
import jgnash.uifx.util.InjectFXML;
import jgnash.util.LockedCommodityNode;

/**
 * Add / remove currency controller.
 *
 * @author Craig Cavanaugh
 */
public class AddRemoveCurrencyController {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private Button addButton;

    @FXML
    private TextField newCurrencyTextField;

    @FXML
    private ListView<CurrencyNode> availableList;

    @FXML
    private ListView<LockedCommodityNode<CurrencyNode>> selectedList;

    @FXML
    private ResourceBundle resources;

    private final BooleanProperty validProperty = new SimpleBooleanProperty(true);

    @FXML   
    void initialize() {
        availableList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        selectedList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        selectedList.setCellFactory(param -> new LockedCommodityListCell<>());


        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        newCurrencyTextField.textProperty().addListener((observable, oldValue, newValue)
                -> validProperty.set(engine.getCurrency(newCurrencyTextField.getText()) == null));

        addButton.disableProperty().bind(newCurrencyTextField.textProperty().isEmpty().or(validProperty.not()));

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

        final ArrayList<LockedCommodityNode<CurrencyNode>> list = new ArrayList<>();

        for (final CurrencyNode node : availNodes) {
            if (activeNodes.contains(node)) {
                list.add(new LockedCommodityNode<>(node, true));
            } else {
                list.add(new LockedCommodityNode<>(node, false));
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

            availableList.getItems().remove(currencyNode);
            selectedList.getItems().add(new LockedCommodityNode<>(currencyNode, false));
        }

        FXCollections.sort(selectedList.getItems());
    }

    @FXML
    private void handleRemoveAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);


        selectedList.getSelectionModel().getSelectedItems().stream()
                .filter(node -> node != null && !node.isLocked()).forEach(node -> {
            selectedList.getItems().remove(node);
            availableList.getItems().add(node.getNode());

            engine.removeCommodity(node.getNode());
        });

        FXCollections.sort(availableList.getItems());
    }


    @FXML
    private void handleNewCurrencyAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final CurrencyNode node = DefaultCurrencies.buildCustomNode(newCurrencyTextField.getText());

        // the add could fail if the commodity symbol is a duplicate
        if (engine.addCurrency(node)) {
            selectedList.getItems().add(new LockedCommodityNode<>(node, false));
            FXCollections.sort(selectedList.getItems());
            newCurrencyTextField.clear();
        }
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parent.get().getWindow()).close();
    }
}
