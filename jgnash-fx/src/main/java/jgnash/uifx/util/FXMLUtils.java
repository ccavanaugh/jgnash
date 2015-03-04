package jgnash.uifx.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.uifx.MainApplication;

/**
 * FXML Utility methods
 *
 * @author Craig Cavanaugh
 */
public class FXMLUtils {

    private FXMLUtils() {
        // Utility class
    }

    /**
     * Reduces boilerplate code to load an fxml file
     *
     * @param consumer {@code Consumer to pass to the parent node},
     * @param fileName name of the fxml file.  It's assumed to be in the same package as the consumer
     * @param resourceBundle {@code ResourceBundle} to pass to the {@code FXMLLoader}
     * @param <R> must extend {code Node}
     * @param <C> the fxml controller
     *
     * @return the controller for the fxml file
     */
    public static <R, C> C loadFXML(final Consumer<R> consumer, final String fileName, final ResourceBundle resourceBundle) {
        final URL fxmlUrl = consumer.getClass().getResource(fileName);
        final FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl, resourceBundle);

        try {
            R root = fxmlLoader.load();
            C controller = fxmlLoader.getController();
            consumer.accept(root);

            return controller;
        } catch (final IOException ioe) { // log and throw an unchecked exception
            Logger.getLogger(FXMLUtils.class.getName()).log(Level.SEVERE, ioe.getMessage(), ioe);
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Creates a new Stage with application defaults {@code StageStyle.DECORATED}, {@code Modality.APPLICATION_MODAL}
     * with the specified fxml file as the {@code Scene}.
     *
     * @param controller controller object to pass to the {@code FXMLLoader}
     * @param fileName name of the fxml file.  It's assumed to be in the same package as the controller
     * @param resourceBundle {@code ResourceBundle} to pass to the {@code FXMLLoader}
     * @return new {@code Stage}
     */
    public static Stage loadFXML(final Object controller, final String fileName, final ResourceBundle resourceBundle) {
        final URL fxmlUrl = controller.getClass().getResource(fileName);
        final FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl, resourceBundle);

        final Stage stage = new Stage(StageStyle.DECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);

        if (MainApplication.getInstance() != null) {    // null check is only necessary to pass unit tests
            stage.initOwner(MainApplication.getInstance().getPrimaryStage());
        }

        try {
            fxmlLoader.setController(controller);

            final Scene scene = new Scene(fxmlLoader.load());
            scene.getStylesheets().addAll(MainApplication.DEFAULT_CSS);

            stage.setScene(scene);
        } catch (final IOException ioe) { // log and throw an unchecked exception
            Logger.getLogger(FXMLUtils.class.getName()).log(Level.SEVERE, ioe.getMessage(), ioe);
            throw new UncheckedIOException(ioe);
        }

        return stage;
    }

    /**
     * Creates a new Stage with application defaults {@code StageStyle.DECORATED}, {@code Modality.APPLICATION_MODAL}
     * with the specified fxml {@code URL} as the {@code Scene}.
     *
     * @param fxmlUrl the fxml {@code URL}
     * @param resourceBundle {@code ResourceBundle} to pass to the {@code FXMLLoader}
     * @return new {@code Stage}
     */
    public static Stage loadFXML(final URL fxmlUrl, final ResourceBundle resourceBundle) {
        final FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl, resourceBundle);

        final Stage stage = new Stage(StageStyle.DECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(MainApplication.getInstance().getPrimaryStage());

        try {
            final Scene scene = new Scene(fxmlLoader.load());
            scene.getStylesheets().addAll(MainApplication.DEFAULT_CSS);

            stage.setScene(scene);
        } catch (final IOException ioe) { // log and throw an unchecked exception
            Logger.getLogger(FXMLUtils.class.getName()).log(Level.SEVERE, ioe.getMessage(), ioe);
            throw new UncheckedIOException(ioe);
        }

        return stage;
    }
}
