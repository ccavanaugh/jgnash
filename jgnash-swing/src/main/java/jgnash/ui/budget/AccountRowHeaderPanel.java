/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.EngineFactory;
import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetGoal;
import jgnash.ui.components.ShadowBorder;
import jgnash.ui.components.expandingtable.ExpandingTable;

import org.jdesktop.swingx.JXTitledPanel;

/**
 * @author Craig Cavanaugh
 *
 */
final class AccountRowHeaderPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ExpandingBudgetTableModel tableModel;

    private final Budget budget;

    private JTable table;

    AccountRowHeaderPanel(final Budget budget, final ExpandingBudgetTableModel model) {
        tableModel = model;

        this.budget = budget;

        layoutMainPanel();
    }

    public JPanel getTableHeader() {
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);

        JPanel panel = new JXTitledPanel("  ", header);
        panel.setBorder(ShadowBorder.getCompondShadowBorder());

        return panel;
    }

    JTable getTable() {
        return table;
    }

    /**
     * Returns the height of a table row, in pixels. The Expanding table alters row height to create space for icons.
     * This exposes the row height to make it possible to synchronize the height across the period tables
     * 
     * @see JTable#getRowHeight()
     * @see BudgetPeriodPanel#setRowHeight(int)
     * @return the height in pixels of a table row
     */
    int getRowHeight() {
        return table.getRowHeight();
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("pref:g", "t:d:g");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        setLayout(layout);

        table = new AccountTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // can only handle one selection at a time
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // add a double click listener to edit an account
        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int rowIndex = ((JTable) e.getSource()).getSelectedRow();

                    Account account = tableModel.get(rowIndex);

                    if (!account.isLocked() && !account.isPlaceHolder()) {
                        showBudgetGoalDialog(account);
                    }
                }
            }
        });

        builder.add(table, CC.xy(1, 1));

        setBorder(ShadowBorder.getCompondShadowBorder());
    }

    @Override
    public void setPreferredSize(final Dimension preferredSize) {
        table.setPreferredSize(new Dimension(preferredSize.width, table.getRowCount() * table.getRowHeight()));
        table.doLayout();
    }

    private void showBudgetGoalDialog(final Account account) {
        BudgetGoal oldGoal = budget.getBudgetGoal(account);
        BudgetGoalDialog d = new BudgetGoalDialog(account, oldGoal, budget.getWorkingYear());

        d.setVisible(true);

        if (d.getResult()) {
            BudgetGoal newGoal = d.getBudgetGoal();

            if (!newGoal.equals(oldGoal)) {
                EngineFactory.getEngine(EngineFactory.DEFAULT).updateBudgetGoals(budget, account, newGoal);
            }
        }
    }

    public JComponent getFooter() {
        FormLayout layout = new FormLayout("d:g", "d");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout, new JPanel());

        JTable table = new JTable(new GroupTableModel());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFocusable(false);
        table.setCellSelectionEnabled(false);

        builder.add(table, CC.xy(1, 1));

        builder.setBorder(ShadowBorder.getCompondShadowBorder());

        return builder.getPanel();
    }

    private static class AccountTable extends ExpandingTable<Account> {

        public AccountTable(final ExpandingBudgetTableModel model) {
            super(model);
        }

        @Override
        public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {

            Component c = super.prepareRenderer(renderer, row, column);

            Account account = ((ExpandingBudgetTableModel) getModel()).get(row);
            c.setEnabled(!account.isPlaceHolder());

            return c;
        }

    }

    /**
     * Table model to display account groups
     */
    private class GroupTableModel extends AbstractTableModel {

        private List<AccountGroup> groups;

        GroupTableModel() {
            groups = tableModel.getAccountGroups();

            tableModel.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    groups = tableModel.getAccountGroups();
                    fireTableDataChanged();
                }
            });
        }

        @Override
        public int getRowCount() {
            return groups.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return groups.get(rowIndex).toString();
        }
    }
}
