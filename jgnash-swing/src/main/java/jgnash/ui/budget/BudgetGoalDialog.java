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
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import jgnash.engine.Account;
import jgnash.engine.MathConstants;
import jgnash.engine.budget.BudgetGoal;
import jgnash.engine.budget.BudgetPeriod;
import jgnash.engine.budget.BudgetPeriodDescriptor;
import jgnash.engine.budget.BudgetPeriodDescriptorFactory;
import jgnash.text.CommodityFormat;
import jgnash.ui.UIApplication;
import jgnash.ui.components.FormattedJTable;
import jgnash.ui.util.DialogUtils;
import jgnash.ui.util.JTableUtils;
import jgnash.util.Resource;

/**
 * A Dialog to manage a BudgetGoal.
 * 
 * @author Craig Cavanaugh
 */
public final class BudgetGoalDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(BudgetGoalDialog.class.getName());

    private static final int SCROLLPANE_WIDTH = 120;

    private static final int SCROLLPANE_HEIGHT = 200;

    private final Resource rb = Resource.get();

    private JComboBox<BudgetPeriod> budgetPeriodCombo;

    private BudgetGoal budgetGoal;

    private JButton cancelButton;

    private JButton okButton;

    private boolean result = false;

    private PeriodTableModel model;

    private Account account;

    private int workingYear;

    /**
     * Creates a dialog for modifying account specific budget goals. The supplied <code>BudgetGoal</code> is cloned
     * internally so side effects do not occur.
     * 
     * @param account <code>Account</code> budget goals being modified
     * @param budgetGoal <code>BudgetGoal</code> to clone and modify
     * @param workingYear the working year for the budget periods
     */
    public BudgetGoalDialog(final Account account, final BudgetGoal budgetGoal, int workingYear) {
        super(UIApplication.getFrame(), true);
        setTitle(rb.getString("Title.BudgetGoal") + " - " + account.getName());
        setIconImage(Resource.getImage("/jgnash/resource/gnome-money.png"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        this.workingYear = workingYear;
        this.account = account;

        if (budgetGoal == null) {
            throw new IllegalArgumentException("BudgetGoal may not be null");
        }

        try {
            this.budgetGoal = (BudgetGoal) budgetGoal.clone();
        } catch (CloneNotSupportedException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }

        model = new PeriodTableModel(BudgetPeriodDescriptorFactory.getDescriptors(workingYear, budgetGoal.getBudgetPeriod()));

        layoutMainPanel();
    }
 
	private void layoutMainPanel() {
        FormLayout contentLayout = new FormLayout("fill:p:g, $lcgap, fill:p:g","f:p:g, $ugap, f:p");
        JPanel contentPanel = new JPanel(contentLayout);
        DefaultFormBuilder contentBuilder = new DefaultFormBuilder(contentLayout, contentPanel);
        contentBuilder.setDefaultDialogBorder();

        FormLayout layout = new FormLayout("right:d, $lcgap, fill:p:g", "f:p, $rgap, d, $ugap, f:p:g");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        cancelButton = new JButton(rb.getString("Button.Cancel"));
        okButton = new JButton(rb.getString("Button.Ok"));

        budgetPeriodCombo = new JComboBox<>();            
        budgetPeriodCombo.setModel(new DefaultComboBoxModel<>(BudgetPeriod.values()));        
        budgetPeriodCombo.setSelectedItem(getBudgetGoal().getBudgetPeriod());

        builder.append(new JLabel(rb.getString("Label.Period")), budgetPeriodCombo);
        builder.nextLine();
        builder.nextLine();

        builder.append(new JLabel(rb.getString("Label.Currency")), new JLabel(account.getCurrencyNode().getSymbol()));
        builder.nextLine();
        builder.nextLine();

        JTable table = new GoalTable(model);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // save entry if focus is lost
        ToolTipManager.sharedInstance().unregisterComponent(table);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(SCROLLPANE_WIDTH, SCROLLPANE_HEIGHT)); // force it something small so it will resize correctly

        builder.append(scrollPane, 3);

        FormLayout fillLayout = new FormLayout("right:d, $lcgap, fill:p:g", "f:p, $rgap, d, $ugap, f:p:g");
        DefaultFormBuilder fillBuilder = new DefaultFormBuilder(fillLayout);
        fillBuilder.setBorder(new TitledBorder(rb.getString("Title.SmartFill")));

        budgetPeriodCombo.addActionListener(this);
        cancelButton.addActionListener(this);
        okButton.addActionListener(this);

        contentBuilder.append(builder.getPanel(), fillBuilder.getPanel());
        contentBuilder.nextLine();
        contentBuilder.nextLine();
        contentBuilder.append(ButtonBarFactory.buildOKCancelBar(okButton, cancelButton), 3);

        getContentPane().add(contentBuilder.getPanel());

        pack();
        setMinimumSize(getSize());
        DialogUtils.addBoundsListener(this);

        JTableUtils.packGenericTable(table); // pack columns for better default appearance
    }

    public BudgetGoal getBudgetGoal() {
        return budgetGoal;
    }

    public boolean getResult() {
        return result;
    }

    private void updatePeriod() {
        BudgetPeriod period = (BudgetPeriod) budgetPeriodCombo.getSelectedItem();

        getBudgetGoal().setBudgetPeriod(period);

        model.updateDescriptors(BudgetPeriodDescriptorFactory.getDescriptors(workingYear, budgetGoal.getBudgetPeriod()));
    }

    @Override
    public void actionPerformed(final ActionEvent e) {

        if (e.getSource() == cancelButton) {
            result = false;
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } else if (e.getSource() == okButton) {
            result = true;
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } else if (e.getSource() == budgetPeriodCombo) {
            updatePeriod();
        }
    }

    class PeriodTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private List<BudgetPeriodDescriptor> periodDescriptors;

        private String[] columnNames = { rb.getString("Column.Period"), rb.getString("Column.Amount") };

        public PeriodTableModel(final List<BudgetPeriodDescriptor> descriptors) {
            this.periodDescriptors = descriptors;
        }

        void updateDescriptors(final List<BudgetPeriodDescriptor> descriptors) {
            this.periodDescriptors = descriptors;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return periodDescriptors.size();
        }

        @Override
        public int getColumnCount() {
            return 2; // period descriptor and amount
        }

        @Override
        public String getColumnName(final int columnIndex) {
            return columnNames[columnIndex];
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {

            BudgetPeriodDescriptor descriptor = periodDescriptors.get(rowIndex);

            switch (columnIndex) {
                case 0:
                    return descriptor.getPeriodDescription();
                case 1:
                    BigDecimal goal = budgetGoal.getGoal(descriptor.getStartPeriod(), descriptor.getEndPeriod());
                    return goal.setScale(account.getCurrencyNode().getScale(), MathConstants.roundingMode);
                default:
                    return "";
            }
        }

        @Override
        public boolean isCellEditable(final int rowIndex, final int columnIndex) {
            return columnIndex == 1;
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return String.class;
                case 1:
                    return BigDecimal.class;
                default:
                    return String.class;
            }
        }

        @Override
        public void setValueAt(final Object value, final int rowIndex, final int columnIndex) {

            try {
                BigDecimal amount = new BigDecimal(value.toString());

                BudgetPeriodDescriptor descriptor = periodDescriptors.get(rowIndex);

                budgetGoal.setGoal(descriptor.getStartPeriod(), descriptor.getEndPeriod(), amount);

                fireTableRowsUpdated(rowIndex, rowIndex);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }
    }

    private class GoalTable extends FormattedJTable {

        private NumberFormat commodityFormatter;

        public GoalTable(final TableModel model) {
            super(model);
            commodityFormatter = CommodityFormat.getShortNumberFormat(account.getCurrencyNode());
        }

        @Override
        public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {
            Component c = super.prepareRenderer(renderer, row, column);

            if (column == 1) {
                ((JLabel) c).setText(commodityFormatter.format(getModel().getValueAt(row, column)));
            }

            ((JLabel) c).setText(getModel().getValueAt(row, column).toString());

            return c;
        }
    }
}
