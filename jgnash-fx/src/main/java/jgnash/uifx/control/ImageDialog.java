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
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.uifx.MainApplication;
import jgnash.uifx.StaticUIMethods;
import jgnash.util.ResourceUtils;

/**
 * @author Craig Cavanaugh
 */
public class ImageDialog {

    private Stage dialog;

    private ImageView imageView = new ImageView();

    public static void showImage(final Path path) {
        ImageDialog imageDialog = new ImageDialog();
        imageDialog.setImage(path);

        imageDialog.dialog.show();
    }

    ImageDialog() {
        final ResourceBundle resources = ResourceUtils.getBundle();

        dialog = new Stage(StageStyle.DECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(MainApplication.getPrimaryStage());
        dialog.setTitle(resources.getString("Title.ViewImage"));
        dialog.setMinWidth(250);
        dialog.setMinWidth(250);

        final ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(imageView);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        dialog.setScene(new Scene(scrollPane));

        dialog.getScene().getStylesheets().add(MainApplication.DEFAULT_CSS);
        dialog.getScene().getRoot().getStyleClass().addAll("form", "dialog");
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
