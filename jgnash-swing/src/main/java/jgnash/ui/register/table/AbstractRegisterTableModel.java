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
package jgnash.ui.register.table;

import java.awt.EventQueue;
import java.math.BigDecimal;
import java.util.Arrays;

import javax.swing.table.AbstractTableModel;

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.message.ChannelEvent;
import jgnash.message.Message;
import jgnash.message.MessageBus;
import jgnash.message.MessageChannel;
import jgnash.message.MessageListener;
import jgnash.message.MessageProperty;
import jgnash.ui.register.AccountBalanceDisplayManager;
import jgnash.util.BigDecimalCache;
import jgnash.util.Resource;

/**
 * By default, the transactions are returned in the natural order of the account.
 * <p/>
 * Column order will be handled by JTable. This model is responsible for handling column visibility and saving/restoring
 * user widths.
 * <p/>
 * This model does some internal manipulation to make it possible to hide specific columns.
 *
 * @author Craig Cavanaugh
 * @version $Id: AbstractRegisterTableModel.java 3129 2012-01-22 21:51:05Z ccavanaugh $
 */
public abstract class AbstractRegisterTableModel extends AbstractTableModel implements MessageListener, AccountTableModel, PackableTableModel {

    private static final long serialVersionUID = 1545319678064522849L;

    static final Resource rb = Resource.get();

    Account account;

    BigDecimalCache balanceCache;

    private String cNames[];

    private Class<?> cClass[];

    String reconcileSymbol = RECONCILED_SYM;

    final static String ERROR = "error";

    /**
     * the visibility state of a column
     */
    boolean[] columnVisible;

    private int[] columnMap;

    /**
     * Recalculate the visible column count
     */
    private boolean columnCountCache = false;

    /**
     * The cached visible column count
     */
    private int columnCount;

    private int[] columnWidths = null;

    AbstractRegisterTableModel(final Account account, final String[] names, final Class<?>[] clazz) {
        assert account != null;
        init(account, names, clazz);
    }

    /* Width of 0 - minimal width
     * Width 1 to 100 - weighted column width, result is a percentage of total
     * columns that are not specified as a minimal width
     */
    @Override
    public int[] getPreferredColumnWeights() {
        return columnWidths.clone(); // return a clone to prevent accidental change
    }

    public void setPreferredColumnWeights(final int[] widths) {
        columnWidths = widths.clone();
    }

    private void init(final Account a, final String[] names, final Class<?>[] clazz) {
        cNames = Arrays.copyOf(names, names.length);
        cClass = Arrays.copyOf(clazz, clazz.length);

        account = a;

        balanceCache = new BigDecimalCache(a.getTransactionCount());

        MessageBus.getInstance().registerListener(this, MessageChannel.COMMODITY, MessageChannel.TRANSACTION, MessageChannel.SYSTEM);

        /* set up the visibility array */
        columnVisible = new boolean[cNames.length];
        Arrays.fill(columnVisible, true);

        /* build the columnMap */
        buildColumnMap();
    }

    @Override
    public Account getAccount() {
        return account;
    }

    /**
     * Returns the name of the columns
     *
     * @return An array of Strings for the names of the columns
     */
    public String[] getColumnNames() {
        return cNames;
    }

    protected abstract Object getInternalValueAt(int row, int col);

    /**
     * Changes the visibility of a column and notifies the JTable
     *
     * @param col     The column to change the visibility of
     * @param visible True if visible
     */
    private void setColumnVisible(final int col, final boolean visible) {
        if (visible != columnVisible[col]) {
            columnVisible[col] = visible;
            buildColumnMap();
            fireTableStructureChanged();
        }
    }

    /**
     * Changes the visibility of a column and notifies the JTable
     *
     * @param name    The column name to change the visibility of
     * @param visible True if visible
     */
    public void setColumnVisible(final String name, final boolean visible) {
        for (int i = 0; i < cNames.length; i++) {
            if (cNames[i].equals(name)) {
                setColumnVisible(i, visible);
            }
        }
    }

