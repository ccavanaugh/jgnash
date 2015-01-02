package jgnash.uifx.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
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
     * @param parentNode {@code Consumer to pass to the parent node},
     * @param fileName name of the fxml file.  It's assumed to be in the same package as the parentNode
     * @param resourceBundle {@code ResourceBundle} to pass to the {@code FXMLLoader}
     * @param <N> must extend {code Node}
     * @param <C> must implement {@code Initializable}
     *
     * @return the controller for the fxml file
     */
    public static <N extends Node, C extends Initializable> C loadFXML(final Consumer<N> parentNode, final String fileName, final ResourceBundle resourceBundle) {
        final URL fxmlUrl = parentNode.getClass().getResource(fileName);
        final FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl, resourceBundle);

        try {
            N node = fxmlLoader.load();
            C controller = fxmlLoader.getController();
            parentNode.accept(node);

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
     * @param controller {@code Initializable to pass to the {@code FXMLLoader}
     * @param fileName name of the fxml file.  It's assumed to be in the same package as the controller
     * @param resourceBundle {@code ResourceBundle} to pass to the {@code FXMLLoader}
     * @return new {@code Stage}
     */
    public static Stage loadFXML(final Initializable controller, final String fileName, final ResourceBundle resourceBundle) {
        final URL fxmlUrl = controller.getClass().getResource(fileName);
        final FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl, resourceBundle);

        final Stage stage = new Stage(StageStyle.DECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(MainApplication.getPrimaryStage());

        try {
            fxmlLoader.setController(controller);
            stage.setScene(new Scene(fxmlLoader.load()));
        } catch (final IOException ioe) { // log and throw an unchecked exception
            Logger.getLogger(FXMLUtils.class.getName()).log(Level.SEVERE, ioe.getMessage(), ioe);
            throw new UncheckedIOException(ioe);
        }

        return stage;
    }
}
