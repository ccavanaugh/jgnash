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
package jgnash.uifx.views.budget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.stage.Stage;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.budget.Budget;
import jgnash.engine.message.ChannelEvent;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.TextInputDialog;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Controller for budget management.
 *
 * @author Craig Cavanaugh
 */
public class BudgetManagerDialogController implements MessageListener {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private Button duplicateButton;

    @FXML
    private Button renameButton;

    @FXML
    private ListView<Budget> budgetListView;

    @FXML
    private Button deleteButton;

    @FXML
    private ResourceBundle resources;

    @FXML
    private void initialize() {
        deleteButton.disableProperty().bind(budgetListView.getSelectionModel().selectedItemProperty().isNull());
        duplicateButton.disableProperty().bind(budgetListView.getSelectionModel().selectedItemProperty().isNull());
        renameButton.disableProperty().bind(budgetListView.getSelectionModel().selectedItemProperty().isNull());

        budgetListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        JavaFXUtils.runLater(this::loadBudgetListView);

        MessageBus.getInstance().registerListener(this, MessageChannel.BUDGET);
    }

    private void loadBudgetListView() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        // Create a sorted List of active budgets
        final List<Budget> budgetList = engine.getBudgetList();
        Collections.sort(budgetList);

        budgetListView.getItems().setAll(budgetList);
    }

    @FXML
    private void handleCloseAction() {
        MessageBus.getInstance().unregisterListener(this, MessageChannel.BUDGET);
        ((Stage)parent.get().getWindow()).close();
    }

    @FXML
    private void handleNewAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final Budget newBudget = new Budget();

        String name = resources.getString("Word.NewBudget");

        int count = 2;

        while (true) {
            boolean nameIsUnique = true;

            for (final Budget budget : engine.getBudgetList()) {
                if (budget.getName().equals(name)) {
                    name = resources.getString("Word.NewBudget") + " " + count;
                    count++;
                    nameIsUnique = false;
                }
            }

            if (nameIsUnique) {
                break;
            }
        }

        newBudget.setName(name);
        newBudget.setDescription(resources.getString("Word.NewBudget"));

        if (!engine.addBudget(newBudget)) {
            StaticUIMethods.displayError(resources.getString("Message.Error.NewBudget"));
        }
    }

    @FXML
    private void handleNewHistoricalAction() {
        final FXMLUtils.Pair<HistoricalBudgetDialogController> pair =
                FXMLUtils.load(HistoricalBudgetDialogController.class.getResource("HistoricalBudgetDialog.fxml"),
                        resources.getString("Title.NewBudget"));

        pair.getStage().show();
        pair.getStage().setResizable(false);
    }

    @FXML
    private void handleDuplicateAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        for (final Budget budget : budgetListView.getSelectionModel().getSelectedItems()) {
            try {
                final Budget newBudget = (Budget) budget.clone();
                if (!engine.addBudget(newBudget)) {
                    StaticUIMethods.displayError(resources.getString("Message.Error.BudgetDuplicate"));
                }
            } catch (final CloneNotSupportedException e) {
                Logger.getLogger(BudgetManagerDialogController.class.getName()).log(Level.SEVERE, e.toString(), e);
            }
        }
    }

    @FXML
    private void handleRenameAction() {
        for (final Budget budget : budgetListView.getSelectionModel().getSelectedItems()) {

            final TextInputDialog textInputDialog = new TextInputDialog(budget.getName());
            textInputDialog.setTitle(resources.getString("Title.RenameBudget"));
            textInputDialog.setContentText(resources.getString("Label.RenameBudget"));

            final Optional<String> optional = textInputDialog.showAndWait();

            optional.ifPresent(s -> {
                if (!s.isEmpty()) {
                    final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                    Objects.requireNonNull(engine);

                    budget.setName(s);
                    engine.updateBudget(budget);
                }
            });
        }
    }

    @FXML
    private void handleDeleteAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final List<Budget> selected = new ArrayList<>(budgetListView.getSelectionModel().getSelectedItems());

        if (!selected.isEmpty()) {
            final String message = selected.size() == 1 ? resources.getString("Message.ConfirmBudgetDelete") :
                                           resources.getString("Message.ConfirmMultipleBudgetDelete");

            if (StaticUIMethods.showConfirmationDialog(resources.getString("Title.Confirm"), message).getButtonData()
                        == ButtonBar.ButtonData.YES) {
                selected.stream().filter(value -> !engine.removeBudget(value)).forEach(value
                                                                                               -> StaticUIMethods.displayError(resources.getString("Message.Error.BudgetRemove")));
            }
        }
    }

    @Override
    public void messagePosted(final Message message) {
        if (message.getEvent() == ChannelEvent.BUDGET_ADD || message.getEvent() == ChannelEvent.BUDGET_REMOVE
                    || message.getEvent() == ChannelEvent.BUDGET_UPDATE) {
            JavaFXUtils.runLater(this::loadBudgetListView);
        }
    }

    public static void showBudgetManager() {
        final FXMLUtils.Pair<BudgetManagerDialogController> pair =
                FXMLUtils.load(BudgetManagerDialogController.class.getResource("BudgetManagerDialog.fxml"),
                        ResourceUtils.getString("Title.BudgetManager"));

        pair.getStage().show();
        pair.getStage().setResizable(false);
    }
}
