/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import jgnash.engine.CommodityNode;
import jgnash.engine.CurrencyNode;
import jgnash.engine.EngineFactory;
import jgnash.message.Message;
import jgnash.message.MessageBus;
import jgnash.message.MessageChannel;
import jgnash.message.MessageProperty;

/**
 * Currency ComboBox selector
 *
 * @author Craig Cavanaugh
 *
 */
public class CurrencyComboBox extends AbstractCommodityComboBox<CurrencyNode> {

    public CurrencyComboBox() {
        super();

        SwingWorker<List<CurrencyNode>, Void> worker = new SwingWorker<List<CurrencyNode>, Void>() {

            CurrencyNode defaultNode;

            @Override
            public List<CurrencyNode> doInBackground() {
                defaultNode = EngineFactory.getEngine(EngineFactory.DEFAULT).getDefaultCurrency();
                return EngineFactory.getEngine(EngineFactory.DEFAULT).getCurrencies();
            }

            @Override
            public void done() {
                try {
                    model.addAll(get());
                    CurrencyComboBox.this.setSelectedNode(defaultNode);
                } catch (InterruptedException | ExecutionException e) {
                    Logger.getLogger(CurrencyComboBox.class.getName()).severe(e.getLocalizedMessage());
                }
            }
        };

        worker.execute();

        MessageBus.getInstance().registerListener(this, MessageChannel.COMMODITY);
    }

    /**
     * This constructor does not listen to engine events because the source of the objects is not known
     *
     * @param list CurrencyNode list
     */
    public CurrencyComboBox(Collection<CurrencyNode> list) {
        super();
        model.addAll(list);
    }

    @Override
    public void messagePosted(final Message event) {
        final CommodityNode node = (CommodityNode) event.getObject(MessageProperty.COMMODITY);

        if (node instanceof CurrencyNode) {

            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
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

                }
            });
        }
    }
}
