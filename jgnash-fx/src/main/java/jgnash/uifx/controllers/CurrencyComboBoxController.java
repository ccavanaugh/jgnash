/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.uifx.controllers;

import java.util.List;

import jgnash.engine.CurrencyNode;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.ComboBox;

/**
 * ComboBox controller for currency selection
 *
 * @author Craig Cavanaugh
 */
public class CurrencyComboBoxController implements MessageListener {

    /** Model for the ComboBox */
    private ObservableList<CurrencyNode> items;

    public CurrencyComboBoxController(ComboBox<CurrencyNode> comboBox) {

        final List<CurrencyNode> nodeList = EngineFactory.getEngine(EngineFactory.DEFAULT).getCurrencies();
        final CurrencyNode defaultCurrency = EngineFactory.getEngine(EngineFactory.DEFAULT).getDefaultCurrency();

        // extract and reuse the default model
        items = comboBox.getItems();

        // warp in a sorted list
        comboBox.setItems(new SortedList<>(items, null));

        items.addAll(nodeList);
        comboBox.setValue(defaultCurrency);

        MessageBus.getInstance().registerListener(this, MessageChannel.COMMODITY);
    }

    @Override
    public void messagePosted(final Message event) {
        if (event.getObject(MessageProperty.COMMODITY) instanceof CurrencyNode) {

            final CurrencyNode node = (CurrencyNode) event.getObject(MessageProperty.COMMODITY);

            Platform.runLater(() -> {
                switch (event.getEvent()) {
                    case CURRENCY_REMOVE:
                        items.removeAll(node);
                        break;
                    case CURRENCY_ADD:
                        items.add(node);
                        break;
                    case CURRENCY_MODIFY:
                        items.removeAll(node);
                        items.add(node);
                        break;
                    default:
                        break;
                }
            });
        }
    }
}
