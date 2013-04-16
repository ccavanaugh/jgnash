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

import java.awt.*;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;

import jgnash.engine.CommodityNode;
import jgnash.message.MessageListener;
import jgnash.util.DefaultDaemonThreadFactory;

/**
 * Abstract JComboBox for listing available commodities
 *
 * @author Craig Cavanaugh
 */
public abstract class AbstractCommodityComboBox<T extends CommodityNode> extends JComboBox<T> implements MessageListener {

    final SortedComboBoxModel<T> model = new SortedComboBoxModel<>();

    private final ExecutorService threadPool = Executors.newSingleThreadExecutor(new DefaultDaemonThreadFactory());

    @SuppressWarnings("unchecked")
	AbstractCommodityComboBox() {
        super();
        setModel(model);
        super.setEditable(false);
    }

    void addAll(Collection<T> items) {
        model.addAll(items);
    }

    @Override
    public void setEditable(boolean aFlag) {
    }

    @SuppressWarnings("unchecked")
    public T getSelectedNode() {
        T node = (T) getSelectedItem();
        if (node != null) {
            return node;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void setSelectedNode(final T node) {
        if (node == null) {
            setSelectedIndex(-1);
        } else {

            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    int size = model.getSize();

                    for (int i = 0; i < size; i++) {
                        if (node.matches(model.getElementAt(i))) {
                            final T tNode = model.getElementAt(i);

                            EventQueue.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    setSelectedItem(tNode);
                                }
                            });

                            break;
                        }
                    }
                }
            });
        }
    }

    void updateNode(T node) {
        model.updateElement(node);
    }
}
