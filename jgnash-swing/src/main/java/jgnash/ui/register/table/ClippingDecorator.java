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

import java.util.Date;

import javax.swing.event.TableModelListener;

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.util.DateUtils;

/**
 * A decorator that wraps an AbstractRegisterTableModel to clip the transactions displayed using a start and end date.
 * 
 * @author Craig Cavanaugh
 * @version $Id: ClippingDecorator.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public final class ClippingDecorator implements ClippingModel {

    private AbstractRegisterTableModel model;

    private Date endDate = new Date();

    private Date startDate = new Date();

    private int startIndex = 0;

    private int endIndex = 0;

    public ClippingDecorator(final AbstractRegisterTableModel model) {
        this.model = model;

        if (model.getRowCount() > 0) {

            startDate = model.getTransactionAt(0).getDate();

            endIndex = model.getRowCount() - 1;

            endDate = getEndDate();
        }
    }

    /**
     * @see jgnash.ui.register.table.ClippingModel#setStartDate(java.util.Date)
     */
    @Override
    public void setStartDate(final Date startDate) {
        findStartIndex(startDate);
    }

    @Override
    public void setStartIndex(final int start) {
        startIndex = start;
    }

    /**
     * @see jgnash.ui.register.table.ClippingModel#getStartDate()
     */
    @Override
    public Date getStartDate() {
        Date date = new Date();

        /* Do not assume the model/account as any transactions */
        if (model.getRowCount() > 0) {
            date = model.getTransactionAt(startIndex).getDate();
        }
        return date;
    }

    /**
     * @see jgnash.ui.register.table.ClippingModel#setEndDate(java.util.Date)
     */
    @Override
    public void setEndDate(final Date stopDate) {
        findStopIndex(stopDate);
    }

    @Override
    public void setEndIndex(final int end) {
        endIndex = end;
    }

    /**
     * @see jgnash.ui.register.table.ClippingModel#getEndDate()
     */
    @Override
    public Date getEndDate() {
        Date date = new Date();

        /* Do not assume the model/account as any transactions */
        if (model.getRowCount() > 0) {
            date = model.getTransactionAt(endIndex).getDate();
        }
        return date;
    }

    /**
     * @see jgnash.ui.register.table.RegisterModel#setReconcileSymbol(java.lang.String)
     */
    @Override
    public void setReconcileSymbol(final String reconcileSymbol) {
        model.setReconcileSymbol(reconcileSymbol);
    }

    /**
     * @return prefered column weight
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
        if (model.getAccount().getTransactionCount() == 0) {
            return 0;
        }

        if (endIndex == startIndex) {

            Transaction t = getAccount().getTransactionAt(getAccount().getTransactionCount() - 1);
            if (DateUtils.before(t.getDate(), startDate, false)) {
                return 0;
            }

            t = model.getTransactionAt(startIndex);
            if (DateUtils.after(t.getDate(), startDate, true) && DateUtils.before(t.getDate(), endDate, true)) {
                return 1;
            }
            return 0;
        }

        return endIndex - startIndex + 1;
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
    public String getColumnName(int col) {
        return model.getColumnName(col);
    }

    /**
     * @param col index of column class to retrieve
     * @see javax.swing.table.TableModel#getColumnClass(int)
     */
    @Override
    public Class<?> getColumnClass(int col) {
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
        return model.getValueAt(row + startIndex, col);
    }

    /**
     * @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
     */
    @Override
    public void setValueAt(final Object arg0, final int arg1, final int arg2) {
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

    private void findStartIndex(final Date date) {
        startDate = date;

        for (int i = 0; i < model.getRowCount(); i++) {
            Transaction t = model.getTransactionAt(i);
            if (DateUtils.after(t.getDate(), date, true)) {
                startIndex = i;
                return;
            }
        }
        startIndex = model.getRowCount() - 1;
    }

    private void findStopIndex(final Date date) {
        endDate = date;

        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            Transaction t = model.getTransactionAt(i);
            if (DateUtils.before(t.getDate(), date, true)) {
                endIndex = i;
                return;
            }
        }
        endIndex = model.getRowCount() - 1;
    }
}
