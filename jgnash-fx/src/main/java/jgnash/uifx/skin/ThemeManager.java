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
package jgnash.uifx.skin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.prefs.Preferences;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import jgnash.resource.util.OS;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.views.main.MainView;
import jgnash.util.NotNull;
import jgnash.util.Nullable;

/**
 * Theme manager.
 *
 * @author Craig Cavanaugh
 */
public class ThemeManager {

    /**
     * Default style sheet.
     */
    private static final String DEFAULT_CSS = "jgnash/skin/default.css";

    private static final String USER_STYLE = "userStyle";

    private static final Preferences preferences;

    private static final String LAST = "last";

    private static final String FONT_SCALE = "fontScale";

    private static final String ACCENT_COLOR = "accentColor";

    private static final String BASE_COLOR = "baseColor";

    private static final String FOCUS_COLOR = "focusColor";

    private static Menu themesMenu;

    private static final ToggleGroup toggleGroup = new ToggleGroup();

    private static final ThemeHandler themeHandler = new ThemeHandler();

    private static final byte CASPIAN = 0;

    private static final byte MODENA = 1;

    static final byte ACCENT = 0;

    static final byte BASE = 1;

    static final byte FOCUS = 2;

    private static final String DEFAULT_CASPIAN_ACCENT_COLOR = "#0093ff";

    private static final String DEFAULT_MODENA_ACCENT_COLOR = "#0096c9";

    private static final String DEFAULT_CASPIAN_BASE_COLOR = "#d0d0d0";

    private static final String DEFAULT_MODENA_BASE_COLOR = "#ececec";

    private static final String DEFAULT_CASPIAN_FOCUS_COLOR = "#0093ff";

    private static final String DEFAULT_MODENA_FOCUS_COLOR = "#039ED3";

    private static final String[][] KNOWN_THEMES = {
            {"Modena", Application.STYLESHEET_MODENA},
            {"Caspian", Application.STYLESHEET_CASPIAN},
    };

    private static final String[][] DEFAULT_COLORS = {
            {DEFAULT_CASPIAN_ACCENT_COLOR, DEFAULT_CASPIAN_BASE_COLOR, DEFAULT_CASPIAN_FOCUS_COLOR},
            {DEFAULT_MODENA_ACCENT_COLOR, DEFAULT_MODENA_BASE_COLOR, DEFAULT_MODENA_FOCUS_COLOR}
    };

    private static final DoubleProperty fontScale = new SimpleDoubleProperty(1);

    private static final SimpleObjectProperty<Color> accentColor = new SimpleObjectProperty<>();

    private static final SimpleObjectProperty<Color> baseColor = new SimpleObjectProperty<>();

    private static final SimpleObjectProperty<Color> focusColor = new SimpleObjectProperty<>();

    private static final SimpleObjectProperty<Paint> controlTextFill = new SimpleObjectProperty<>(Color.BLACK);

    private static final StringExpression styleProperty;

    private static final double WINDOWS_DEFAULT = 0.95;

    private static final double OPACITY_FACTOR = 0.1334;

