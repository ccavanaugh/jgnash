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

import java.awt.EventQueue;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Transaction;
import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetPeriodDescriptor;
import jgnash.engine.budget.BudgetPeriodResults;
import jgnash.engine.budget.BudgetResultsModel;
import jgnash.message.ChannelEvent;
import jgnash.message.Message;
import jgnash.message.MessageListener;
import jgnash.message.MessageProperty;
import jgnash.message.MessageProxy;
import jgnash.util.Resource;

import static javax.swing.event.TableModelEvent.*;

/**
 * A display model for a budget period/descriptor.
 *
 * @author Craig Cavanaugh
 * @version $Id: BudgetPeriodModel.java 3154 2012-02-04 12:35:28Z ccavanaugh $
 */
public final class BudgetPeriodModel implements TableModel, MessageListener {

    private final transient Budget budget;

    private final transient BudgetPeriodDescriptor descriptor;

    /**
     * List of accounts that are budgeted against. It will add and remove accounts automatically
     */
    private transient ExpandingBudgetTableModel expandingBudgetTableModel;

    private transient BudgetResultsModel resultsModel;

    private transient TableModelListener expandingBudgetTableModelListener;

    /**
     * Message proxy
     */
    private MessageProxy proxy = new MessageProxy();

    /**
     * List of listeners
     */
    protected EventListenerList listenerList = new EventListenerList();

    private static final Logger logger = Logger.getLogger(BudgetPeriodModel.class.getName());

    private final String[] columnNames = {Resource.get().getString("Column.Budgeted"),
            Resource.get().getString("Column.Change"), Resource.get().getString("Column.Remaining")};

    static {
        logger.setLevel(Level.ALL);
    }

    public BudgetPeriodModel(final BudgetPeriodDescriptor descriptor, final ExpandingBudgetTableModel budgetTableModel) {
        if (descriptor == null || budgetTableModel == null) {
            throw new IllegalArgumentException();
        }

        this.expandingBudgetTableModel = budgetTableModel;

        this.budget = this.expandingBudgetTableModel.getBudget();

        this.descriptor = descriptor;

        resultsModel = this.expandingBudgetTableModel.getResultsModel();

        registerListeners();
    }

