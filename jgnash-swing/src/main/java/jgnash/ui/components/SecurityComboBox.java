/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import jgnash.engine.CommodityNode;
import jgnash.engine.EngineFactory;
import jgnash.engine.SecurityNode;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageProperty;

/**
 * SecurityNode ComboBox
 *
 * @author Craig Cavanaugh
 *
 */
public class SecurityComboBox extends AbstractCommodityComboBox<SecurityNode> {

    public SecurityComboBox() {
        super();

        SwingWorker<List<SecurityNode>, Void> worker = new SwingWorker<List<SecurityNode>, Void>() {

            @Override
            public List<SecurityNode> doInBackground() {
                return EngineFactory.getEngine(EngineFactory.DEFAULT).getSecurities();
            }

            @Override
            public void done() {
                try {
                    model.addAll(get());

                    if (model.getSize() > 0) {
                        SecurityComboBox.this.setSelectedIndex(0);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    Logger.getLogger(SecurityComboBox.class.getName()).severe(e.getLocalizedMessage());
                }
            }
        };

        worker.execute();

        MessageBus.getInstance().registerListener(this, MessageChannel.COMMODITY);
    }

    public SecurityNode getSelectedSecurityNode() {
        return (SecurityNode) getSelectedItem();
    }

    @Override
    public void messagePosted(final Message event) {
        final CommodityNode node = (CommodityNode) event.getObject(MessageProperty.COMMODITY);

        if (node instanceof SecurityNode) {

            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    switch (event.getEvent()) {
                        case SECURITY_ADD:
                            model.addElement((SecurityNode) node);
                            return;
                        case SECURITY_REMOVE:
                            model.removeElement(node);
                            return;
                        case SECURITY_MODIFY:
                            updateNode((SecurityNode) node);
                            return;
                        default:
                    }

                }
            });
        }
    }
}