    static {
        preferences = Preferences.userNodeForPackage(ThemeManager.class);

        final StringProperty _accentColor = new SimpleStringProperty();
        final StringProperty _baseColor = new SimpleStringProperty();
        final StringProperty _focusColor = new SimpleStringProperty();
        final StringProperty _faintFocusColor = new SimpleStringProperty();

        // restore the old value, default to a smaller value for Windows OS
        fontScale.set(preferences.getDouble(FONT_SCALE, OS.isSystemWindows() ? WINDOWS_DEFAULT : 1));

        // Save the value when it changes
        fontScale.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                preferences.putDouble(FONT_SCALE, newValue.doubleValue());
            }
        });

        accentColor.set(Color.web(preferences.get(ACCENT_COLOR, DEFAULT_MODENA_ACCENT_COLOR)));
        baseColor.set(Color.web(preferences.get(BASE_COLOR, DEFAULT_MODENA_BASE_COLOR)));
        focusColor.set(Color.web(preferences.get(FOCUS_COLOR, DEFAULT_MODENA_FOCUS_COLOR)));

        // restore the old base color value
        switch (preferences.get(LAST, Application.STYLESHEET_MODENA)) {
            case Application.STYLESHEET_CASPIAN:
                accentColor.set(Color.web(preferences.get(ACCENT_COLOR, DEFAULT_CASPIAN_ACCENT_COLOR)));
                baseColor.set(Color.web(preferences.get(BASE_COLOR, DEFAULT_CASPIAN_BASE_COLOR)));
                focusColor.set(Color.web(preferences.get(FOCUS_COLOR, DEFAULT_CASPIAN_FOCUS_COLOR)));
                break;
            case Application.STYLESHEET_MODENA:
            default:
                accentColor.set(Color.web(preferences.get(ACCENT_COLOR, DEFAULT_MODENA_ACCENT_COLOR)));
                baseColor.set(Color.web(preferences.get(BASE_COLOR, DEFAULT_MODENA_BASE_COLOR)));
                focusColor.set(Color.web(preferences.get(FOCUS_COLOR, DEFAULT_MODENA_FOCUS_COLOR)));
        }

        _accentColor.set(colorToHex(accentColor.getValue()));
        _baseColor.set(colorToHex(baseColor.getValue()));
        _focusColor.set(colorToHex(focusColor.getValue()));
        _faintFocusColor.set(colorToHex(Color.web(colorToHex(focusColor.getValue()), OPACITY_FACTOR)));

        accentColor.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                preferences.put(ACCENT_COLOR, colorToHex(newValue));
                _accentColor.set(colorToHex(newValue));
            }
        });

        // Save the value when it changes
        baseColor.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                preferences.put(BASE_COLOR, colorToHex(newValue));
                _baseColor.set(colorToHex(newValue));

                controlTextFill.set(getBaseTextColor());
            }
        });

        focusColor.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                preferences.put(FOCUS_COLOR, colorToHex(newValue));
                _focusColor.set(colorToHex(newValue));
                _faintFocusColor.set(colorToHex(Color.web(colorToHex(focusColor.getValue()), OPACITY_FACTOR)));
            }
        });

        // Create the binding format for the style / font size
        styleProperty = Bindings.format(Locale.US, "-fx-font-size: %1$.6fem; -fx-base: %2$s; -fx-focus-color: %3$s; " +
                        "-fx-faint-focus-color: %4$s; -fx-accent: %5$s; -fx-selection-bar: %3$s",
                fontScale, _baseColor, _focusColor, _faintFocusColor,
                _accentColor);
    }

    private ThemeManager() {
        // Utility class
    }

    public static void applyStyleSheets(final Scene scene) {
        final String userTheme = preferences.get(USER_STYLE, null);

        if (userTheme != null && !userTheme.isBlank()) {
            scene.getStylesheets().addAll(ThemeManager.DEFAULT_CSS, userTheme);
        } else {
            scene.getStylesheets().addAll(ThemeManager.DEFAULT_CSS);
        }
    }

    public static void applyStyleSheets(final Parent parent) {
        final String userTheme = preferences.get(USER_STYLE, null);

        if (userTheme != null && !userTheme.isBlank()) {
            parent.getStylesheets().addAll(ThemeManager.DEFAULT_CSS, userTheme);
        } else {
            parent.getStylesheets().addAll(ThemeManager.DEFAULT_CSS);
        }
    }

    static Color getDefaultColor(final String theme, final byte colorIndex) {
        return Color.web(DEFAULT_COLORS[themeToIndex(theme)][colorIndex]);
    }

    private static byte themeToIndex(final String theme) {
        switch (theme) {
            case Application.STYLESHEET_CASPIAN:
                return CASPIAN;
            case Application.STYLESHEET_MODENA:
            default:
                return MODENA;
        }
    }

    public static void addKnownThemes(@NotNull final Menu menu) {
        Objects.requireNonNull(menu);

        themesMenu = menu;

        for (final String[] theme : KNOWN_THEMES) {
            final RadioMenuItem radioMenuItem = new RadioMenuItem(theme[0]);
            radioMenuItem.setUserData(theme[1]);
            radioMenuItem.setOnAction(themeHandler);
            radioMenuItem.setToggleGroup(toggleGroup);

            themesMenu.getItems().add(radioMenuItem);
        }

        JavaFXUtils.runLater(ThemeManager::syncRadioMenuItem);
    }

    private static void syncRadioMenuItem() {
        final String last = preferences.get(LAST, Application.STYLESHEET_MODENA);

        for (final MenuItem menuItem : themesMenu.getItems()) {
            if (menuItem.getUserData() != null && menuItem.getUserData().equals(last)) {
                ((RadioMenuItem) menuItem).setSelected(true);
                break;
            }
        }
    }

    public static void restoreLastUsedTheme() {
        Application.setUserAgentStylesheet(preferences.get(LAST, Application.STYLESHEET_MODENA));

        controlTextFill.setValue(getBaseTextColor());   // force an update after the stylesheet has been applied
    }

    public static boolean setUserStyle(@Nullable Path path) {

        boolean result = false;

        if (path != null && Files.exists(path)) {
            final String newValue = "file:///" + path.toString().replace("\\", "/");

            if (!preferences.get(USER_STYLE, "").equals(newValue)) {
                preferences.put(USER_STYLE, "file:///" + path.toString().replace("\\", "/"));

                applyStyleSheets(MainView.getPrimaryStage().getScene());    // apply style sheet

                result = true;
            }
        } else {
            if (preferences.get(USER_STYLE, null) != null) {
                preferences.remove(USER_STYLE);
                result = true;
            }
        }

        return result;
    }

    public static ObservableValue<String> styleProperty() {
        return styleProperty;
    }

    /**
     * Font scale property.  Always use a weak listener to prevent leaks
     *
     * @return current scale factor
     */
    public static DoubleProperty fontScaleProperty() {
        return fontScale;
    }

    static SimpleObjectProperty<Color> baseColorProperty() {
        return baseColor;
    }

    static SimpleObjectProperty<Color> focusColorProperty() {
        return focusColor;
    }

    static SimpleObjectProperty<Color> accentColorProperty() {
        return accentColor;
    }

    public static SimpleObjectProperty<Paint> controlTextFillProperty() {
        return controlTextFill;
    }

    static String getCurrentTheme() {
        return preferences.get(LAST, Application.STYLESHEET_MODENA);
    }

    /**
     * Utility method to discover the {@code Paint} used for {@code Button} text.
     *
     * @return Base Paint used for Buttons
     */
    private static Paint getBaseTextColor() {
        final Button button = new Button(BASE_COLOR);
        final Scene scene = new Scene(new Group(button));
        scene.getRoot().styleProperty().setValue(styleProperty().getValue());
        button.applyCss();
        return button.getTextFill();
    }

    /**
     * Utility method to discover the base font size in pixels.
     *
     * @return font size in pixels
     */
    public static double getBaseTextHeight() {
        final Text text = new Text();
        final Scene scene = new Scene(new Group(text));
        scene.getRoot().styleProperty().setValue(styleProperty().getValue());
        text.applyCss();

        return Math.ceil(text.getLayoutBounds().getHeight());
    }

    private static class ThemeHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(final ActionEvent event) {
            final MenuItem menuItem = (MenuItem) event.getSource();

            Application.setUserAgentStylesheet(menuItem.getUserData().toString());

            preferences.put(LAST, menuItem.getUserData().toString());

            final String current = preferences.get(LAST, null);

            if (current != null && !current.equals(menuItem.getUserData().toString())) {
                Application.setUserAgentStylesheet(menuItem.getUserData().toString());
            }
        }
    }

    private static String colorToHex(final Color color) {
        return String.format("#%02X%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255),
                (int) (color.getOpacity() * 255));
    }
}
