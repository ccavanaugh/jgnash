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
package jgnash.uifx.dialog.security;

import java.util.List;
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

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityNode;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.CurrencyComboBox;
import jgnash.uifx.control.IntegerTextField;
import jgnash.uifx.control.QuoteSourceComboBox;
import jgnash.uifx.util.InjectFXML;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Controller for creating and modifying securities.
 *
 * @author Craig Cavanaugh
 */
public class CreateModifySecuritiesController implements MessageListener {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private Button applyButton;

    @FXML
    private Button deleteButton;

    @FXML
    private ListView<SecurityNode> listView;

    @FXML
    private TextField symbolTextField;

    @FXML
    private TextField cusipTextField;

    @FXML
    private QuoteSourceComboBox quoteSourceComboBox;

    @FXML
    private TextField descriptionTextField;

    @FXML
    private IntegerTextField scaleTextField;

    @FXML
    private CurrencyComboBox reportedCurrencyComboBox;

    @FXML
    private ResourceBundle resources;

    private final SimpleObjectProperty<SecurityNode> selectedSecurityNode = new SimpleObjectProperty<>();

    @FXML
    void initialize() {
        selectedSecurityNode.bind(listView.getSelectionModel().selectedItemProperty());

        deleteButton.disableProperty().bind(Bindings.isNull(selectedSecurityNode));

        listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> loadForm());

        MessageBus.getInstance().registerListener(this, MessageChannel.COMMODITY);

        // disable button if the symbol, scale, or reported currency are not specified.
        applyButton.disableProperty()
                .bind(symbolTextField.textProperty().isEmpty()
                        .or(scaleTextField.textProperty().isEmpty())
                              .or(reportedCurrencyComboBox.valueProperty().isNull()));

        new Thread(this::loadList).start();
    }

    private SecurityNode buildSecurityNode() {
        final SecurityNode node = new SecurityNode(reportedCurrencyComboBox.getValue());

        node.setDescription(descriptionTextField.getText());
        node.setScale(scaleTextField.getInteger().byteValue());
        node.setSymbol(symbolTextField.getText().trim());
        node.setISIN(cusipTextField.getText());
        node.setQuoteSource(quoteSourceComboBox.getValue());

        return node;
    }

    private void loadForm() {
        if (selectedSecurityNode.get() != null) {
            final SecurityNode node = selectedSecurityNode.get();
            symbolTextField.setText(node.getSymbol());
            cusipTextField.setText(node.getISIN().trim());
            descriptionTextField.setText(node.getDescription());
            scaleTextField.setInteger((int) node.getScale());
            reportedCurrencyComboBox.setValue(node.getReportedCurrencyNode());
            quoteSourceComboBox.setValue(node.getQuoteSource());
        }
    }

    private void clearForm() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        listView.getSelectionModel().clearSelection();
        symbolTextField.setText("");
        cusipTextField.setText("");
        descriptionTextField.setText("");
        quoteSourceComboBox.setValue(QuoteSource.NONE);
        reportedCurrencyComboBox.setValue(engine.getDefaultCurrency());

        if (reportedCurrencyComboBox.getValue() != null) {  // null value is a miss configured file
            scaleTextField.setInteger((int) reportedCurrencyComboBox.getValue().getScale());
        }
    }

    private void loadList() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final List<SecurityNode> securityNodeList = engine.getSecurities();

        JavaFXUtils.runLater(() -> {
            listView.getItems().setAll(securityNodeList);
            FXCollections.sort(listView.getItems());
            clearForm();
        });
    }

    @FXML
    private void handleNewAction() {
        clearForm();
    }

    @FXML
    private void handleDeleteAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        if (!engine.removeSecurity(selectedSecurityNode.get())) {
            StaticUIMethods.displayWarning(resources.getString("Message.Warn.CommodityInUse"));
        }

        clearForm();
    }

    @FXML
    private void handleCancelAction() {
        clearForm();
    }

    @FXML
    private void handleApplyAction() {

        // always ensure a positive scale is entered
        if (scaleTextField.getInteger() <= 0) {
            scaleTextField.setInteger((int)reportedCurrencyComboBox.getValue().getScale());
        }

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final SecurityNode newNode = buildSecurityNode();

        if (selectedSecurityNode.get() != null) {
            if (!engine.updateCommodity(selectedSecurityNode.get(), newNode)) {
                StaticUIMethods.displayError(ResourceUtils.getString("Message.Error.SecurityUpdate",
                        newNode.getSymbol()));
            }
        } else {
            if (!engine.addSecurity(newNode)) {
                StaticUIMethods.displayError(ResourceUtils.getString("Message.Error.SecurityAdd", newNode.getSymbol()));
            }
        }
        clearForm();
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parent.get().getWindow()).close();
    }

    @Override
    public void messagePosted(Message message) {
        if (message.getObject(MessageProperty.COMMODITY) instanceof SecurityNode) {
            switch (message.getEvent()) {
                case SECURITY_ADD:
                case SECURITY_MODIFY:
                case SECURITY_REMOVE:
                    loadList();
                    break;
                default:
            }
        }
    }
}
