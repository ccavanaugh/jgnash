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
package jgnash.uifx.skin;

import java.util.Objects;
import java.util.prefs.Preferences;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;

import jgnash.util.NotNull;

/**
 * Theme manager
 *
 * @author Craig Cavanaugh
 */
public class ThemeManager {

    private static final String LAST = "last";

    private static Menu themesMenu;

    private static ToggleGroup toggleGroup = new ToggleGroup();

    private static Handler handler = new Handler();

    private static String[][] KNOWN_THEMES = {
            {"Modena", Application.STYLESHEET_MODENA},
            {"Caspian", Application.STYLESHEET_CASPIAN}
    };

    private ThemeManager() {
        // Utility class
    }

    public static void addKnownThemes(@NotNull final Menu menu) {
        Objects.requireNonNull(menu);

        themesMenu = menu;

        for (final String[] theme : KNOWN_THEMES) {
            final RadioMenuItem radioMenuItem = new RadioMenuItem(theme[0]);
            radioMenuItem.setUserData(theme[1]);
            radioMenuItem.setOnAction(handler);
            radioMenuItem.setToggleGroup(toggleGroup);

            themesMenu.getItems().add(radioMenuItem);
        }

        Platform.runLater(ThemeManager::syncToggle);
    }

    public static void syncToggle() {
        final Preferences preferences = Preferences.userNodeForPackage(ThemeManager.class);
        final String last = preferences.get(LAST, Application.STYLESHEET_MODENA);

        for (final MenuItem menuItem : themesMenu.getItems()) {
            if (menuItem.getUserData() != null && menuItem.getUserData().equals(last)) {
                ((RadioMenuItem) menuItem).setSelected(true);
                break;
            }
        }
    }

    public static void restoreLastUsedTheme() {
        final Preferences preferences = Preferences.userNodeForPackage(ThemeManager.class);

        Application.setUserAgentStylesheet(preferences.get(LAST, Application.STYLESHEET_MODENA));
    }

    private static class Handler implements EventHandler<ActionEvent> {
        @Override
        public void handle(final ActionEvent event) {
            final MenuItem menuItem = (MenuItem) event.getSource();
            final Preferences preferences = Preferences.userNodeForPackage(ThemeManager.class);

            Application.setUserAgentStylesheet(menuItem.getUserData().toString());

            preferences.put(LAST, menuItem.getUserData().toString());

            final String current = preferences.get(LAST, null);

            if (current != null && !current.equals(menuItem.getUserData().toString())) {
                Application.setUserAgentStylesheet(menuItem.getUserData().toString());
            }
        }
    }
}
