/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ChoiceDialog;

import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.views.main.MainView;
import jgnash.time.DateUtils;
import jgnash.util.ResourceUtils;

/**
 * UI Action to change the default locale.
 *
 * @author Craig Cavanaugh
 */
public class DefaultDateFormatAction {

    public static void showAndWait() {

        final Task<Set<String>> task = new Task<Set<String>>() {
            final ResourceBundle resources = ResourceUtils.getBundle();

            private Set<String> dateFormats;

            @Override
            protected Set<String> call() {
                dateFormats = DateUtils.getAvailableDateFormats();
                return dateFormats;
            }

            @Override
            protected void succeeded() {
                super.succeeded();

                Platform.runLater(() -> {
                    final ChoiceDialog<String> dialog = new ChoiceDialog<>(DateUtils.getShortDatePattern(), dateFormats);
                    dialog.setTitle(resources.getString("Title.SelDefDateFormat"));

                    dialog.getDialogPane().getStylesheets().addAll(MainView.DEFAULT_CSS);
                    dialog.getDialogPane().getScene().getRoot().styleProperty().bind(ThemeManager.styleProperty());
                    dialog.getDialogPane().getStyleClass().addAll("form", "dialog");
                    dialog.setHeaderText(resources.getString("Title.SelDefDateFormat"));

                    final Optional<String> optional = dialog.showAndWait();

                    optional.ifPresent(datePattern -> {
                        try {
                            DateUtils.setDateFormatPattern(datePattern);
                        } catch (IllegalArgumentException e) {
                            StaticUIMethods.displayError(resources.getString("Message.Error.InvalidDateFormat"));
                        }
                    });
                });
            }
        };

        new Thread(task).start();
    }
}
