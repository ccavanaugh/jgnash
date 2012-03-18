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
package jgnash.ui.budget;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.EventQueue;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ToolTipManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;

import jgnash.engine.AccountGroup;
import jgnash.engine.EngineFactory;
import jgnash.engine.budget.BudgetPeriodDescriptor;
import jgnash.message.Message;
import jgnash.message.MessageListener;
import jgnash.text.CommodityFormat;
import jgnash.ui.components.ShadowBorder;
import jgnash.ui.util.JTableUtils;

import org.jdesktop.swingx.JXTitledPanel;

/**
 * Panel for displaying one budget budgetPeriod
 *
 * @author Craig Cavanaugh
 *
 */
final class BudgetPeriodPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JXTitledPanel periodHeader;

    private JPanel periodFooter;

    private JTable table;
    
    private JTable footerTable;

    private BudgetPeriodModel model;

    public BudgetPeriodPanel(final BudgetPeriodModel model) {
        this.model = model;
        layoutMainPanel();
    }

    /**
     * Determines if the specified date lies within or inclusive of this panel's budgetPeriod
     *
     * @param date check date
     * @return true if the date lies within this budgetPeriod
     * @see BudgetPeriodDescriptor#isBetween(java.util.Date)
     * @see BudgetPeriodModel#isBetween(java.util.Date)
     */
    boolean isBetween(final Date date) {
        return model.isBetween(date);
    }

    /**
     * Sets the height, in pixels, of all cells to rowHeight, revalidates, and repaints. The height of the cells will be
     * equal to the row height minus the row margin.
     *
     * @param rowHeight new row height
     * @see AccountRowHeaderPanel#getRowHeight()
     * @see JTable#setRowHeight(int)
     */
    protected void setRowHeight(final int rowHeight) {
        table.setRowHeight(rowHeight);
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("d:g", "d");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        setLayout(layout);

        NumberFormat format = CommodityFormat.getShortNumberFormat(model.getCurrency());

        table = new BudgetResultsTable(model, format);
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

        FormLayout layout = new FormLayout("d:g", "d");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout, new JPanel());

        NumberFormat format = CommodityFormat.getShortNumberFormat(EngineFactory.getEngine(EngineFactory.DEFAULT).getDefaultCurrency());

        footerTable = new BudgetResultsTable(new FooterModel(), format);
        footerTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        footerTable.setFocusable(false);
        footerTable.setCellSelectionEnabled(false);

        builder.add(footerTable, CC.xy(1, 1));

        builder.setBorder(ShadowBorder.getCompondShadowBorder());

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

            model.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    fireTableDataChanged();
                }
            });

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

        protected BigDecimal getTotalRemaining(final AccountGroup group) {
            return model.getRemainingTotal(group);
        }

        protected BigDecimal getTotalChange(final AccountGroup group) {
            return model.getChangeTotal(group);
        }

        protected BigDecimal getTotalBudgeted(final AccountGroup group) {
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
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            fireTableDataChanged();
                            JTableUtils.packTables(table, footerTable); 
                        }
                    });                   
                default:
            }
        }
    }
}
