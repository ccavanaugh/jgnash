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
package jgnash.ui.account;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.ChannelEvent;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.text.CommodityFormat;
import jgnash.ui.components.expandingtable.AbstractExpandingTableModel;
import jgnash.ui.components.expandingtable.ExpandingTableNode;
import jgnash.ui.register.AccountBalanceDisplayManager;
import jgnash.util.Resource;

/**
 * TableModel that can expand and contract the displayed rows
 * 
 * @author Craig Cavanaugh
 * @author Peter Vida
 *
 */
public final class ExpandingAccountTableModel extends AbstractExpandingTableModel<Account> implements AccountFilterModel {

    private boolean incomeVisible = true;

    private boolean expenseVisible = true;

    private boolean accountVisible = true;

    private boolean hiddenVisible = true;

    private final transient String[] columnNames;

    private final transient Class<?>[] columnTypes;

    private static final Logger logger = Logger.getLogger(ExpandingAccountTableModel.class.getName());

    private final transient CommodityFormat formatter = CommodityFormat.getFullFormat();

    private final transient Preferences p = Preferences.userNodeForPackage(ExpandingAccountTableModel.class);

    private static final String HIDDEN_VISIBLE = "HiddenVisible";

    private static final String EXPENSE_VISIBLE = "ExpenseVisible";

    private static final String INCOME_VISIBLE = "IncomeVisible";

    private static final String ACCOUNT_VISIBLE = "AccountVisible";

    private final transient MessageBusListener messageListener = new MessageBusListener();

