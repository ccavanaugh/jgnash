/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
package jgnash.uifx.actions;

import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ChoiceDialog;

import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.views.main.MainView;
import jgnash.util.LocaleObject;
import jgnash.resource.util.ResourceUtils;

/**
 * UI Action to change the default locale.
 *
 * @author Craig Cavanaugh
 */
public class DefaultLocaleAction {

    public static void showAndWait() {

        final Task<LocaleObject[]> task = new Task<LocaleObject[]>() {
            final ResourceBundle resources = ResourceUtils.getBundle();

            private LocaleObject[] localeObjects;

            @Override
            protected LocaleObject[] call() {
                localeObjects = LocaleObject.getLocaleObjects();
                return localeObjects;
            }

            @Override
            protected void succeeded() {
                super.succeeded();

                Platform.runLater(() -> {
                    final ChoiceDialog<LocaleObject> dialog = new ChoiceDialog<>(new LocaleObject(Locale.getDefault()), localeObjects);
                    dialog.setTitle(resources.getString("Title.SelDefLocale"));

                    dialog.getDialogPane().getStylesheets().addAll(MainView.DEFAULT_CSS);
                    dialog.getDialogPane().getScene().getRoot().styleProperty().bind(ThemeManager.styleProperty());
                    dialog.getDialogPane().getStyleClass().addAll("form", "dialog");
                    dialog.setHeaderText(resources.getString("Title.SelDefLocale"));

                    final Optional<LocaleObject> optional = dialog.showAndWait();

                    optional.ifPresent(localeObject -> {
                        ResourceUtils.setLocale(localeObject.getLocale());
                        StaticUIMethods.displayMessage(localeObject + "\n" + resources.getString("Message.RestartLocale"));
                    });
                });
            }
        };

        new Thread(task).start();
    }
}
