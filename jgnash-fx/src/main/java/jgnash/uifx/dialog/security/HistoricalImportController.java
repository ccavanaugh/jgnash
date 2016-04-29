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
package jgnash.uifx.dialog.security;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.net.security.UpdateFactory;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.util.InjectFXML;
import jgnash.time.DateUtils;
import jgnash.util.ResourceUtils;

import org.controlsfx.control.CheckListView;

/**
 * Historical import controller
 *
 * @author Craig Cavanaugh
 */
public class HistoricalImportController {

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private Button stopButton;

    @FXML
    private Button selectAllButton;

    @FXML
    private Button clearAllButton;

    @FXML
    private Button invertAllButton;

    @FXML
    private Label messageLabel;

    @FXML
    private Button startButton;

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    private CheckListView<SecurityNode> checkListView;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private ResourceBundle resources;

    private Task<Void> updateTask = null;

    private volatile boolean requestCancel = false;

    private final BooleanProperty disableUIProperty = new SimpleBooleanProperty();

    @FXML
    void initialize() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final List<SecurityNode> securityNodes = engine.getSecurities().stream()
                .filter(securityNode -> securityNode.getQuoteSource() != QuoteSource.NONE).collect(Collectors.toList());

        Collections.sort(securityNodes);

        checkListView.getItems().addAll(securityNodes);

        startDatePicker.setValue(LocalDate.now().minusMonths(1));

        checkListView.disableProperty().bind(disableUIProperty);
        endDatePicker.disableProperty().bind(disableUIProperty);
        startDatePicker.disableProperty().bind(disableUIProperty);
        startButton.disableProperty().bind(disableUIProperty);
        selectAllButton.disableProperty().bind(disableUIProperty);
        clearAllButton.disableProperty().bind(disableUIProperty);
        invertAllButton.disableProperty().bind(disableUIProperty);

        stopButton.disableProperty().bind(disableUIProperty.not());
    }

    @FXML
    private void handleSelectAllAction() {
        checkListView.getCheckModel().checkAll();
    }

    @FXML
    private void handleClearAllAction() {
        checkListView.getCheckModel().clearChecks();
    }

    @FXML
    private void handleInvertSelectionAction() {
        for (int i = 0; i < checkListView.getCheckModel().getItemCount(); i++) {
            if (checkListView.getCheckModel().isChecked(i)) {
                checkListView.getCheckModel().clearCheck(i);
            } else {
                checkListView.getCheckModel().check(i);
            }
        }
    }

    @FXML
    private void handleStartAction() {
        disableUIProperty.setValue(true);

        final DateTimeFormatter dateTimeFormatter = DateUtils.getShortDateTimeFormat();

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        updateTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                final LocalDate startDate = startDatePicker.getValue();
                final LocalDate endDate = endDatePicker.getValue();

                final Map<SecurityNode, List<SecurityHistoryNode>> historyMap = new HashMap<>();

                // create a defensive copy
                final List<SecurityNode> securityNodes =
                        new ArrayList<>(checkListView.getCheckModel().getCheckedItems());

                // need to determine the total count
                long historyCount = 0;

                // Collect and count the number of nodes
                for (final SecurityNode securityNode : securityNodes) {
                    updateMessage(ResourceUtils.getString("Message.DownloadingX", securityNode.getSymbol()));

                    final List<SecurityHistoryNode> historyNodes =
                            UpdateFactory.downloadHistory(securityNode, startDate, endDate);

                    Collections.reverse(historyNodes);  // reverse the sort order

                    historyCount += historyNodes.size();

                    historyMap.put(securityNode, historyNodes);
                }

                // need to track the total processed count
                long processedHistory = 0;

                for (final Map.Entry<SecurityNode, List<SecurityHistoryNode>> entry : historyMap.entrySet()) {
                    if (!requestCancel) {
                        for (final SecurityHistoryNode historyNode : entry.getValue()) {
                            if (!requestCancel) {
                                engine.addSecurityHistory(entry.getKey(), historyNode);
                                updateProgress(++processedHistory, historyCount);

                                updateMessage(ResourceUtils.getString("Message.UpdatedPriceDate", entry.getKey().getSymbol(),
                                        dateTimeFormatter.format(historyNode.getLocalDate())));
                            }
                        }
                    }
                }

                updateMessage("");

                return null;
            }
        };

        updateTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> taskComplete());
        updateTask.addEventHandler(WorkerStateEvent.WORKER_STATE_CANCELLED, event -> taskComplete());
        updateTask.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, event -> taskComplete());

        progressBar.progressProperty().bind(updateTask.progressProperty());
        messageLabel.textProperty().bind(updateTask.messageProperty());

        final Thread thread = new Thread(updateTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void taskComplete() {
        requestCancel = false;

        progressBar.progressProperty().unbind();
        progressBar.progressProperty().set(0);

        messageLabel.textProperty().unbind();
        updateTask = null;

        checkListView.getCheckModel().clearChecks();

        disableUIProperty.setValue(false);
    }

    @FXML
    private void handleCloseAction() {
        handleStopAction();

        ((Stage) parentProperty.get().getWindow()).close();
    }

    @FXML
    private void handleStopAction() {
        if (updateTask != null) {
            requestCancel = true;

            try {
                updateTask.get();    // wait for a graceful exit
            } catch (final ExecutionException | InterruptedException e) {
                Logger.getLogger(HistoricalImportController.class.getName()).log(Level.SEVERE,
                        e.getLocalizedMessage(), e);
            }
        }
    }
}