    public ExpandingAccountTableModel() {
        logger.setLevel(Level.ALL);

        Resource rb = Resource.get();

        columnNames = new String[] { rb.getString("Column.AccountName"), rb.getString("Column.Entries"),
                        rb.getString("Column.Balance"), rb.getString("Column.ReconciledBalance"),
                        rb.getString("Column.Currency"), rb.getString("Column.Type") };
        columnTypes = new Class<?>[] { String.class, String.class, BigDecimal.class, BigDecimal.class, String.class,
                        String.class };

        MessageBus.getInstance().registerListener(messageListener, MessageChannel.ACCOUNT, MessageChannel.COMMODITY, MessageChannel.SYSTEM);

        AccountBalanceDisplayManager.addAccountBalanceDisplayModeChangeListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                fireTableDataChanged();
            }
        });

        restorePreferences();
    }

    @Override
    public int getVisibleDepth(final Account object) {
        return object.getDepth();
    }

    @Override
    public boolean isParent(final Account object) {
        return object.isParent();
    }

    @Override
    public Collection<Account> getChildren(final Account object) {
        return object.getChildren();
    }

    @Override
    public boolean isVisible(final Account object) {
        if (super.isVisible(object)) {
            AccountType type = object.getAccountType();
            if (type == AccountType.INCOME && isIncomeVisible()) {
                if (!object.isVisible() && isHiddenVisible() || object.isVisible()) {
                    return true;
                }
            } else if (type == AccountType.EXPENSE && isExpenseVisible()) {
                if (!object.isVisible() && isHiddenVisible() || object.isVisible()) {
                    return true;
                }
            } else if (type != AccountType.INCOME && type != AccountType.EXPENSE && isAccountVisible()) {
                if (!object.isVisible() && isHiddenVisible() || object.isVisible()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(final int columnIndex) {
        return columnNames[columnIndex];
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        ReadLock readLock = rwl.readLock();

        try {
            readLock.lock();

            ExpandingTableNode<Account> node = getExpandingTableNodeAt(rowIndex);

            if (node == null || node.getObject() == null) {
                logger.log(Level.WARNING, "Null data", new Exception());
                return "";
            }

            Account account = node.getObject();

            switch (columnIndex) {
                case 0:
                    return account.getName();
                case 1:
                    return account.getTransactionCount();
                case 2:
                    BigDecimal balance = AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), account.getTreeBalance());
                    return formatter.format(balance, account.getCurrencyNode());
                case 3:
                    BigDecimal reconciledBalance = AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), account.getReconciledTreeBalance());
                    return formatter.format(reconciledBalance, account.getCurrencyNode());
                case 4:
                    return account.getCurrencyNode().getSymbol();
                case 5:
                    return account.getAccountType().toString();
                default:
                    return "Error";
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    protected Collection<Account> getModelObjects() {
        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        return engine.getAccountList();
    }

    @Override
    protected Account getRootObject() {
        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        return engine.getRootAccount();
    }

    @Override
    public String getSearchString(final Account object) {
        return object.getName();
    }

    private void restorePreferences() {
        setAccountVisible(p.getBoolean(ACCOUNT_VISIBLE, true));
        setExpenseVisible(p.getBoolean(EXPENSE_VISIBLE, true));
        setHiddenVisible(p.getBoolean(HIDDEN_VISIBLE, true));
        setIncomeVisible(p.getBoolean(INCOME_VISIBLE, true));
    }

    @Override
    public synchronized void setAccountVisible(final boolean visible) {
        if (accountVisible != visible) {
            p.putBoolean(ACCOUNT_VISIBLE, visible);
            accountVisible = visible;
            fireNodeChanged();
        }
    }

    @Override
    public synchronized void setExpenseVisible(final boolean visible) {
        if (expenseVisible != visible) {
            p.putBoolean(EXPENSE_VISIBLE, visible);
            expenseVisible = visible;
            fireNodeChanged();
        }
    }

    @Override
    public synchronized void setHiddenVisible(final boolean visible) {
        if (hiddenVisible != visible) {
            p.putBoolean(HIDDEN_VISIBLE, visible);
            hiddenVisible = visible;
            fireNodeChanged();
        }
    }

    @Override
    public synchronized void setIncomeVisible(final boolean visible) {
        if (incomeVisible != visible) {
            p.putBoolean(INCOME_VISIBLE, visible);
            incomeVisible = visible;
            fireNodeChanged();
        }
    }

    @Override
    public synchronized boolean isAccountVisible() {
        return accountVisible;
    }

    @Override
    public synchronized boolean isExpenseVisible() {
        return expenseVisible;
    }

    @Override
    public synchronized boolean isHiddenVisible() {
        return hiddenVisible;
    }

    @Override
    public synchronized boolean isIncomeVisible() {
        return incomeVisible;
    }

    private void unregister() {
        MessageBus.getInstance().unregisterListener(messageListener, MessageChannel.ACCOUNT, MessageChannel.COMMODITY, MessageChannel.SYSTEM);
    }

    @Override
    public Account getParent(final Account object) {
        return object.getParent();
    }

    private class MessageBusListener implements MessageListener {

        @Override
        public void messagePosted(final Message event) {
            if (event.getEvent() == ChannelEvent.FILE_CLOSING) {
                unregister();
                return;
            }

            if (EngineFactory.getEngine(EngineFactory.DEFAULT) == null) {
                return;
            }

            final Account a = (Account) event.getObject(MessageProperty.ACCOUNT);

            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    switch (event.getEvent()) {
                        case ACCOUNT_ADD:
                            addNode(a);
                            break;
                        case ACCOUNT_REMOVE:
                            removeNode(a);
                            break;
                        case ACCOUNT_MODIFY:
                        case ACCOUNT_VISIBILITY_CHANGE:
                        case SECURITY_HISTORY_ADD:
                        case SECURITY_HISTORY_REMOVE:
                            fireNodeChanged();
                            break;
                        case FILE_LOAD_SUCCESS:
                        case FILE_NEW_SUCCESS:
                            logger.warning("Should not have received a load and new file notification");
                            break;
                        default: // ignore any other messages that don't belong to us
                            break;
                    }
                }
            });
        }
    }
}
