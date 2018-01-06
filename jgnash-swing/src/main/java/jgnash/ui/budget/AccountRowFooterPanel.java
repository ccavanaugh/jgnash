/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Transaction;
import jgnash.engine.budget.BudgetPeriodResults;
import jgnash.engine.budget.BudgetResultsModel;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.text.CommodityFormat;
import jgnash.ui.components.ShadowBorder;
import jgnash.ui.util.JTableUtils;
import jgnash.util.ResourceUtils;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTitledPanel;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static javax.swing.event.TableModelEvent.UPDATE;

/**
 * Panel to display a row footer which summarizes account totals
 *
 * @author Craig Cavanaugh
 */
public class AccountRowFooterPanel extends JPanel {

    private final ExpandingBudgetTableModel model;

    private final AccountRowSummaryModel summaryModel;

    private JComponent header;

    private JComponent footer;

    private JTable table;

    private JTable footerTable;

    private final ExecutorService pool = Executors.newCachedThreadPool();

    private final BudgetResultsModel resultsModel;

    AccountRowFooterPanel(final ExpandingBudgetTableModel model) {
        this.model = model;

        this.resultsModel = model.getResultsModel();

        summaryModel = new AccountRowSummaryModel(model);

        layoutMainPanel();
    }

    public void unregisterListeners() {
        summaryModel.unregisterListeners();
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("d:g", "d");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        setLayout(layout);

        table = new SummaryTable(summaryModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFocusable(false);
        table.setCellSelectionEnabled(false);

        JTableHeader tableHeader = new JTableHeader(table.getColumnModel());
        tableHeader.setReorderingAllowed(false);
        tableHeader.setResizingAllowed(false);
        tableHeader.setTable(table);

        builder.add(table, CC.xy(1, 1));

        header = buildHeader(tableHeader);
        footer = buildFooter();

        setBorder(ShadowBorder.getCompondShadowBorder());

        JTableUtils.packTables(table, footerTable); 

        ToolTipManager.sharedInstance().unregisterComponent(table);
        ToolTipManager.sharedInstance().unregisterComponent(tableHeader);
    }

    public JComponent getTableHeader() {
        return header;
    }

    private static JComponent buildHeader(final JTableHeader tableHeader) {
        final JXTitledPanel panelHeader = new JXTitledPanel(ResourceUtils.getString("Title.Summary"), tableHeader);
        panelHeader.setBorder(ShadowBorder.getCompondShadowBorder());

        return panelHeader;
    }

    /**
     * Sets the height, in pixels, of all cells to rowHeight, revalidates, and
     * repaints. The height of the cells will be equal to the row height minus
     * the row margin.
     *
     * @param rowHeight new row height
     * @see AccountRowHeaderPanel#getRowHeight()
     * @see JTable#setRowHeight(int)
     */
    void setRowHeight(final int rowHeight) {
        table.setRowHeight(rowHeight);
    }

    private JComponent buildFooter() {
        FormLayout layout = new FormLayout("d:g", "d");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout, new JXPanel());

        NumberFormat format = CommodityFormat.getFullNumberFormat(resultsModel.getBaseCurrency());

        footerTable = new BudgetResultsTable(new FooterModel(), format);
        footerTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        footerTable.setFocusable(false);
        footerTable.setCellSelectionEnabled(false);

        builder.add(footerTable, CC.xy(1, 1));

        builder.border(ShadowBorder.getCompondShadowBorder());

        return builder.getPanel();
    }

    public JComponent getFooter() {
        return footer;
    }   

    private class SummaryTable extends AbstractResultsTable {

        SummaryTable(final TableModel model) {
            super(model);
        }

        @Override
        protected NumberFormat getNumberFormat(final int row) {
            Account account = model.get(row);
            return CommodityFormat.getFullNumberFormat(account.getCurrencyNode());
        }
    }

    /**
     * AccountTable model to for the summary footer that computes results by
     * account group
     */
    private class FooterModel extends AbstractTableModel implements MessageListener {

        private List<AccountGroup> groups;

