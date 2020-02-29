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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.views.main.MainView;
import jgnash.util.NotNull;

/**
 * FXML Utility methods.
 *
 * @author Craig Cavanaugh
 */
public class FXMLUtils {

    private FXMLUtils() {
        // Utility class
    }

    /**
     * A pair consisting of the Stage and fxml controller.
     *
     * @param <C> the fxml controller type
     */
    public static class Pair<C> {
        private final C controller;
        private final Stage stage;

        Pair(final C controller, final Stage stage) {
            this.controller = controller;
            this.stage = stage;
        }

        public C getController() {
            return controller;
        }

        public Stage getStage() {
            return stage;
        }
    }

    /**
     * Loads a scene and sets the specified {@code Stage} as the root and controller.  Application defaults are
     * set for the {@code Stage}
     *
     * @param stage          {@code Stage}
     * @param fileName       name of the fxml file.  It's assumed to be in the same package as the stage
     * @param resourceBundle {@code ResourceBundle} to pass to the {@code FXMLLoader}
     */
    public static void loadFXML(@NotNull final Stage stage, @NotNull final String fileName,
                                @NotNull final ResourceBundle resourceBundle) {
        final FXMLLoader fxmlLoader = new FXMLLoader(stage.getClass().getResource(fileName), resourceBundle);
        fxmlLoader.setRoot(stage);
        fxmlLoader.setController(stage);

        try {
            fxmlLoader.load();
        } catch (final IOException e) {
            Logger.getLogger(stage.getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
        }

        stage.initOwner(MainView.getPrimaryStage());
        ThemeManager.applyStyleSheets(stage.getScene());
        stage.getScene().getRoot().styleProperty().bind(ThemeManager.styleProperty());
        stage.initStyle(StageStyle.DECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.getIcons().add(StaticUIMethods.getApplicationIcon());
    }

    /**
     * Reduces boilerplate code to load a fxml file.
     *
     * @param consumer       {@code Consumer to pass to the parent node},
     * @param fileName       name of the fxml file.  It's assumed to be in the same package as the consumer
     * @param resourceBundle {@code ResourceBundle} to pass to the {@code FXMLLoader}
     * @param <R>            must extend {@code Node}
     * @param <C>            the fxml controller
     * @return the controller for the fxml file
     */
    public static <R extends Node, C> C loadFXML(@NotNull final Consumer<R> consumer, @NotNull final String fileName,
                                                 @NotNull final ResourceBundle resourceBundle) {
        final URL fxmlUrl = consumer.getClass().getResource(fileName);
        final FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl, resourceBundle);

        try {
            R root = fxmlLoader.load();
            C controller = fxmlLoader.getController();
            consumer.accept(root);

            root.styleProperty().bind(ThemeManager.styleProperty());

            // Inject the root into the controller
            injectParent(controller, root);

            return controller;
        } catch (final IOException ioe) { // log and throw an unchecked exception
            Logger.getLogger(FXMLUtils.class.getName()).log(Level.SEVERE, ioe.getMessage(), ioe);
            throw new UncheckedIOException(ioe);
        }
    }

    private static Field[] getDeclaredFields(final Class<?> clazz) {
        final List<Field> fields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));

        if (clazz.getSuperclass() != null) {
            fields.addAll(Arrays.asList(getDeclaredFields(clazz.getSuperclass())));
        }
        return fields.toArray(new Field[0]);
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
            if (field.isAnnotationPresent(InjectFXML.class) && field.getName().equals("parent")) {
                field.setAccessible(true);
                try {
                    final ObjectProperty<Object> property = (ObjectProperty<Object>) field.get(object);
                    property.set(value);
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

        if (MainView.getInstance() != null) {    // null check is only necessary to pass unit tests
            stage.initOwner(MainView.getPrimaryStage());
        }

        try {
            fxmlLoader.setController(controller);

            final Scene scene = new Scene(fxmlLoader.load());
            ThemeManager.applyStyleSheets(scene);
            scene.getRoot().styleProperty().bind(ThemeManager.styleProperty());

            stage.setScene(scene);
            stage.getIcons().add(StaticUIMethods.getApplicationIcon());

            stage.sizeToScene();    // force a resize, some stages need a push

            // Inject the scene into the controller
            injectParent(controller, scene);
        } catch (final IOException ioe) { // log and throw an unchecked exception
            Logger.getLogger(FXMLUtils.class.getName()).log(Level.SEVERE, ioe.getMessage(), ioe);
            throw new UncheckedIOException(ioe);
        }

        return stage;
    }

    /**
     * Simple FXML loader that handles exceptions.
     *
     * @param fxmlUrl        the fxml {@code URL}
     * @param resourceBundle {@code ResourceBundle} to pass to the {@code FXMLLoader}
     * @param <T>            expected return type being loaded
     * @return The loaded object hierarchy.
     * @see FXMLLoader#load()
     */
    public static <T> T load(final URL fxmlUrl, final ResourceBundle resourceBundle) {
        final FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl, resourceBundle);

        try {
            return fxmlLoader.load();
        } catch (IOException e) {
            StaticUIMethods.displayException(e);
        }

        return null;
    }

    /**
     * Creates a new Stage with application defaults {@code StageStyle.DECORATED}, {@code Modality.APPLICATION_MODAL}
     * with the specified fxml {@code URL} as the {@code Scene}.  The default resource bundle is used.
     *
     * @param fxmlUrl the fxml {@code URL}
     * @param title the Stage title
     * @param <C> the fxml controller type
     * @return Pair containing the Stage and controller
     */
    public static <C> Pair<C> load(@NotNull final URL fxmlUrl, final String title) {
        final FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl, ResourceUtils.getBundle());

        final Stage stage = new Stage(StageStyle.DECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(MainView.getPrimaryStage());

        try {
            final Scene scene = new Scene(fxmlLoader.load());
            ThemeManager.applyStyleSheets(scene);
            scene.getRoot().styleProperty().bind(ThemeManager.styleProperty());

            final C controller = fxmlLoader.getController();

            stage.setScene(scene);
            stage.getIcons().add(StaticUIMethods.getApplicationIcon());
            stage.setTitle(title);

            stage.sizeToScene();    // force a resize, some stages need a push

            // Inject the scene into the controller
            injectParent(controller, scene);

            stage.setOnShown(event -> JavaFXUtils.runNow(() -> {
                stage.sizeToScene();    // force the stage to resize to the scene before setting the minimums

                stage.setMinHeight(stage.getHeight());
                stage.setMinWidth(stage.getWidth());
            }));

            return new Pair<>(controller, stage);
        } catch (final IOException ioe) { // log and throw an unchecked exception
            Logger.getLogger(FXMLUtils.class.getName()).log(Level.SEVERE, ioe.getMessage(), ioe);
            throw new UncheckedIOException(ioe);
        }
    }
}