    public void setColumnVisibility(final boolean[] array) {
        if (array != null && array.length == columnVisible.length) {
            columnVisible = array.clone();
            buildColumnMap();
            fireTableStructureChanged();
            columnCountCache = false; // recalculate the column count
        }
    }

    /**
     * Returns the visibility of a column
     *
     * @param col The column to get visibility of
     * @return The visibility of a column
     */
    public boolean isColumnVisible(final int col) {
        return columnVisible[col];
    }

    /**
     * Returns a boolean array representing the visibility of each column in the model. <b>Do not modify the array
     * outside this class</b>
     *
     * @return the boolean array representing the visibility of each column
     */
    public boolean[] getColumnVisibility() {
        return columnVisible;
    }

    /**
     * Generates the mapping between visible columns
     */
    void buildColumnMap() {
        int index = 0;

        columnMap = new int[getColumnCount()];

        for (int i = 0; i < columnVisible.length; i++) {
            if (columnVisible[i]) {
                columnMap[index] = i;
                index++;
            }
        }
    }

    int getColumnMapping(final int col) {
        return columnMap[col];
    }

    @Override
    public int getRowCount() {
        return account.getTransactionCount();
    }

    @Override
    public String getColumnName(final int col) {
        return cNames[columnMap[col]];
    }

    /**
     * Returns the number of visible columns
     *
     * @return the number of visible columns
     */
    @Override
    public int getColumnCount() {
        if (columnCountCache) {
            return columnCount;
        }

        columnCount = 0;

        for (boolean aColumnVisible : columnVisible) {
            if (aColumnVisible) {
                columnCount++;
            }
        }
        return columnCount;
    }

    @Override
    public Class<?> getColumnClass(final int col) {
        return cClass[columnMap[col]];
    }

    @Override
    public Object getValueAt(final int row, final int col) {
        return getInternalValueAt(row, columnMap[col]);
    }

    @Override
    public void messagePosted(final Message event) {

        if (event.getEvent() == ChannelEvent.CURRENCY_MODIFY) {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    fireTableDataChanged();
                }
            });
        }

        if (account.equals(event.getObject(MessageProperty.ACCOUNT))) {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    switch (event.getEvent()) {
                        case FILE_CLOSING:
                            unregister();
                            return;
                        case TRANSACTION_ADD:
                            Transaction t = (Transaction) event.getObject(MessageProperty.TRANSACTION);
                            int index = account.indexOf(t);
                            balanceCache.ensureCapacity(account.getTransactionCount());
                            balanceCache.clear(index);
                            fireTableRowsInserted(index, index);
                            break;
                        case TRANSACTION_REMOVE:
                            balanceCache.clear();
                            fireTableDataChanged();
                            break;
                        default:
                            break;

                    }
                }
            });
        }
    }

    void unregister() {
        MessageBus.getInstance().unregisterListener(this, MessageChannel.SYSTEM, MessageChannel.TRANSACTION);

        balanceCache.trimToSize(0);
        account = null;
    }

    public Transaction getTransactionAt(final int index) {
        return account.getTransactionAt(index);
    }

    public int indexOf(final Transaction t) {
        return account.indexOf(t);
    }

    /**
     * Returns the balance of the account at the specific index The balance at each index is cached to prevent
     * recalculation each time the table requests an update
     *
     * @param index index to get balance at
     * @return balance at the given index
     */
    BigDecimal getBalanceAt(final int index) {
        if (balanceCache.get(index) != null) {
            return AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), balanceCache.get(index));
        }
        BigDecimal balance = account.getBalanceAt(index);
        balanceCache.set(index, balance);
        return AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), balance);
    }

    /**
     * @param reconcileSymbol The reconcileSymbol to set.
     */
    public void setReconcileSymbol(final String reconcileSymbol) {
        this.reconcileSymbol = reconcileSymbol;
    }
}
