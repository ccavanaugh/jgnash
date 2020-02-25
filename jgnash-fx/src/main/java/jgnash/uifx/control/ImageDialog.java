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
package jgnash.uifx.control;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.views.main.MainView;

/**
 * A dialog for displaying and printing an image.
 *
 * @author Craig Cavanaugh
 */
public class ImageDialog {

    private static final int MARGIN = 20;
    private static final int MIN_SIZE = 100;

    private final Stage dialog;
    private final ImageView imageView = new ImageView();
    private final StatusBar statusBar = new StatusBar();

    public static void showImage(final Path path) {
        final ImageDialog imageDialog = new ImageDialog();
        imageDialog.setImage(path);

        imageDialog.dialog.show();
    }

    private ImageDialog() {
        final ResourceBundle resources = ResourceUtils.getBundle();

        dialog = new Stage(StageStyle.DECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(MainView.getPrimaryStage());
        dialog.setTitle(resources.getString("Title.ViewImage"));

        // Set a sane default size
        dialog.setWidth(450);
        dialog.setHeight(350);

        final BorderPane borderPane = new BorderPane();
        final ToolBar toolBar = new ToolBar();

        final Button printButton = new Button(resources.getString("Button.Print"));

        printButton.setOnAction(event -> JavaFXUtils.printImageView(imageView));

        toolBar.getItems().add(printButton);

        final StackPane stackPane = new StackPane();
        stackPane.getChildren().add(imageView);

        stackPane.setMinSize(MIN_SIZE, MIN_SIZE);

        imageView.fitWidthProperty().bind(stackPane.widthProperty().subtract(MARGIN));
        imageView.fitHeightProperty().bind(stackPane.heightProperty().subtract(MARGIN));

        borderPane.setTop(toolBar);
        borderPane.setCenter(stackPane);
        borderPane.setBottom(statusBar);
        borderPane.styleProperty().bind(ThemeManager.styleProperty());

        dialog.setScene(new Scene(borderPane));
        ThemeManager.applyStyleSheets(dialog.getScene());

        // Remember dialog size and location
        StageUtils.addBoundsListener(dialog, ImageDialog.class, MainView.getPrimaryStage());
    }

    private void setImage(final Path path) {
        if (Files.exists(path)) {
            try {
                final Image image = new Image(path.toUri().toURL().toString(), true);
                imageView.setPreserveRatio(true);
                imageView.setImage(image);
                statusBar.textProperty().set(path.toString());
            } catch (final MalformedURLException e) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
            }
        } else {
            StaticUIMethods.displayError(ResourceUtils.getString("Message.Error.MissingAttachment", path.toString()));
        }
    }
}
