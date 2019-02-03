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
package jgnash.ui.budget;

import java.awt.EventQueue;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ToolTipManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;

import jgnash.engine.AccountGroup;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.budget.BudgetPeriodDescriptor;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageListener;
import jgnash.text.CommodityFormat;
import jgnash.ui.components.ShadowBorder;
import jgnash.ui.util.JTableUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

import org.jdesktop.swingx.JXTitledPanel;

/**
 * Panel for displaying one budget budgetPeriod
 *
 * @author Craig Cavanaugh
 */
final class BudgetPeriodPanel extends JPanel {

    private JXTitledPanel periodHeader;

    private JPanel periodFooter;

    private JTable table;
    
    private JTable footerTable;

    private final BudgetPeriodModel model;

    public BudgetPeriodPanel(final BudgetPeriodModel model) {
        this.model = model;
        layoutMainPanel();
    }

    /**
     * Determines if the specified date lies within or inclusive of this panel's budgetPeriod
     *
     * @param date check date
     * @return true if the date lies within this budgetPeriod
     * @see BudgetPeriodDescriptor#isBetween(java.time.LocalDate)
     * @see BudgetPeriodModel#isBetween(java.time.LocalDate)
     */
    boolean isBetween(final LocalDate date) {
        return model.isBetween(date);
    }

    /**
     * Sets the height, in pixels, of all cells to rowHeight, revalidate, and repaints. The height of the cells will be
     * equal to the row height minus the row margin.
     *
     * @param rowHeight new row height
     * @see AccountRowHeaderPanel#getRowHeight()
     * @see JTable#setRowHeight(int)
     */
    void setRowHeight(final int rowHeight) {
        table.setRowHeight(rowHeight);
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("d:g", "d");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        setLayout(layout);

        table = new AccountPeriodResultsTable(model);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFocusable(false);
        table.setCellSelectionEnabled(false);

        JTableHeader header = new JTableHeader(table.getColumnModel());
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setTable(table);

        buildHeader(header);
        buildFooter();
        
        JTableUtils.packTables(table, footerTable); 
                
        builder.add(table, CC.xy(1, 1));

        setBorder(ShadowBorder.getCompondShadowBorder());

        ToolTipManager.sharedInstance().unregisterComponent(table);
        ToolTipManager.sharedInstance().unregisterComponent(header);
    }        

    public JComponent getPeriodHeader() {
        return periodHeader;
    }

    private void buildHeader(final JTableHeader header) {
        periodHeader = new JXTitledPanel(model.getPeriodDescription(), header);
        periodHeader.setBorder(ShadowBorder.getCompondShadowBorder());
    }

    private void buildFooter() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final FormLayout layout = new FormLayout("d:g", "d");

        final DefaultFormBuilder builder = new DefaultFormBuilder(layout, new JPanel());

        final NumberFormat format = CommodityFormat.getFullNumberFormat(engine.getDefaultCurrency());

        footerTable = new BudgetResultsTable(new FooterModel(), format);
        footerTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        footerTable.setFocusable(false);
        footerTable.setCellSelectionEnabled(false);

        builder.add(footerTable, CC.xy(1, 1));

        builder.border(ShadowBorder.getCompondShadowBorder());

        periodFooter = builder.getPanel();
    }

    public JComponent getPeriodFooter() {
        return periodFooter;
    }

    /**
     * AccountTable model to for the summary footer that computes results by account group
     */
    private class FooterModel extends AbstractTableModel implements MessageListener {

        private List<AccountGroup> groups;

        FooterModel() {

            groups = model.getExpandingBudgetTableModel().getAccountGroups();
            registerListeners();
        }

        private void registerListeners() {

            model.addTableModelListener(e -> fireTableDataChanged());

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
            switch (columnIndex) {
                case 0:
                    return getTotalBudgeted(groups.get(rowIndex));
                case 1:
                    return getTotalChange(groups.get(rowIndex));
                case 2:
                    return getTotalRemaining(groups.get(rowIndex));
                default:
                    return BigDecimal.ZERO;
            }
        }

        BigDecimal getTotalRemaining(final AccountGroup group) {
            return model.getRemainingTotal(group);
        }

        BigDecimal getTotalChange(final AccountGroup group) {
            return model.getChangeTotal(group);
        }

        BigDecimal getTotalBudgeted(final AccountGroup group) {
            return model.getBudgetedTotal(group);
        }

        @Override
        public void messagePosted(final Message event) {
            switch (event.getEvent()) {
                case ACCOUNT_ADD:
                case ACCOUNT_REMOVE:
                case ACCOUNT_MODIFY:
                case BUDGET_UPDATE:
                    groups = model.getExpandingBudgetTableModel().getAccountGroups();
                    EventQueue.invokeLater(() -> {
                        fireTableDataChanged();
                        JTableUtils.packTables(table, footerTable);
                    });
                    break;
                default:
                    break;  // ignore any other events
            }
        }
    }
}
