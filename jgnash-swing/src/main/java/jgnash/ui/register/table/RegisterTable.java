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
package jgnash.ui.register.table;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.table.TableCellRenderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import jgnash.engine.Account;
import jgnash.engine.CommodityNode;
import jgnash.engine.CurrencyNode;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.text.CommodityFormat;
import jgnash.ui.components.FormattedJTable;
import jgnash.ui.register.AccountBalanceDisplayManager;
import jgnash.time.DateUtils;
import jgnash.util.NotNull;

/**
 * Extended JTable for displaying a transaction register
 *
 * @author Craig Cavanaugh
 * @author axnotizes
 */
public class RegisterTable extends FormattedJTable {

    private NumberFormat fullFormat;

    private NumberFormat shortFormat;

    private final DateTimeFormatter dateFormatter = DateUtils.getShortDateFormatter();

    public RegisterTable(final AccountTableModel dm) {
        super(dm);
        init();
    }

    private void init() {
        setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        setCellSelectionEnabled(true);
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setShowGrid(false);
        setFillsViewportHeight(true);

        AccountTableModel model = (AccountTableModel) getModel();
        CommodityNode node = model.getAccount().getCurrencyNode();

        fullFormat = CommodityFormat.getFullNumberFormat(node);
        shortFormat = CommodityFormat.getShortNumberFormat(node);

        // disable tool tips to improve speed   
        ToolTipManager.sharedInstance().unregisterComponent(getTableHeader());
    }

    /**
     * Override prepareRenderer instead of using a custom renderer so the look and feel is preserved
     *
     * @see javax.swing.JTable#prepareRenderer(javax.swing.table.TableCellRenderer, int, int)
     */
    @Override
    public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {
        Component c = super.prepareRenderer(renderer, row, column);

        final Object value = getModel().getValueAt(row, convertColumnIndexToModel(column));   // column may have been reordered

        if (getModel() instanceof AbstractRegisterTableModel) {
            Transaction t = ((AbstractRegisterTableModel) getModel()).getTransactionAt(row);

            if (t.getLocalDate().isAfter(LocalDate.now())) {
                c.setFont(c.getFont().deriveFont(Font.ITALIC));
            }

            if (QuantityStyle.class.isAssignableFrom(getColumnClass(column)) && t instanceof InvestmentTransaction && c instanceof JLabel) {
                ((JLabel) c).setHorizontalAlignment(SwingConstants.RIGHT);

                if (value instanceof Number) {
                    final NumberFormat numberFormat = CommodityFormat.getShortNumberFormat(((InvestmentTransaction) t).getSecurityNode());
                    ((JLabel) c).setText(numberFormat.format(value));
                } else {
                    ((JLabel) c).setText("");
                }
            }
        }

        if (LocalDate.class.isAssignableFrom(getColumnClass(column)) && c instanceof JLabel) {
            if (value instanceof LocalDate) {
                ((JLabel) c).setText(dateFormatter.format((TemporalAccessor) value));
            }
        } else if (FullCommodityStyle.class.isAssignableFrom(getColumnClass(column)) && c instanceof JLabel) {

            ((JLabel) c).setHorizontalAlignment(SwingConstants.RIGHT);

            if (value instanceof Number) {

                if (!isRowSelected(row) && ((BigDecimal) value).signum() < 0) {
                    c.setForeground(Color.RED);
                }

                ((JLabel) c).setText(fullFormat.format(value));
            } else {
                ((JLabel) c).setText("");
            }
        } else if (ShortCommodityStyle.class.isAssignableFrom(getColumnClass(column)) && c instanceof JLabel) {

            ((JLabel) c).setHorizontalAlignment(SwingConstants.RIGHT);

            if (value instanceof Number) {
                ((JLabel) c).setText(shortFormat.format(value));
            } else {
                ((JLabel) c).setText("");
            }
        }

        return c;
    }

    @Override
    public String getToolTipText(@NotNull final MouseEvent event) {
        int[] rows = getSelectedRows();

        if (rows.length > 1) {
            AccountTableModel model = (AccountTableModel) getModel();
            Account account = model.getAccount();
            CurrencyNode node = account.getCurrencyNode();

            BigDecimal amount = BigDecimal.ZERO;

            // correct the row indexes if the model is sorted
            if (model instanceof SortedTableModel) {
                for (int i = 0; i < rows.length; i++) {
                    rows[i] = ((SortedTableModel) model).convertRowIndexToAccount(rows[i]);
                }
            }

            for (int row : rows) {
                amount = amount.add(AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), account.getTransactionAt(row).getAmount(account)));
            }

            return CommodityFormat.getFullNumberFormat(node).format(amount);
        }

        return null;
    }
}
