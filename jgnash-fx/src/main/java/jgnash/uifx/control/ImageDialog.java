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
package jgnash.uifx.control;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.uifx.MainApplication;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.util.StageUtils;
import jgnash.util.ResourceUtils;

/**
 * Very simple dialog for displaying an image
 *
 * @author Craig Cavanaugh
 */
public class ImageDialog {

    private final Stage dialog;

    private final ImageView imageView = new ImageView();

    public static void showImage(final Path path) {
        ImageDialog imageDialog = new ImageDialog();
        imageDialog.setImage(path);

        imageDialog.dialog.show();
    }

    private ImageDialog() {
        final ResourceBundle resources = ResourceUtils.getBundle();

        dialog = new Stage(StageStyle.DECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(MainApplication.getPrimaryStage());
        dialog.setTitle(resources.getString("Title.ViewImage"));

        // Set a sane default size
        dialog.setWidth(450);
        dialog.setHeight(350);

        final StackPane stackPane = new StackPane();

        imageView.fitWidthProperty().bind(dialog.widthProperty().subtract(20));
        imageView.fitHeightProperty().bind(dialog.heightProperty().subtract(20));

        stackPane.getChildren().addAll(imageView);

        dialog.setScene(new Scene(stackPane));

        dialog.getScene().getStylesheets().add(MainApplication.DEFAULT_CSS);
        dialog.getScene().getRoot().getStyleClass().addAll("form", "dialog");

        // Remember dialog size and location
        StageUtils.addBoundsListener(dialog, ImageDialog.class);
    }

    void setImage(final Path path) {
        if (Files.exists(path)) {
            try {
                final Image image = new Image(path.toUri().toURL().toString(), true);
                imageView.setPreserveRatio(true);
                imageView.setImage(image);
            } catch (final MalformedURLException e) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
            }
        } else {
            StaticUIMethods.displayError(ResourceUtils.getString("Message.Error.MissingAttachment", path.toString()));
        }
    }
}
