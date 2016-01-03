/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelListener;

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.engine.search.PayeeMatcher;
import jgnash.util.NotNull;

/**
 * A decorator that wraps an AbstractRegisterTableModel to filter
 * the transactions displayed using the string "filter".
 *
 * @author Pranay Kumar
 */
public class FilterDecorator implements FilterModel {

    private final AbstractRegisterTableModel model;
    private String filter;
    private PayeeMatcher pm;
    private final List<Integer> matchedPayee;

    @Override
    public void setFilter(final String filter) {
        this.filter = filter;
        pm = new PayeeMatcher(filter, false);
        updateMatches();
    }

    private void updateMatches() {
        this.matchedPayee.clear();
        for (int i = 0; i < model.getRowCount(); i++) {
            Transaction t = model.getTransactionAt(i);
            if (pm.matches(t)) {
                matchedPayee.add(i);
            }
        }
    }

    public FilterDecorator(final AbstractRegisterTableModel model) {
        this.model = model;
        filter = "*";
        pm = new PayeeMatcher(filter, false);
        matchedPayee = new ArrayList<>();
        updateMatches();
    }

    /**
     * @return preferred column weight
     * @see jgnash.ui.register.table.RegisterModel#getPreferredColumnWeights()
     */
    @Override
    public int[] getPreferredColumnWeights() {
        return model.getPreferredColumnWeights();
    }

    /**
     * @return account for this decorator
     * @see jgnash.ui.register.table.AccountTableModel#getAccount()
     */
    @Override
    public Account getAccount() {
        return model.getAccount();
    }

    /**
     * @see javax.swing.table.TableModel#getRowCount()
     */
    @Override
    public int getRowCount() {
        return matchedPayee.size();
    }

    /**
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    @Override
    public int getColumnCount() {
        return model.getColumnCount();
    }

    /**
     * @param col index of column name to retrieve
     * @see javax.swing.table.TableModel#getColumnName(int)
     */
    @Override
    public String getColumnName(final int col) {
        return model.getColumnName(col);
    }

    /**
     * @param col index of column class to retrieve
     * @see javax.swing.table.TableModel#getColumnClass(int)
     */
    @Override
    public Class<?> getColumnClass(final int col) {
        return model.getColumnClass(col);
    }

    /**
     * @see javax.swing.table.TableModel#isCellEditable(int, int)
     */
    @Override
    public boolean isCellEditable(final int arg0, final int arg1) {
        return model.isCellEditable(arg0, arg1);
    }

    /**
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    public Object getValueAt(final int row, final int col) {
        return model.getValueAt(this.matchedPayee.get(row), col);
    }

    /**
     * @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
     */
    @Override
    public void setValueAt(@NotNull final Object arg0, final int arg1, final int arg2) {
        model.setValueAt(arg0, arg1, arg2);
    }

    /**
     * @see javax.swing.table.TableModel#addTableModelListener(javax.swing.event.TableModelListener)
     */
    @Override
    public void addTableModelListener(final TableModelListener arg0) {
        model.addTableModelListener(arg0);
    }

    /**   
     * @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
     */
    @Override
    public void removeTableModelListener(final TableModelListener arg0) {
        model.removeTableModelListener(arg0);
    }

    @Override
    public void setColumnVisible(final String name, final boolean visible) {
        model.setColumnVisible(name, visible);
    }
}
