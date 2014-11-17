/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.uifx;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jgnash.MainFX;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.net.security.AbstractYahooParser;
import jgnash.net.security.UpdateFactory;
import jgnash.uifx.control.BusyPane;
import jgnash.uifx.control.TabViewPane;
import jgnash.uifx.tasks.CloseFileTask;
import jgnash.uifx.utils.StageUtils;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.Nullable;
import jgnash.util.ResourceUtils;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.controlsfx.control.StatusBar;

/**
 * JavaFX version of jGnash.
 *
 * @author Craig Cavanaugh
 */
public class MainApplication extends Application implements MessageListener {
    /**
     * Default style sheet
     */
    public static final String DEFAULT_CSS = "jgnash/skin/default.css";

    private static final Logger logger = Logger.getLogger(MainApplication.class.getName());

    private ResourceBundle rb = ResourceUtils.getBundle();

    private Executor backgroundExecutor = Executors.newSingleThreadExecutor(new DefaultDaemonThreadFactory());

    private LogHandler logHandler = new LogHandler();

    protected static Stage primaryStage;

    private StatusBar statusBar;

    private TabViewPane tabViewPane;

    private static BusyPane busyPane;

    @Override
    public void start(final Stage stage) throws Exception {
        primaryStage = stage;

        busyPane = new BusyPane();

        final MenuBar menuBar = FXMLLoader.load(MainFX.class.getResource("fxml/MainMenuBar.fxml"), rb);
        final ToolBar mainToolBar = FXMLLoader.load(MainFX.class.getResource("fxml/MainToolBar.fxml"), rb);
        tabViewPane = new TabViewPane();

        final VBox top = new VBox();
        top.getChildren().addAll(menuBar, mainToolBar);
        top.setFillWidth(true);

        statusBar = new StatusBar();

        final BorderPane borderPane = new BorderPane();
        borderPane.setTop(top);
        borderPane.setBottom(statusBar);
        borderPane.setCenter(tabViewPane);
        BorderPane.setAlignment(tabViewPane, Pos.CENTER);

        final StackPane stackPane = new StackPane();
        stackPane.getChildren().addAll(borderPane, busyPane);

        final Scene scene = new Scene(stackPane, 600, 400);
        scene.getStylesheets().add(DEFAULT_CSS);

        stage.setTitle(MainFX.VERSION);
        stage.setScene(scene);
        stage.setResizable(true);

        installHandlers();

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM);

        StageUtils.addBoundsListener(stage, getClass());

        stage.show();

        registerLogHandler(Engine.class);
        registerLogHandler(EngineFactory.class);
        registerLogHandler(AbstractYahooParser.class);

        UpdateFactory.addLogHandler(logHandler);
    }

    private void addViews() {
        try {
            Pane pane = FXMLLoader.load(MainFX.class.getResource("fxml/AccountsView.fxml"), ResourceUtils.getBundle());

            tabViewPane.addTab(pane, rb.getString("Tab.Accounts"));
            tabViewPane.addTab(null, rb.getString("Tab.Register"));
            tabViewPane.addTab(null, rb.getString("Tab.Reminders"));
            tabViewPane.addTab(null, rb.getString("Tab.Budgeting"));

        } catch (final IOException e) {
            StaticUIMethods.displayException(e);
        }
    }

    private void removeViews() {
        tabViewPane.getTabs().clear();
    }

    private void installHandlers() {

        // Close the file cleanly if it is still open
        //   primaryStage.setOnHiding(windowEvent -> {... does not work, bug?
        getPrimaryStage().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, windowEvent -> {
            if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {
                windowEvent.consume();  // consume the event and let the shutdown handler deal with closure
                CloseFileTask.initiateShutdown();
            }
        });
    }

    /**
     * Provides access to the primary stage.
     *
     * @return the primary stage
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    private void displayStatus(final String status) {
        Platform.runLater(() -> statusBar.setText(status));
    }

    /**
     * Sets the application in a busy state and waits for the provided {@code Task} to complete.
     * The caller is responsible for starting the task.
     *
     * @param task long running {@code Task} to monitor
     */
    public static void setBusy(@Nullable final Task<?> task) {
        busyPane.setTask(task);
    }

    @Override
    public void stop() {
        logger.info("Shutting down");
    }

    @Override
    public void messagePosted(final Message event) {
        Platform.runLater(() -> {
            switch (event.getEvent()) {
                case FILE_LOAD_SUCCESS:
                case FILE_NEW_SUCCESS:
                    updateTitle();
                    addViews();
                    break;
                case FILE_CLOSING:
                    removeViews();
                    updateTitle();
                    break;
                case FILE_IO_ERROR:
                case FILE_LOAD_FAILED:
                case FILE_NOT_FOUND:
                    displayStatus("File system error TBD");  // TODO: need a description
                case ACCOUNT_REMOVE_FAILED:
                    StaticUIMethods.displayError(rb.getString("Message.Error.AccountRemove"));
                    break;
                case BACKGROUND_PROCESS_STARTED:
                    setBusyBackground(true);
                    break;
                case BACKGROUND_PROCESS_STOPPED:
                    setBusyBackground(false);
                    break;
                default:
                    break;
            }
        });
    }

    private void setBusyBackground(final boolean busy) {
        if (busy) {
            statusBar.setProgress(-1);
        } else {
            statusBar.setProgress(0);
        }
    }

    private void updateTitle() {
        backgroundExecutor.execute(() -> {
            Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

            Platform.runLater(() -> {
                if (engine != null) {
                    getPrimaryStage().setTitle(MainFX.VERSION + "  [" + EngineFactory.getActiveDatabase() + ']');
                } else {
                    getPrimaryStage().setTitle(MainFX.VERSION);
                }
            });
        });
    }

    private void registerLogHandler(final Class<?> clazz) {
        Logger.getLogger(clazz.getName()).addHandler(logHandler);
    }

    private class LogHandler extends Handler {

        @Override
        public void close() throws SecurityException {
        }

        @Override
        public void flush() {
        }

        @Override
        public synchronized void publish(final LogRecord record) {

            if (record.getLevel() == Level.WARNING || record.getLevel() == Level.SEVERE) {
                // TODO add / remove warning and info icon
                displayStatus(record.getMessage());


            } else if (record.getLevel() == Level.INFO) {
                displayStatus(record.getMessage());
            }

        }
    }
}
