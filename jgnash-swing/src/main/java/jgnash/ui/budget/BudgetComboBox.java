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
package jgnash.ui.budget;

import java.awt.Component;
import java.awt.EventQueue;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.budget.Budget;
import jgnash.message.Message;
import jgnash.message.MessageBus;
import jgnash.message.MessageChannel;
import jgnash.message.MessageListener;
import jgnash.message.MessageProperty;
import jgnash.ui.components.SortedComboBoxModel;
import jgnash.util.Resource;

/**
 * ComboBox for displaying a list of Budgets. Automatically refreshes itself when necessary
 *
 * @author Craig Cavanaugh
 *
 */
public final class BudgetComboBox extends JComboBox<Budget> {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("CallToThreadStartDuringObjectConstruction")
    public BudgetComboBox() {
        super();

        setModel(new BudgetModel());
        setRenderer(new Renderer(getRenderer()));

        // lazily create a prototype value so UI will size correctly
        Thread thread = new Thread() {

            @Override
            public void run() {

                Resource rb = Resource.get();

                Budget prototype = new Budget();
                prototype.setName(rb.getString("Word.NewBudget") + " " + 1);

                setPrototypeDisplayValue(prototype);
            }
        };

        thread.start();

        if (getModel().getSize() > 0) {
            setSelectedIndex(0);
        }
    }

    /**
     * Returns the selected Budget
     *
     * @return the Budget
     */
    public Budget getSelectedBudget() {
        return (Budget) getSelectedItem();
    }

    /**
     * Sets the selected Budget
     *
     * @param budget Budget to select
     */
    public void setSelectedBudget(final Budget budget) {
        super.setSelectedItem(budget);
    }

    protected static final class BudgetModel extends SortedComboBoxModel<Budget> implements MessageListener {

        private static final long serialVersionUID = 1L;

        public BudgetModel() {
            super();

            Engine e = EngineFactory.getEngine(EngineFactory.DEFAULT);

            addAll(e.getBudgetList());

            MessageBus.getInstance().registerListener(BudgetModel.this, MessageChannel.BUDGET, MessageChannel.SYSTEM);
        }

        // Model update must not be pushed to the EDT to maintain synchronous behavior.
        // The view already makes a call to this method from the EDT
        @Override
        public void messagePosted(final Message event) {

            switch (event.getEvent()) {
                case FILE_CLOSING:
                    MessageBus.getInstance().unregisterListener(BudgetModel.this, MessageChannel.BUDGET, MessageChannel.SYSTEM);
                    break;
                case BUDGET_REMOVE:
                    if (event.getObject(MessageProperty.BUDGET) != null) {
                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                Object selected = getSelectedItem();

                                removeElement(event.getObject(MessageProperty.BUDGET));

                                if (selected == event.getObject(MessageProperty.BUDGET) && getSize() > 0) {
                                    setSelectedItem(getElementAt(0));
                                }
                            }
                        });
                    }
                    break;
                case BUDGET_ADD:
                    if (event.getObject(MessageProperty.BUDGET) != null) {
                        EventQueue.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                addElement((Budget) event.getObject(MessageProperty.BUDGET));

                                if (getSize() == 1) {
                                    setSelectedItem(event.getObject(MessageProperty.BUDGET));
                                }
                            }
                        });
                    }
                    break;
                case BUDGET_UPDATE:
                    final Object selected = getSelectedItem();
                    
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            

                            fireContentsChanged(this, 0, getSize());

                            if (selected != null) {
                                setSelectedItem(selected);
                            }
                        }
                    });

                    break;
                default: // ignore any other messages that don't belong to us
                    break;

            }
        }
    }

    /**
     * ComboBox renderer Display a specified text when the ComboBox is disabled
     */
    private class Renderer implements ListCellRenderer<Budget> {

        private final ListCellRenderer<? super Budget> delegate;

        public Renderer(final ListCellRenderer<? super Budget> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getListCellRendererComponent(final JList<? extends Budget> list, final Budget value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            if (value != null) {
                BudgetComboBox.this.setToolTipText(value.getDescription());
                
                Component c = delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                if (c instanceof JLabel) {
                	((JLabel)c).setText(value.getName());
                }
                
                return c;
            }
            return delegate.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus);
        }
    }
}
