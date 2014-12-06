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
import jgnash.util.Nullable;
import jgnash.util.ResourceUtils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
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

        alert.getDialogPane().getScene().getStylesheets().add(MainApplication.DEFAULT_CSS);

        Platform.runLater(alert::showAndWait);
    }

    public static void displayMessage(final String message) {
        final Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(ResourceUtils.getBundle().getString("Title.Information"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(MainApplication.getPrimaryStage());

        alert.getDialogPane().getScene().getStylesheets().add(MainApplication.DEFAULT_CSS);

        Platform.runLater(alert::showAndWait);
    }

    /**
     * Displays a Yes and No dialog requesting confirmation
     *
     * @param title Dialog title
     * @param message Dialog message
     * @return {@code ButtonBar.ButtonData.YES} or {@code ButtonBar.ButtonData.NO}
     */
    public static ButtonType showConfirmationDialog(final String title, final String message) {

        // Yes and no is preferred, but there appears to be a bug
        //final ResourceBundle rb = ResourceUtils.getBundle();
        //ButtonType buttonTypeYes = new ButtonType(rb.getString("Button.Yes"), ButtonBar.ButtonData.YES);
        //ButtonType buttonTypeNo = new ButtonType(rb.getString("Button.No"), ButtonBar.ButtonData.NO);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(null);

        alert.initOwner(MainApplication.getPrimaryStage());
        alert.getDialogPane().getScene().getStylesheets().add(MainApplication.DEFAULT_CSS);

        return alert.showAndWait().get();
    }

    public static void displayException(final Throwable exception) {

        final Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(ResourceUtils.getBundle().getString("Title.Error"));
        alert.setHeaderText(exception.getLocalizedMessage());
        alert.initOwner(MainApplication.getPrimaryStage());

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

        Platform.runLater(alert::showAndWait);
    }

    public static void displayTaskProgress(final Task<?> task) {
        MainApplication.setBusy(task);
    }

    /**
     * Returns a JavaFx Image from the class path
     * @param image resource path
     * @return {@code} Image or {@code null} if not found
     */
    @Nullable
    public static Image getImage(final String image) {
        Image resourceImage;

        try {
            resourceImage = new Image(StaticUIMethods.class.getResourceAsStream(image));
        } catch (final Exception ex) {
            Logger.getLogger(StaticUIMethods.class.getName()).log(Level.WARNING, ex.getLocalizedMessage(), ex);
            resourceImage = null;
        }

        return resourceImage;
    }

    /**
     * Handler for displaying uncaught exceptions
     */
    public static class ExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(final Thread t, final Throwable throwable) {
            // ignore any exceptions thrown by the help plaf
            if (throwable.getStackTrace()[0].getClassName().contains("help.plaf")) {
                return;
            }

            Logger.getLogger(ExceptionHandler.class.getName()).log(Level.SEVERE, throwable.getLocalizedMessage(), throwable);

            displayException(throwable);
        }
    }
}
