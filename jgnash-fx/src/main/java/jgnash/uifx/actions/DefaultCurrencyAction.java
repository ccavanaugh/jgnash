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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ChoiceDialog;

import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.views.main.MainView;
import jgnash.resource.util.ResourceUtils;

/**
 * UI Action to change the default currency.
 *
 * @author Craig Cavanaugh
 */
public class DefaultCurrencyAction {

    public static void showAndWait() {

        final Task<List<CurrencyNode>> task = new Task<>() {
            final ResourceBundle resources = ResourceUtils.getBundle();

            private List<CurrencyNode> currencyNodeList;

            @Override
            protected List<CurrencyNode> call() {

                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                Objects.requireNonNull(engine);

                currencyNodeList = engine.getCurrencies();

                return currencyNodeList;
            }

            @Override
            protected void succeeded() {
                super.succeeded();

                Platform.runLater(() -> {

                    final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                    Objects.requireNonNull(engine);

                    final ChoiceDialog<CurrencyNode> dialog = new ChoiceDialog<>(engine.getDefaultCurrency(), currencyNodeList);
                    dialog.setTitle(resources.getString("Title.SelDefCurr"));

                    dialog.getDialogPane().getStylesheets().addAll(MainView.DEFAULT_CSS);
                    dialog.getDialogPane().getScene().getRoot().styleProperty().bind(ThemeManager.styleProperty());
                    dialog.getDialogPane().getStyleClass().addAll("form", "dialog");
                    dialog.setHeaderText(resources.getString("Title.SelDefCurr"));

                    final Optional<CurrencyNode> optional = dialog.showAndWait();

                    optional.ifPresent(currencyNode -> {
                        engine.setDefaultCurrency(currencyNode);

                        Platform.runLater(() -> StaticUIMethods.displayMessage(resources.getString("Message.CurrChange")
                                + " " + engine.getDefaultCurrency().getSymbol()));

                    });
                });
            }
        };

        new Thread(task).start();
    }
}
