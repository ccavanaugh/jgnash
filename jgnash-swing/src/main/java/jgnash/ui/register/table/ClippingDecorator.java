/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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

import java.time.LocalDate;

import javax.swing.event.TableModelListener;

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.time.DateUtils;
import jgnash.util.NotNull;

/**
 * A decorator that wraps an AbstractRegisterTableModel to clip the transactions displayed using a start and end date.
 * 
 * @author Craig Cavanaugh
 */
public final class ClippingDecorator implements ClippingModel {

    private final AbstractRegisterTableModel model;

    private LocalDate endDate = LocalDate.now();

    private LocalDate startDate = LocalDate.now();

    private int startIndex = 0;

    private int endIndex = 0;

    public ClippingDecorator(final AbstractRegisterTableModel model) {
        this.model = model;

        if (model.getRowCount() > 0) {

            startDate = model.getTransactionAt(0).getLocalDate();

            endIndex = model.getRowCount() - 1;

            endDate = getEndDate();
        }
    }

    /**
     * @see jgnash.ui.register.table.ClippingModel#setStartDate(java.time.LocalDate)
     */
    @Override
    public void setStartDate(final LocalDate startDate) {
        findStartIndex(startDate);
    }

    @Override
    public void setStartIndex(final int start) {
        startIndex = start;
    }

//    /**
//     * @see jgnash.ui.register.table.ClippingModel#getStartDate()
//     */
//    @Override
//    public Date getStartDate() {
//        Date date = new Date();
//
//        /* Do not assume the model/account as any transactions */
//        if (model.getRowCount() > 0) {
//            date = model.getTransactionAt(startIndex).getDate();
//        }
//        return date;
//    }

    /**
     * @see jgnash.ui.register.table.ClippingModel#setEndDate(java.time.LocalDate)
     */
    @Override
    public void setEndDate(final LocalDate stopDate) {
        findStopIndex(stopDate);
    }

    @Override
    public void setEndIndex(final int end) {
        endIndex = end;
    }


    private LocalDate getEndDate() {
        LocalDate date = LocalDate.now();

        /* Do not assume the model/account has any transactions */
        if (model.getRowCount() > 0) {
            date = model.getTransactionAt(endIndex).getLocalDate();
        }
        return date;
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
        if (model.getAccount().getTransactionCount() == 0) {
            return 0;
        }

        if (endIndex == startIndex) {

            Transaction t = getAccount().getTransactionAt(getAccount().getTransactionCount() - 1);
            if (DateUtils.before(t.getLocalDate(), startDate, false)) {
                return 0;
            }

            t = model.getTransactionAt(startIndex);
            if (DateUtils.after(t.getLocalDate(), startDate) && DateUtils.before(t.getLocalDate(), endDate, true)) {
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

    private void findStartIndex(final LocalDate date) {
        startDate = date;

        for (int i = 0; i < model.getRowCount(); i++) {
            Transaction t = model.getTransactionAt(i);
            if (DateUtils.after(t.getLocalDate(), date)) {
                startIndex = i;
                return;
            }
        }
        startIndex = model.getRowCount() - 1;
    }

    private void findStopIndex(final LocalDate date) {
        endDate = date;

        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            Transaction t = model.getTransactionAt(i);
            if (DateUtils.before(t.getLocalDate(), date)) {
                endIndex = i;
                return;
            }
        }
        endIndex = model.getRowCount() - 1;
    }
}
