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
package jgnash.ui.components;

import java.awt.EventQueue;
import java.util.List;
import java.util.Objects;

import jgnash.engine.CommodityNode;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageProperty;

/**
 * Currency ComboBox selector
 *
 * @author Craig Cavanaugh
 */
public class CurrencyComboBox extends AbstractCommodityComboBox<CurrencyNode> {

    public CurrencyComboBox() {
        super();

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        // Model loading and setting of default was being done in the background.  If an immediate set was performed,
        // the background operation would override.
        final List<CurrencyNode> nodeList = engine.getCurrencies();
        final CurrencyNode defaultCurrency = engine.getDefaultCurrency();

        model.addAll(nodeList);
        setSelectedNode(defaultCurrency);

        MessageBus.getInstance().registerListener(this, MessageChannel.COMMODITY);
    }

//    /**
//     * This constructor does not listen to engine events because the source of the objects is not known
//     *
//     * @param list CurrencyNode list
//     */
//    public CurrencyComboBox(Collection<CurrencyNode> list) {
//        super();
//        model.addAll(list);
//    }

    @Override
    public void messagePosted(final Message event) {
        final CommodityNode node = event.getObject(MessageProperty.COMMODITY);

        if (node instanceof CurrencyNode) {

            EventQueue.invokeLater(() -> {
                switch (event.getEvent()) {
                    case CURRENCY_REMOVE:
                        model.removeElement(node);
                        break;
                    case CURRENCY_ADD:
                        model.addElement((CurrencyNode) node);
                        break;
                    case CURRENCY_MODIFY:
                        updateNode((CurrencyNode) node);
                        break;
                    default:
                        break;
                }

            });
        }
    }
}
