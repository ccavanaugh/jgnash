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

import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import jgnash.engine.CommodityNode;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.IntegerTextField;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Controller of modifying currencies.
 *
 * @author Craig Cavanaugh
 */
public class ModifyCurrencyController implements MessageListener {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private Button applyButton;

    @FXML
    private ListView<CurrencyNode> listView;

    @FXML
    private TextField symbolTextField;

    @FXML
    private TextField descriptionTextField;

    @FXML
    private IntegerTextField scaleTextField;

    @FXML
    private TextField prefixTextField;

    @FXML
    private TextField suffixTextField;

    @FXML
    private ResourceBundle resources;

    private final SimpleObjectProperty<CurrencyNode> selectedCurrency = new SimpleObjectProperty<>();

    @FXML
    void initialize() {
        MessageBus.getInstance().registerListener(this, MessageChannel.COMMODITY);

        selectedCurrency.bind(listView.getSelectionModel().selectedItemProperty());

        selectedCurrency.addListener((observable, oldValue, newValue) -> JavaFXUtils.runLater(this::loadForm));

        // unregister when the window closes
        parent.addListener((observable, oldValue, newValue)
                -> newValue.windowProperty().addListener((observable1, oldValue1, newValue1) ->
                newValue1.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST,
                        event -> MessageBus.getInstance().unregisterListener(ModifyCurrencyController.this,
                                MessageChannel.COMMODITY))));

        applyButton.disableProperty().bind(Bindings.or(selectedCurrency.isNull(),
                scaleTextField.textProperty().isEmpty()));

        symbolTextField.disableProperty().bind(selectedCurrency.isNotNull());

        loadModel();
    }

    private void loadModel() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        listView.getItems().setAll(engine.getCurrencies());

        FXCollections.sort(listView.getItems());
    }

    private void loadForm() {
        if (selectedCurrency.get() != null) {
            symbolTextField.setText(selectedCurrency.get().getSymbol());
            descriptionTextField.setText(selectedCurrency.get().getDescription());
            scaleTextField.setInteger((int) selectedCurrency.get().getScale());
            prefixTextField.setText(selectedCurrency.get().getPrefix());
            suffixTextField.setText(selectedCurrency.get().getSuffix());
        } else {
            clearForm();
        }
    }

    private void clearForm() {
        symbolTextField.clear();
        descriptionTextField.clear();
        scaleTextField.clear();
        prefixTextField.clear();
        suffixTextField.clear();

        listView.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleClearAction() {
        clearForm();
    }

    @FXML
    private void handleApplyAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final CurrencyNode newNode = new CurrencyNode();

        newNode.setSymbol(symbolTextField.getText());
        newNode.setDescription(descriptionTextField.getText());
        newNode.setPrefix(prefixTextField.getText());
        newNode.setScale(scaleTextField.getInteger().byteValue());
        newNode.setSuffix(suffixTextField.getText());

        if (!engine.updateCommodity(selectedCurrency.get(), newNode)) {
            StaticUIMethods.displayError(ResourceUtils.getString("Message.Error.CurrencyUpdate", newNode.getSymbol()));
        }

        loadModel();
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parent.get().getWindow()).close();
    }

    @Override
    public void messagePosted(final Message message) {
        final CommodityNode node = message.getObject(MessageProperty.COMMODITY);

        if (node instanceof CurrencyNode) {
            switch (message.getEvent()) {
                case CURRENCY_REMOVE:
                    listView.getItems().remove(node);
                    if (node.equals(selectedCurrency.get())) {
                        handleClearAction();
                    }
                    break;
                case CURRENCY_REMOVE_FAILED:
                    StaticUIMethods.displayError(resources.getString("Message.Warn.CurrencyInUse"));
                    break;
                case CURRENCY_ADD:
                case CONFIG_MODIFY:
                    handleClearAction();
                    loadModel();
                    break;
                case CURRENCY_ADD_FAILED:
                    StaticUIMethods.displayError(resources.getString("Message.Error.AddCurrency"));
                    break;
                case CURRENCY_MODIFY_FAILED:
                    StaticUIMethods.displayError(resources.getString("Message.Error.ModifyCurrency"));
                    break;
                default:
            }
        }
    }
}
