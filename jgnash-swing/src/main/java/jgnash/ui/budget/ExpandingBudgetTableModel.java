/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Comparators;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetResultsModel;
import jgnash.engine.message.ChannelEvent;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.engine.message.MessageProxy;
import jgnash.ui.components.expandingtable.AbstractExpandingTableModel;
import jgnash.util.ResourceUtils;

/**
 * TableModel that can expand and contract the displayed rows
 *
 * @author Craig Cavanaugh
 */
public class ExpandingBudgetTableModel extends AbstractExpandingTableModel<Account> implements MessageListener {

    private static final String COLUMN_NAME = ResourceUtils.getString("Column.Account");

    private final MessageProxy proxy = new MessageProxy();

    private final Budget budget;

    private final BudgetResultsModel model;

    public ExpandingBudgetTableModel(final BudgetResultsModel model) {
        this.budget = model.getBudget();
        this.model = model;

        initializeModel();
        registerListeners();
    }

    public BudgetResultsModel getResultsModel() {
        return model;
    }

    /**
     * Gets the budget assigned to this model
     *
     * @return budget assigned to this model
     */
    public Budget getBudget() {
        return budget;
    }

    /**
     * Exposes message events from the Account, Budget, and System channels
     *
     * @param messageListener new listener to add
     */
    public final synchronized void addMessageListener(final MessageListener messageListener) {
        proxy.addMessageListener(messageListener);
    }

    public final synchronized void removeMessageListener(final MessageListener messageListener) {
        proxy.removeMessageListener(messageListener);
    }

    private void registerListeners() {
        model.addMessageListener(this);
    }

    private void unregisterListeners() {
        model.removeMessageListener(this);
    }

    private void addAccount(final Account account) {
        rwl.writeLock().lock();

        try {
            if (includeAccount(account)) {
                addNode(account);
            }
        } finally {
            rwl.writeLock().unlock();
        }
    }

    /**
     * Determines is the account will be added to the model
     *
     * @param account Account to test
     * @return true if it should be added
     */
    private boolean includeAccount(final Account account) {
        // account must be visible and not marked for exclusion from a budget
        return model.includeAccount(account);
    }

    private void removeAccount(final Account account) {
        rwl.writeLock().lock();

        try {
            removeNode(account);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    private void updateAccountStructure() {
        rwl.writeLock().lock();

        try {
            initializeModel(); // results are indeterminate, so force a reload
        } finally {
            rwl.writeLock().unlock();
        }
    }

    /**
     * Returns a sorted list of account groups in this model.
     *
     * @return a sorted list of account groups
     */
    public final List<AccountGroup> getAccountGroups() {
        return model.getAccountGroupList();
    }

    /**
     * @see AbstractExpandingTableModel#getVisibleDepth(Comparable)
     */
    @Override
    public int getVisibleDepth(final Account account) {

        int depth = 0;

        Account parent = account.getParent();
        while (parent != null) {
            if (!parent.isExcludedFromBudget()) {
                depth++;
            }
            parent = parent.getParent();
        }

        return depth;
    }

    @Override
    public boolean isParent(final Account account) {
        return account.isParent();
    }

    @Override
    public Collection<Account> getChildren(final Account account) {
        return account.getChildren(Comparators.getAccountByCode());
    }

    /**
     * @see jgnash.ui.components.expandingtable.AbstractExpandingTableModel#getModelObjects()
     */
    @Override
    protected Collection<Account> getModelObjects() {
        return model.getAccounts();
    }

    @Override
    public String getSearchString(final Account object) {
        return object.getName();
    }

    @Override
    protected Account getRootObject() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        return engine.getRootAccount();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        return get(rowIndex).getName();
    }

    @Override
    public String getColumnName(final int column) {
        return COLUMN_NAME;
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        return String.class;
    }

    @Override
    public Account getParent(final Account object) {
        return object.getParent();
    }

    @Override
    public final void messagePosted(final Message message) {

        if (message.getEvent() == ChannelEvent.FILE_CLOSING) {
            clear();
            unregisterListeners();
            proxy.forwardMessage(message);
            return;
        }

        // must push update onto the EDT for the view to update correctly
        EventQueue.invokeLater(() -> {
            switch (message.getEvent()) {
                case ACCOUNT_ADD:
                    final Account addAccount = message.getObject(MessageProperty.ACCOUNT);
                    addAccount(addAccount);
                    break;
                case ACCOUNT_REMOVE:
                    final Account removeAccount = message.getObject(MessageProperty.ACCOUNT);
                    removeAccount(removeAccount);
                    break;
                case ACCOUNT_MODIFY:
                    updateAccountStructure();
                    break;
                case BUDGET_UPDATE:
                    if (message.getObject(MessageProperty.BUDGET).equals(budget)) {
                        updateAccountStructure();
                    }
                    break;
                default:
                    break;
            }

            proxy.forwardMessage(message); // pass through all messages
        });
    }
}