    private void registerListeners() {
        expandingBudgetTableModel.addMessageListener(this);

        // automatically forward table events
        expandingBudgetTableModelListener = new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent e) {
                fireTableChanged(e);
            }
        };

        expandingBudgetTableModel.addTableModelListener(expandingBudgetTableModelListener);
    }

    private void unregisterListeners() {
        expandingBudgetTableModel.removeMessageListener(this);
        expandingBudgetTableModel.removeTableModelListener(expandingBudgetTableModelListener);
    }

    @Override
    public int getRowCount() {
        return expandingBudgetTableModel.getRowCount();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(final int columnIndex) {
        return columnNames[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        return BigDecimal.class;
    }

    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        BudgetPeriodResults results = getResults(expandingBudgetTableModel.get(rowIndex));

        switch (columnIndex) {
            case 0:
                return results.getBudgeted();
            case 1:
                return results.getChange();
            case 2:
                return results.getRemaining();
            default:
                return BigDecimal.ZERO;
        }
    }

    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addTableModelListener(final TableModelListener listener) {
        listenerList.add(TableModelListener.class, listener);
    }

    @Override
    public void removeTableModelListener(final TableModelListener listener) {
        listenerList.remove(TableModelListener.class, listener);
    }

    public synchronized void addMessageListener(final MessageListener messageListener) {
        proxy.addMessageListener(messageListener);
    }

    public synchronized void removeMessageListener(final MessageListener messageListener) {
        proxy.removeMessageListener(messageListener);

        unregisterListeners();
    }

    public ExpandingBudgetTableModel getExpandingBudgetTableModel() {
        return expandingBudgetTableModel;
    }

    public BudgetPeriodResults getResults(final Account account) {
        if (account == null) {
            throw new IllegalArgumentException("account may not be null");
        }

        return resultsModel.getResults(descriptor, account);
    }

    public CurrencyNode getCurrency() {
        return resultsModel.getBaseCurrency();
    }

    private void fireUpdate(final Collection<Account> accounts) {
        for (Account account : accounts) {
            int index = expandingBudgetTableModel.indexOf(account);

            if (index >= 0) {
                fireTableRowsUpdated(index, index); // fire update to listeners
            }
        }
    }

    /**
     * Notifies all listeners that rows in the range
     * <code>[firstRow, lastRow]</code>, inclusive, have been updated.
     *
     * @param firstRow the first row
     * @param lastRow  the last row
     * @see TableModelEvent
     * @see EventListenerList
     */
    public void fireTableRowsUpdated(int firstRow, int lastRow) {
        fireTableChanged(new TableModelEvent(this, firstRow, lastRow, ALL_COLUMNS, UPDATE));
    }

    /**
     * Forwards the given notification event to all
     * <code>TableModelListeners</code> that registered
     * themselves as listeners for this table model.
     *
     * @param e the event to be forwarded
     * @see #addTableModelListener
     * @see javax.swing.event.TableModelEvent
     * @see EventListenerList
     */
    public void fireTableChanged(TableModelEvent e) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TableModelListener.class) {
                ((TableModelListener) listeners[i + 1]).tableChanged(e);
            }
        }
    }

    private void processTransactionEvent(final Message message) {
        final Transaction transaction = (Transaction) message.getObject(MessageProperty.TRANSACTION);

        if (isBetween(transaction.getDate())) { // don't update unless needed

            // build a list of accounts include ancestors that will be impacted by the transaction changes
            final Set<Account> accounts = new HashSet<Account>();

            for (Account account : transaction.getAccounts()) {
                accounts.addAll(account.getAncestors());
            }

            fireUpdate(accounts);
        }
    }

    private void updateBudget(final Message message) {
        Budget updatedBudget = (Budget) message.getObject(MessageProperty.BUDGET);

        if (updatedBudget.equals(budget)) {
            fireUpdate(getExpandingBudgetTableModel().getObjects());
        }
    }

    private void updateBudgetPeriod(final Message message) {
        Budget updatedBudget = (Budget) message.getObject(MessageProperty.BUDGET);

        if (updatedBudget.equals(budget)) {
            Account account = (Account) message.getObject(MessageProperty.ACCOUNT);
            fireUpdate(account.getAncestors());
        }
    }

    public BigDecimal getRemainingTotal(final AccountGroup group) {
        return resultsModel.getResults(descriptor, group).getRemaining();
    }

    public BigDecimal getChangeTotal(final AccountGroup group) {
        return resultsModel.getResults(descriptor, group).getChange();
    }

    public BigDecimal getBudgetedTotal(final AccountGroup group) {
        return resultsModel.getResults(descriptor, group).getBudgeted();
    }

    public String getPeriodDescription() {
        return descriptor.getPeriodDescription();
    }

    /**
     * @param date check date
     * @return true is date lies within this period
     * @see BudgetPeriodDescriptor#isBetween(java.util.Date)
     */
    public boolean isBetween(final Date date) {
        return descriptor.isBetween(date);
    }

    @Override
    public void messagePosted(final Message message) {

        if (message.getEvent() == ChannelEvent.FILE_CLOSING) {
            unregisterListeners();
            proxy.forwardMessage(message);
            return;
        }

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                switch (message.getEvent()) {
                    case ACCOUNT_ADD:
                    case ACCOUNT_REMOVE:
                    case ACCOUNT_MODIFY:
                        fireUpdate(expandingBudgetTableModel.getObjects());
                        break;
                    case BUDGET_UPDATE:
                        updateBudget(message);
                        break;
                    case BUDGET_GOAL_UPDATE:
                        updateBudgetPeriod(message);
                        break;
                    case BUDGET_REMOVE:
                        if (message.getObject(MessageProperty.BUDGET).equals(budget)) {
                            unregisterListeners();
                        }
                        break;
                    case TRANSACTION_ADD:
                    case TRANSACTION_REMOVE:
                        processTransactionEvent(message);
                        break;
                    default:
                        break;
                }

                proxy.forwardMessage(message);
            }
        });
    }
}
