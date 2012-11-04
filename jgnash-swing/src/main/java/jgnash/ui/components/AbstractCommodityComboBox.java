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

import java.util.Collection;

import javax.swing.JComboBox;

import jgnash.engine.CommodityNode;
import jgnash.message.MessageListener;

/**
 * Abstract JComboBox for listing available commodities
 *
 * @author Craig Cavanaugh
 */
public abstract class AbstractCommodityComboBox<T extends CommodityNode> extends JComboBox<T> implements MessageListener {

    final SortedComboBoxModel<T> model = new SortedComboBoxModel<>();

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
            int size = model.getSize();
            T tNode;
            for (int i = 0; i < size; i++) {
                tNode = model.getElementAt(i);
                if (node.matches(tNode)) {
                    setSelectedIndex(i);
                    return; // exit the loop early
                }
            }
        }
    }

    void updateNode(T node) {
        model.updateElement(node);
    }
}
