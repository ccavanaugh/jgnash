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
package jgnash.ui.wizards.imports;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultRowSorter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.RowSorter;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import jgnash.convert.imports.ImportState;
import jgnash.convert.imports.ImportTransaction;
import jgnash.engine.Account;
import jgnash.ui.components.AccountListComboBox;
import jgnash.ui.components.FormattedJTable;
import jgnash.ui.util.IconUtils;
import jgnash.time.DateUtils;
import jgnash.util.NotNull;
import jgnash.util.Nullable;
import jgnash.util.ResourceUtils;

/**
 * @author Craig Cavanaugh
 */
class ImportTable extends FormattedJTable {

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private final Model model;

    private final TableCellEditor accEditor = new DefaultCellEditor(new AccountListComboBox());

    private List<? extends ImportTransaction> transactions = Collections.emptyList();

    @SuppressWarnings("rawtypes")
    ImportTable() {
        super();
        model = new Model();
        setAutoCreateRowSorter(true);
        setModel(model);

        RowSorter<?> rowSorter = this.getRowSorter();

        if (rowSorter instanceof DefaultRowSorter) {
            ((DefaultRowSorter) rowSorter).setSortable(0, false);
        }

        // toggle the import state of a transaction
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent e) {
                EventQueue.invokeLater(() -> {
                    int row = rowAtPoint(e.getPoint());
                    int col = columnAtPoint(e.getPoint());

                    if (col == 0 && row >= 0) {
                        row = convertRowIndexToModel(row);
                        col = convertColumnIndexToModel(col);

                        if (col == 0) {
                            ImportTransaction t = transactions.get(row);

                            switch (t.getState()) {
                                case EQUAL:
                                    t.setState(ImportState.NOT_EQUAL);
                                    model.fireTableCellUpdated(row, col);
                                    break;
                                case NOT_EQUAL:
                                    t.setState(ImportState.EQUAL);
                                    model.fireTableCellUpdated(row, col);
                                    break;
                                case NEW:
                                    t.setState(ImportState.IGNORE);
                                    model.fireTableCellUpdated(row, col);
                                    break;
                                case IGNORE:
                                    t.setState(ImportState.NEW);
                                    model.fireTableCellUpdated(row, col);
                                    break;
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * Override prepareRenderer instead of using a custom renderer so the look and feel is preserved
     *
     * @see javax.swing.JTable#prepareRenderer(javax.swing.table.TableCellRenderer, int, int)
     */
    @Override
    public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {

        Component c = super.prepareRenderer(renderer, row, column);

        int modelCol = convertColumnIndexToModel(column);

        if (modelCol == 0) {
            Icon icon = (Icon) model.getValueAt(row, column);
            ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
            ((JLabel) c).setIcon(icon);
            ((JLabel) c).setText(null);
            ((JLabel) c).setToolTipText(null);
        } else if (modelCol == 3 || modelCol == 4) {    // Display tool tip text for memo and payee because of potential length
            if (c instanceof JComponent) {
                if (getValueAt(row, column) != null && !getValueAt(row, column).toString().isEmpty()) {
                    ((JComponent) c).setToolTipText(getValueAt(row, column).toString());
                } else {
                    ((JComponent) c).setToolTipText(null);
                }
            }
        } else {
            if (c instanceof JComponent) {
                ((JComponent) c).setToolTipText(null);
            }
        }

        return c;
    }

    public List<? extends ImportTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(final List<? extends ImportTransaction> transactions) {
        this.transactions = transactions;
    }

    void deleteSelected() {
        int row = getSelectedRow();

        transactions.remove(row);
        model.fireTableRowsDeleted(row, row);
    }

    @Override
    public TableCellEditor getCellEditor(final int row, final int column) {
        if (column == 5) {
            return accEditor;
        }
        return super.getCellEditor(row, column);
    }

    public void fireTableDataChanged() {
        model.fireTableDataChanged();
    }

    class Model extends AbstractTableModel {

        private ImageIcon equalIcon;

        private ImageIcon addIcon;

        private ImageIcon notEqualIcon;

        private ImageIcon removeIcon;

        private final String[] cNames = {" ", rb.getString("Column.Date"), rb.getString("Column.Num"),
                rb.getString("Column.Payee"), rb.getString("Column.Memo"), rb.getString("Column.Account"),
                rb.getString("Column.Amount")};

        private final Class<?>[] cClass = {String.class, String.class, String.class, String.class, String.class,
                String.class, BigDecimal.class};

        private final DateTimeFormatter dateTimeFormatter = DateUtils.getShortDateFormatter();

        Model() {
            notEqualIcon = IconUtils.getIcon("/jgnash/resource/not-equal.png");
            notEqualIcon.setDescription("not");

            equalIcon = IconUtils.getIcon("/jgnash/resource/equal.png");
            equalIcon.setDescription("equals");

            addIcon = IconUtils.getIcon("/jgnash/resource/add.png");
            addIcon.setDescription("add");

            removeIcon = IconUtils.getIcon("/jgnash/resource/remove.png");
            removeIcon.setDescription("remove");
        }

        @Override
        public String getColumnName(final int column) {
            return cNames[column];
        }

        @Override
        public Class<?> getColumnClass(final int column) {
            return cClass[column];
        }

        @Override
        public boolean isCellEditable(final int rowIndex, final int columnIndex) {
            return columnIndex == 5;
        }

        /**
         * Returns the number of columns in the model. A {@code JTable} uses this method to determine how many
         * columns it should create and display by default.
         *
         * @return the number of columns in the model
         * @see #getRowCount
         */

        @Override
        public int getColumnCount() {
            return cNames.length;
        }

        /**
         * Returns the number of rows in the model. A {@code JTable} uses this method to determine how many rows it
         * should display. This method should be quick, as it is called frequently during rendering.
         *
         * @return the number of rows in the model
         * @see #getColumnCount
         */
        @Override
        public int getRowCount() {
            return transactions.size();
        }

        /**
         * Returns the value for the cell at {@code columnIndex} and {@code rowIndex}.
         *
         * @param rowIndex    the row whose value is to be queried
         * @param columnIndex the column whose value is to be queried
         * @return the value Object at the specified cell
         */
        @Override
        @Nullable
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            ImportTransaction transaction = transactions.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    switch (transaction.getState()) {
                        case EQUAL:
                            return equalIcon;
                        case NOT_EQUAL:
                            return notEqualIcon;
                        case IGNORE:
                            return removeIcon;
                        default:
                            return addIcon;
                    }
                case 1:
                    return dateTimeFormatter.format(transaction.getDatePosted());
                case 2:
                    return transaction.getCheckNumber();
                case 3:
                    return transaction.getPayee();
                case 4:
                    return transaction.getMemo();
                case 5:
                    if (transaction.getAccount() != null) {
                        return transaction.getAccount().toString();
                    }
                    return null;
                case 6:
                    return transaction.getAmount();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(@NotNull final Object value, final int rowIndex, final int columnIndex) {
            if (columnIndex == 5) {
                ImportTransaction transaction = transactions.get(rowIndex);

                transaction.setAccount((Account) value);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }

}
