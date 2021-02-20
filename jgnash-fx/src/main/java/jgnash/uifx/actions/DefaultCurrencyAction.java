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
package jgnash.uifx.actions;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.concurrent.Task;

import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.ChoiceDialog;
import jgnash.uifx.util.JavaFXUtils;

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

                JavaFXUtils.runLater(() -> {

                    final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                    Objects.requireNonNull(engine);

                    final ChoiceDialog<CurrencyNode> dialog = new ChoiceDialog<>(engine.getDefaultCurrency(), currencyNodeList);
                    dialog.setTitle(resources.getString("Title.SelDefCurr"));
                    dialog.setContentText(resources.getString("Title.SelDefCurr"));

                    final Optional<CurrencyNode> optional = dialog.showAndWait();

                    optional.ifPresent(currencyNode -> {
                        engine.setDefaultCurrency(currencyNode);

                        JavaFXUtils.runLater(() -> StaticUIMethods.displayMessage(resources.getString("Message.CurrChange")
                                + " " + engine.getDefaultCurrency().getSymbol()));

                    });
                });
            }
        };

        new Thread(task).start();
    }
}
