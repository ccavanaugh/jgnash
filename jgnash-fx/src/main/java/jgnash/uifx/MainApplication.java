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
import jgnash.uifx.utils.StageUtils;
import jgnash.util.ResourceUtils;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX version of jGnash.
 *
 * @author Craig Cavanaugh
 */
public class MainApplication extends Application {
    // private static final Logger logger = Logger.getLogger(MainApplication.class.getName());

    protected static Stage primaryStage;

    @Override
    public void start(final Stage stage) throws Exception {
        primaryStage = stage;

        MenuBar menuBar = FXMLLoader.load(MainFX.class.getResource("fxml/MainMenuBar.fxml"), ResourceUtils.getBundle());

        VBox root = new VBox();
        Scene scene = new Scene(root, 300, 150);

        root.getChildren().add(menuBar);

        stage.setTitle(MainFX.VERSION);
        stage.setScene(scene);
        stage.setResizable(true);

        StageUtils.addBoundsListener(stage, getClass());

        stage.show();
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
