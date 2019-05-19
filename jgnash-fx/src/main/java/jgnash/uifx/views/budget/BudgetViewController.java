/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetResultsExport;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.views.main.MainView;

/**
 * Primary controller for the Budget view.
 *
 * @author Craig Cavanaugh
 */
public class BudgetViewController implements MessageListener {

    private static final String EXPORT_DIR = "exportDir";

    private static final String LAST_BUDGET = "lastBudget";

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

    private BudgetTableController budgetTableController;

    private final Preferences preferences = Preferences.userNodeForPackage(BudgetViewController.class);

    @FXML
    private void initialize() {
        exportButton.disableProperty().bind(availableBudgetsComboBox.valueProperty().isNull());
        propertiesButton.disableProperty().bind(availableBudgetsComboBox.valueProperty().isNull());

        // push to end of application thread to avoid a race
        Platform.runLater(() -> {
            budgetTableController
                    = FXMLUtils.loadFXML(o -> borderPane.setCenter(o), "BudgetTable.fxml", resources);

            availableBudgetsComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    preferences.put(LAST_BUDGET, newValue.getUuid().toString());
                }

                Platform.runLater(() -> budgetTableController.budgetProperty().set(newValue));
            });

            loadComboBox();

            MessageBus.getInstance().registerListener(BudgetViewController.this, MessageChannel.BUDGET,
                    MessageChannel.SYSTEM);
        });
    }

    private void loadComboBox() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        // Create a sorted List of active budgets
        final List<Budget> budgetList = engine.getBudgetList();
        Collections.sort(budgetList);

        availableBudgetsComboBox.getItems().setAll(budgetList);

        String uuid = preferences.get(LAST_BUDGET, "");

        if (!uuid.isEmpty()) {
            final Budget lastBudget = engine.getBudgetByUuid(UUID.fromString(uuid));

            if (budgetList.contains(lastBudget)) {
                availableBudgetsComboBox.setValue(lastBudget);
            } else if (availableBudgetsComboBox.getItems().size() > 0) {
                availableBudgetsComboBox.setValue(availableBudgetsComboBox.getItems().get(0));
            }
        }
    }

    @FXML
    private void handleExportAction() {
        Objects.requireNonNull(budgetTableController);

        final Preferences pref = Preferences.userNodeForPackage(BudgetViewController.class);
        final FileChooser fileChooser = new FileChooser();

        final File initialDirectory = new File(pref.get(EXPORT_DIR, System.getProperty("user.home")));

        // Protect against an IllegalArgumentException
        if (initialDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(resources.getString("Label.SpreadsheetFiles") + " (*.xls, *.xlsx)",
                        "*.xls", "*.xlsx")
        );

        final File file = fileChooser.showSaveDialog(MainView.getPrimaryStage());

        if (file != null) {
            pref.put(EXPORT_DIR, file.getParentFile().getAbsolutePath());

            final Task<Void> exportTask = new Task<>() {
                @Override
                protected Void call() {
                    updateMessage(resources.getString("Message.PleaseWait"));
                    updateProgress(-1, Long.MAX_VALUE);

                    BudgetResultsExport.exportBudgetResultsModel(file.toPath(), budgetTableController.getBudgetResultsModel());
                    return null;
                }
            };

            new Thread(exportTask).start();

            StaticUIMethods.displayTaskProgress(exportTask);
        }
    }

    @FXML
    private void handleManagerAction() {
        BudgetManagerDialogController.showBudgetManager();
    }

    @FXML
    private void handlePropertiesAction() {
        final FXMLUtils.Pair<BudgetPropertiesDialogController> pair =
                FXMLUtils.load(BudgetPropertiesDialogController.class.getResource("BudgetPropertiesDialog.fxml"),
                        resources.getString("Title.BudgetProperties"));

        pair.getController().setBudget(availableBudgetsComboBox.getValue());

        pair.getStage().show();
        pair.getStage().setResizable(false);
    }

    @FXML
    private void handleTodayAction() {
        JavaFXUtils.runLater(budgetTableController::focusCurrentPeriod);
    }

    @Override
    public void messagePosted(final Message message) {
        switch (message.getEvent()) {
            case FILE_CLOSING:
                MessageBus.getInstance().unregisterListener(this, MessageChannel.BUDGET, MessageChannel.SYSTEM);
                JavaFXUtils.runLater(() -> availableBudgetsComboBox.getItems().clear());
                break;
            case BUDGET_REMOVE:
            case BUDGET_ADD:
            case BUDGET_UPDATE:
                JavaFXUtils.runLater(BudgetViewController.this::loadComboBox);
                break;
            default:
                break;
        }
    }
}
