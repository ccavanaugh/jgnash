package jgnash.uifx.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.uifx.MainApplication;
import jgnash.uifx.StaticUIMethods;
import jgnash.util.NotNull;

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
     * Loads a scene and sets the specified {@code Stage} as the root and controller.  Application defaults are
     * set for the {@code Stage}
     *
     * @param stage          {@code Stage}
     * @param fileName       name of the fxml file.  It's assumed to be in the same package as the stage
     * @param resourceBundle {@code ResourceBundle} to pass to the {@code FXMLLoader}
     */
    public static void loadFXML(@NotNull final Stage stage, @NotNull final String fileName, @NotNull final ResourceBundle resourceBundle) {
        final FXMLLoader fxmlLoader = new FXMLLoader(stage.getClass().getResource(fileName), resourceBundle);
        fxmlLoader.setRoot(stage);
        fxmlLoader.setController(stage);

        try {
            fxmlLoader.load();
        } catch (final IOException e) {
            Logger.getLogger(stage.getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
        }

        stage.initOwner(MainApplication.getInstance().getPrimaryStage());
        stage.getScene().getStylesheets().addAll(MainApplication.DEFAULT_CSS);
        stage.initStyle(StageStyle.DECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.getIcons().add(StaticUIMethods.getApplicationIcon());

        StageUtils.addBoundsListener(stage, stage.getClass());
    }

    /**
     * Reduces boilerplate code to load an fxml file
     *
     * @param consumer       {@code Consumer to pass to the parent node},
     * @param fileName       name of the fxml file.  It's assumed to be in the same package as the consumer
     * @param resourceBundle {@code ResourceBundle} to pass to the {@code FXMLLoader}
     * @param <R>            must extend {code Node}
     * @param <C>            the fxml controller
     * @return the controller for the fxml file
     */
    public static <R, C> C loadFXML(final Consumer<R> consumer, final String fileName, final ResourceBundle resourceBundle) {
        final URL fxmlUrl = consumer.getClass().getResource(fileName);
        final FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl, resourceBundle);

        try {
            R root = fxmlLoader.load();
            C controller = fxmlLoader.getController();
            consumer.accept(root);

            // Inject the root into the controller
            injectParent(controller, root);

            return controller;
        } catch (final IOException ioe) { // log and throw an unchecked exception
            Logger.getLogger(FXMLUtils.class.getName()).log(Level.SEVERE, ioe.getMessage(), ioe);
            throw new UncheckedIOException(ioe);
        }
    }

    private static Field[] getDeclaredFields(final Class clazz) {
        final List<Field> fields = new ArrayList<>();
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));

        if (clazz.getSuperclass() != null) {
            fields.addAll(Arrays.asList(getDeclaredFields(clazz.getSuperclass())));
        }
        return fields.toArray(new Field[fields.size()]);
    }

    /**
     * Injects a value into a field of initialized type {@code ObjectProperty}.  The field must also be annotated
     * with {@code javax.inject.Inject}
     *
     * @param object {@code Object} to search for field
     * @param value  value to set
     * @see javafx.beans.property.ObjectProperty
     * @see InjectFXML
     */
    @SuppressWarnings("unchecked")
    private static void injectParent(final Object object, final Object value) {
        for (final Field field : getDeclaredFields(object.getClass())) {
            if (field.isAnnotationPresent(InjectFXML.class) && field.getName().equals("parentProperty")) {
                field.setAccessible(true);
                try {
                    final ObjectProperty property = (ObjectProperty) field.get(object);
                    property.setValue(value);
                } catch (IllegalAccessException e) {
                    Logger.getLogger(FXMLUtils.class.getName()).log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Creates a new Stage with application defaults {@code StageStyle.DECORATED}, {@code Modality.APPLICATION_MODAL}
     * with the specified fxml file as the {@code Scene}.
     *
     * @param controller     controller object to pass to the {@code FXMLLoader}
     * @param fileName       name of the fxml file.  It's assumed to be in the same package as the controller
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
            stage.getIcons().add(StaticUIMethods.getApplicationIcon());

            // Inject the scene into the controller
            injectParent(controller, scene);
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
     * @param fxmlUrl        the fxml {@code URL}
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
            stage.getIcons().add(StaticUIMethods.getApplicationIcon());
        } catch (final IOException ioe) { // log and throw an unchecked exception
            Logger.getLogger(FXMLUtils.class.getName()).log(Level.SEVERE, ioe.getMessage(), ioe);
            throw new UncheckedIOException(ioe);
        }

        return stage;
    }
}
