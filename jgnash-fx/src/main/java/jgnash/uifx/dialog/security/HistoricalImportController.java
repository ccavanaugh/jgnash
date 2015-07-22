package jgnash.uifx.dialog.security;

import java.text.DateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.beans.property.ObjectProperty;
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
import jgnash.util.DateUtils;
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
    private Label messageLabel;

    @FXML
    private Button okButton;

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

    @FXML
    void initialize() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final List<SecurityNode> securityNodes = engine.getSecurities().stream()
                .filter(securityNode -> securityNode.getQuoteSource() != QuoteSource.NONE).collect(Collectors.toList());

        Collections.sort(securityNodes);

        checkListView.getItems().addAll(securityNodes);

        startDatePicker.setValue(LocalDate.now().minusMonths(1));
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
        okButton.disableProperty().setValue(true);
        checkListView.disableProperty().setValue(true);

        final DateFormat dateFormat = DateUtils.getShortDateFormat();

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        updateTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                final Date startDate = startDatePicker.getDate();
                final Date endDate = endDatePicker.getDate();

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

                    historyCount += historyNodes.size();

                    historyMap.put(securityNode, historyNodes);
                }

                // need to track the total processed count
                long processedHistory = 0;

                for (final SecurityNode securityNode : historyMap.keySet()) {
                    if (!requestCancel) {
                        for (final SecurityHistoryNode historyNode : historyMap.get(securityNode)) {
                            if (!requestCancel) {
                                engine.addSecurityHistory(securityNode, historyNode);
                                updateProgress(++processedHistory, historyCount);

                                updateMessage(ResourceUtils.getString("Message.UpdatedPriceDate", securityNode.getSymbol(),
                                        dateFormat.format(historyNode.getDate())));
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

        okButton.disableProperty().setValue(false);
        checkListView.disableProperty().setValue(false);
    }

    @FXML
    private void handleCancelAction() {

        if (updateTask != null) {
            requestCancel = true;

            try {
                updateTask.get();    // wait for a graceful exit
            } catch (final ExecutionException | InterruptedException e) {
                Logger.getLogger(HistoricalImportController.class.getName()).log(Level.SEVERE,
                        e.getLocalizedMessage(), e);
            }
        }

        ((Stage) parentProperty.get().getWindow()).close();
    }
}
