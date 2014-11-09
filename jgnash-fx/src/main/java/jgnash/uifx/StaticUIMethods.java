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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.MainFX;
import jgnash.uifx.utils.StageUtils;
import jgnash.util.ResourceUtils;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Various static UI support methods
 *
 * @author Craig Cavanaugh
 */
public class StaticUIMethods {

    private StaticUIMethods() {
        // Utility class
    }

    public static void showOpenDialog() {
        try {
            Stage dialog = new Stage(StageStyle.DECORATED);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(MainApplication.getPrimaryStage());
            dialog.setTitle(ResourceUtils.getBundle().getString("Title.Open"));
            dialog.setScene(new Scene(FXMLLoader.load(MainFX.class.getResource("fxml/OpenDatabaseForm.fxml"), ResourceUtils.getBundle())));

            dialog.setResizable(false);

            dialog.getScene().getStylesheets().add(MainApplication.DEFAULT_CSS);
            dialog.getScene().getRoot().getStyleClass().addAll("form", "dialog");

            StageUtils.addBoundsListener(dialog, StaticUIMethods.class);

            dialog.show();
        } catch (final IOException e) {
            Logger.getLogger(StaticUIMethods.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    public static void displayError(final String message) {
        final Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(ResourceUtils.getBundle().getString("Title.Error"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(MainApplication.getPrimaryStage());
        alert.initStyle(StageStyle.UTILITY);
        alert.getDialogPane().getScene().getStylesheets().add(MainApplication.DEFAULT_CSS);

        alert.showAndWait();
    }

    public static void displayException(final Throwable exception) {

        final Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(ResourceUtils.getBundle().getString("Title.Error"));
        alert.setHeaderText(exception.getLocalizedMessage());
        alert.initOwner(MainApplication.getPrimaryStage());
        alert.initStyle(StageStyle.UTILITY);

        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);

        final String stackTrace = sw.toString();

        final TextArea textArea = new TextArea(stackTrace);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        final GridPane expandableContent = new GridPane();
        expandableContent.setMaxWidth(Double.MAX_VALUE);
        expandableContent.add(textArea, 0, 0);

        alert.getDialogPane().setExpandableContent(expandableContent);
        alert.getDialogPane().getScene().getStylesheets().add(MainApplication.DEFAULT_CSS);

        alert.showAndWait();
    }

    public static void displayTaskProgress(final Task<?> task) {
        MainApplication.setBusy(task);
    }
}
