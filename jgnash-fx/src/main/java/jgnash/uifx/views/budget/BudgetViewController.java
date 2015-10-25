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
package jgnash.uifx.views.budget;

import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.budget.Budget;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.uifx.util.FXMLUtils;

/**
 * @author Craig Cavanaugh
 */
public class BudgetViewController implements MessageListener {

    @FXML
    private BorderPane borderPane;

    @FXML
    private Button exportButton;

    @FXML
    private Button propertiesButton;

    @FXML
    private ComboBox<Budget> availableBudgetsComboBox;

    @FXML
    private ResourceBundle resources;

    @FXML
    private void initialize() {
        exportButton.disableProperty().bind(availableBudgetsComboBox.valueProperty().isNull());
        propertiesButton.disableProperty().bind(availableBudgetsComboBox.valueProperty().isNull());

        // push to end of application thread to avoid a race
        Platform.runLater(() -> {
            final BudgetTableController budgetTableController
                    = FXMLUtils.loadFXML(o -> borderPane.setCenter((Node) o), "BudgetTable.fxml", resources);

            availableBudgetsComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                Platform.runLater(() -> budgetTableController.budgetProperty().setValue(newValue));
            });

            loadComboBox();

            MessageBus.getInstance().registerListener(BudgetViewController.this, MessageChannel.BUDGET,
                    MessageChannel.SYSTEM);
        });
    }

    private void loadComboBox() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final Budget activeBudget = availableBudgetsComboBox.getValue();

        // Get List of active budgets
        final List<Budget> budgetList = engine.getBudgetList();

        availableBudgetsComboBox.getItems().setAll(budgetList);

        // if a budget is already active, select it, otherwise select the first available
        if (budgetList.contains(activeBudget)) {
            availableBudgetsComboBox.setValue(activeBudget);
        } else if (availableBudgetsComboBox.getItems().size() > 0) {
            availableBudgetsComboBox.setValue(availableBudgetsComboBox.getItems().get(0));
        }
    }

    @Override
    public void messagePosted(final Message message) {
        switch (message.getEvent()) {
            case FILE_CLOSING:
                MessageBus.getInstance().unregisterListener(this, MessageChannel.BUDGET, MessageChannel.SYSTEM);
                Platform.runLater(() -> availableBudgetsComboBox.getItems().clear());
                break;
            case BUDGET_REMOVE:
            case BUDGET_ADD:
            case BUDGET_UPDATE:
                Platform.runLater(BudgetViewController.this::loadComboBox);
                break;
            default:
                break;
        }
    }

    @FXML
    private void handleExportAction() {
        // TODO Implement
    }

    @FXML
    private void handleManagerAction() {
        // TODO Implement
    }

    @FXML
    private void handlePropertiesAction() {
        // TODO Implement
    }
}