        FooterModel() {
            groups = model.getAccountGroups();
            registerListeners();
        }

        private void registerListeners() {

            summaryModel.addTableModelListener(e -> fireTableDataChanged());

            model.addMessageListener(this);
        }

        @Override
        public int getRowCount() {
            return groups.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            return BigDecimal.class;
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            AccountGroup group = resultsModel.getAccountGroupList().get(rowIndex);

            switch (columnIndex) {
                case 0:
                    return resultsModel.getResults(group).getBudgeted();
                case 1:
                    return resultsModel.getResults(group).getChange();
                case 2:
                    return resultsModel.getResults(group).getRemaining();
                default:
                    return BigDecimal.ZERO;
            }
        }

        @Override
        public void messagePosted(final Message event) {
            switch (event.getEvent()) {
                case ACCOUNT_ADD:
                case ACCOUNT_REMOVE:
                case ACCOUNT_MODIFY:
                case BUDGET_UPDATE:
                    groups = model.getAccountGroups();
                    EventQueue.invokeLater(FooterModel.this::fireTableDataChanged);
                    break;
                default:
                    break;  // ignore any other events
            }
        }
    }

    /**
     * AccountTable model to display a row footer which summarizes account
     * totals
     */
    private class AccountRowSummaryModel extends AbstractTableModel implements MessageListener {

        private final ExpandingBudgetTableModel model;

        private transient TableModelListener listener;

        private final String[] columnNames = {ResourceUtils.getString("Column.Budgeted"),
            ResourceUtils.getString("Column.Actual"), ResourceUtils.getString("Column.Remaining")};

        AccountRowSummaryModel(final ExpandingBudgetTableModel model) {
            this.model = model;

            registerListeners();
        }

        private void registerListeners() {
            // pass through the events from the wrapped table model
            listener = AccountRowSummaryModel.this::fireTableChanged;

            model.addTableModelListener(listener);
            model.addMessageListener(this);
        }

        void unregisterListeners() {
            model.removeTableModelListener(listener);
            model.removeMessageListener(this);
        }

        @Override
        public int getRowCount() {
            return model.getRowCount();
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
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            Account account = model.get(rowIndex);

            BudgetPeriodResults results = resultsModel.getResults(account);

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
        public void messagePosted(final Message event) {
            switch (event.getEvent()) {
                case TRANSACTION_ADD:
                case TRANSACTION_REMOVE:
                    processTransactionEvent(event);
                    break;
                case FILE_CLOSING:
                    unregisterListeners();
                    break;
                case BUDGET_GOAL_UPDATE:
                    processBudgetGoalUpdate(event);
                    break;
            }
        }

        private void processBudgetGoalUpdate(final Message message) {
            final Runnable thread = () -> {

                List<Account> accountList = ((Account) message.getObject(MessageProperty.ACCOUNT)).getAncestors();

                for (Account account : accountList) {

                    final int row = model.indexOf(account);

                    EventQueue.invokeLater(() -> {
                        fireTableChanged(new TableModelEvent(AccountRowSummaryModel.this, row, row, ALL_COLUMNS, UPDATE));
                        JTableUtils.packTables(table, footerTable);
                    });
                }
            };

            pool.submit(thread);
        }

        private void processTransactionEvent(final Message message) {

            final Runnable thread = () -> {
                final Transaction transaction = message.getObject(MessageProperty.TRANSACTION);

                // build a list of accounts include ancestors that will be impacted by the transaction changes
                final Set<Account> accounts = new HashSet<>();

                for (Account account : transaction.getAccounts()) {
                    accounts.addAll(account.getAncestors());
                }

                for (Account account : accounts) {
                    final int row = model.indexOf(account);

                    EventQueue.invokeLater(() -> {
                        fireTableChanged(new TableModelEvent(AccountRowSummaryModel.this, row, row, ALL_COLUMNS, UPDATE));
                        JTableUtils.packTables(table, footerTable);
                    });
                }
            };

            pool.submit(thread);
        }
    }
}
