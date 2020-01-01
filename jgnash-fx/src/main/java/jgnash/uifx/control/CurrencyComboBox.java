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
package jgnash.uifx.control;

import java.util.List;
import java.util.Objects;

import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.ComboBox;

import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.uifx.util.JavaFXUtils;

/**
 * ComboBox that allows selection of a CurrencyNode and manages it's own model.
 *
 * @author Craig Cavanaugh
 */
public class CurrencyComboBox extends ComboBox<CurrencyNode> implements MessageListener{

    /** Model for the ComboBox. */
    private ObservableList<CurrencyNode> items;

    public CurrencyComboBox() {
        JavaFXUtils.runLater(this::loadModel); // lazy load to let the ui build happen faster
    }

    private void loadModel() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final List<CurrencyNode> nodeList = engine.getCurrencies();

        // extract and reuse the default model
        items = getItems();

        // warp in a sorted list
        setItems(new SortedList<>(items, null));

        items.addAll(nodeList);

        final CurrencyNode defaultCurrency = engine.getDefaultCurrency();
        setValue(defaultCurrency);

        MessageBus.getInstance().registerListener(this, MessageChannel.COMMODITY, MessageChannel.SYSTEM);
    }

    @Override
    public void messagePosted(final Message event) {
        if (event.getObject(MessageProperty.COMMODITY) instanceof CurrencyNode) {

            final CurrencyNode node = event.getObject(MessageProperty.COMMODITY);

            JavaFXUtils.runLater(() -> {
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
                    case FILE_CLOSING:
                        items.clear();
                    default:
                        break;
                }
            });
        }
    }
}
