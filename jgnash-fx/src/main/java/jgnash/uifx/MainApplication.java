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

import jgnash.MainFX;
import jgnash.engine.EngineFactory;
import jgnash.uifx.tasks.CloseFileTask;
import jgnash.uifx.utils.StageUtils;
import jgnash.util.ResourceUtils;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * JavaFX version of jGnash.
 *
 * @author Craig Cavanaugh
 */
public class MainApplication extends Application {
    // private static final Logger logger = Logger.getLogger(MainApplication.class.getName());

    protected static Stage primaryStage;

    private ToolBar viewToolBar;

    @Override
    public void start(final Stage stage) throws Exception {
        primaryStage = stage;

        MenuBar menuBar = FXMLLoader.load(MainFX.class.getResource("fxml/MainMenuBar.fxml"), ResourceUtils.getBundle());
        ToolBar mainToolBar = FXMLLoader.load(MainFX.class.getResource("fxml/MainToolBar.fxml"), ResourceUtils.getBundle());

        viewToolBar = new ToolBar();
        viewToolBar.setOrientation(Orientation.VERTICAL);

        Button button = new Button();
        Label  label  = new Label("Accounts");
        label.setRotate(-90);
        button.setGraphic(new Group(label));

        viewToolBar.getItems().addAll(button);

        VBox top = new VBox();
        top.getChildren().addAll(menuBar, mainToolBar);
        top.setFillWidth(true);

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(top);
        borderPane.setLeft(viewToolBar);

        Scene scene = new Scene(borderPane, 600, 400);

        stage.setTitle(MainFX.VERSION);
        stage.setScene(scene);
        stage.setResizable(true);

        installHandlers();

        StageUtils.addBoundsListener(stage, getClass());

        stage.show();
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

    @Override
    public void stop() {
        System.out.println("Shutting down");
    }
}
