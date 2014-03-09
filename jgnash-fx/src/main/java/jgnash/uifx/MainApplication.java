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

import jgnash.MainFX;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.uifx.control.TabViewPane;
import jgnash.uifx.tasks.CloseFileTask;
import jgnash.uifx.utils.StageUtils;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.ResourceUtils;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

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

    // private static final Logger logger = Logger.getLogger(MainApplication.class.getName());

    protected static Stage primaryStage;

    private Label statusLabel;

    private TabViewPane tabViewPane;

    private Executor backgroundExecutor = Executors.newSingleThreadExecutor(new DefaultDaemonThreadFactory());

    private ResourceBundle rb = ResourceUtils.getBundle();

    @Override
    public void start(final Stage stage) throws Exception {
        primaryStage = stage;

        MenuBar menuBar = FXMLLoader.load(MainFX.class.getResource("fxml/MainMenuBar.fxml"), rb);
        ToolBar mainToolBar = FXMLLoader.load(MainFX.class.getResource("fxml/MainToolBar.fxml"), rb);
        tabViewPane = new TabViewPane();

        VBox top = new VBox();
        top.getChildren().addAll(menuBar, mainToolBar);
        top.setFillWidth(true);

        HBox bottom = new HBox();
        statusLabel = new Label("Ready");
        bottom.getChildren().addAll(statusLabel);

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(top);
        borderPane.setBottom(bottom);
        borderPane.setCenter(tabViewPane);
        BorderPane.setAlignment(tabViewPane, Pos.CENTER);
        BorderPane.setMargin(bottom, new Insets(8, 8, 8, 8));

        Scene scene = new Scene(borderPane, 600, 400);
        scene.getStylesheets().add(DEFAULT_CSS);

        stage.setTitle(MainFX.VERSION);
        stage.setScene(scene);
        stage.setResizable(true);

        installHandlers();

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM);

        StageUtils.addBoundsListener(stage, getClass());

        stage.show();
    }

    private void addViews() {
        try {
            Pane pane = FXMLLoader.load(MainFX.class.getResource("fxml/Accounts.fxml"), ResourceUtils.getBundle());

            tabViewPane.addTab(pane, "Accounts");
            tabViewPane.addTab(null, "Register");
            tabViewPane.addTab(null, "Reminders");
            tabViewPane.addTab(null, "Budget");

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
        Platform.runLater(() -> statusLabel.setText(status));
    }

    @Override
    public void stop() {
        System.out.println("Shutting down");
    }

    @Override
    public void messagePosted(final Message event) {
        Platform.runLater(() -> {
            switch (event.getEvent()) {
                case FILE_LOAD_SUCCESS:
                case FILE_NEW_SUCCESS:
                    updateTitle();
                    displayStatus("File loaded");
                    addViews();
                    break;
                case FILE_CLOSING:
                    removeViews();
                    updateTitle();
                    displayStatus("File closed");
                    break;
                case FILE_IO_ERROR:
                case FILE_LOAD_FAILED:
                case FILE_NOT_FOUND:
                    displayStatus("File system error TBD");  // TODO: need a description
                case ACCOUNT_REMOVE_FAILED:
                    StaticUIMethods.displayError(rb.getString("Message.Error.AccountRemove"));
                    break;
                default:
                    break;
            }
        });
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
}
