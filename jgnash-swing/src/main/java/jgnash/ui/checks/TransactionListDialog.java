/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.ui.checks;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.text.CommodityFormat;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.components.FormattedJTable;
import jgnash.util.DateUtils;
import jgnash.util.Resource;

/**
 * Displays a dialog that list all printable transactions. Specific transactions can be selected to print.
 * 
 * @author Craig Cavanaugh
 */
class TransactionListDialog extends JDialog implements ActionListener, ListSelectionListener {

    private final Resource rb = Resource.get();

    private final String PRINT = rb.getString("Item.Print");

    private Model model = null;

    private boolean returnStatus = false; // return status of dialog

    private JTable table;

    private JButton cancelButton;

    private JButton clearButton;

    private JButton invertButton;

    private JButton okButton;

    private JButton selectButton;

    public TransactionListDialog() {
        super(UIApplication.getFrame(), true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        layoutMainPanel();
        setLocationRelativeTo(UIApplication.getFrame());
    }

    private void initComponents() {
        setTitle(rb.getString("Title.TransactionList"));

        selectButton = new JButton(rb.getString("Button.SelectAll"));
        clearButton = new JButton(rb.getString("Button.ClearAll"));
        invertButton = new JButton(rb.getString("Button.InvertSelection"));
        okButton = new JButton(rb.getString("Button.Ok"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));

        model = new Model(_getPrintableTransactions());
        table = new FormattedJTable(model);

        table.getSelectionModel().addListSelectionListener(this);

        cancelButton.addActionListener(this);
        okButton.addActionListener(this);

        selectButton.addActionListener(this);
        clearButton.addActionListener(this);
        invertButton.addActionListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.border(Borders.DIALOG);

        builder.appendTitle(rb.getString("Message.TransToPrint"));
        builder.append(StaticUIMethods.buildLeftAlignedBar(selectButton, clearButton, invertButton));
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.appendRow(RowSpec.decode("f:75dlu:g"));
        builder.append(new JScrollPane(table));
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(StaticUIMethods.buildOKCancelBar(okButton, cancelButton));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        pack();

        setMinimumSize(getSize());
    }

    /**
     * Closes the dialog
     */
    private void closeDialog() {        
        dispatchEvent(new WindowEvent(TransactionListDialog.this, WindowEvent.WINDOW_CLOSING));
        table.getSelectionModel().removeListSelectionListener(this);
        table = null;        
    }

    private List<Transaction> _getPrintableTransactions() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        List<Transaction> l = new ArrayList<>();

        for (final Transaction t : engine.getTransactions()) {
            if (PRINT.equalsIgnoreCase(t.getNumber())) {
                l.add(t);
            }
        }

        Collections.sort(l); // use natural sort order
        return l;
    }

    public boolean getReturnStatus() {
        return returnStatus;
    }

    public List<Transaction> getPrintableTransactions() {
        return model.getPrintableTransactions();
    }

    /**
     * Invoked when an action occurs.
     * 
     * @param e event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == okButton) {
            returnStatus = true;
            closeDialog();
        } else if (e.getSource() == cancelButton) {
            returnStatus = false;
            closeDialog();
        } else if (e.getSource() == clearButton) {
            model.clearAll();
        } else if (e.getSource() == selectButton) {
            model.selectAll();
        } else if (e.getSource() == invertButton) {
            model.invertAll();
        }
    }

    /**
     * Called whenever the value of the selection changes.
     * 
     * @param e the event that characterizes the change.
     */
    @Override
    public void valueChanged(final ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        } // ignore extra messages

        if (e.getSource() == table.getSelectionModel()) {
            int i = table.getSelectedRow();
            if (i >= 0) {
                Wrapper w = model.getWrapperAt(i);
                w.print = !w.print;
                model.fireTableRowsUpdated(i, i);
                table.clearSelection();
            }
        }
    }

    private class Model extends AbstractTableModel {

        public static final String MARKED = "MARKED";

        private final ArrayList<Wrapper> wrapperList = new ArrayList<>(); // list of transactions

        private final String[] columnNames = { rb.getString("Column.Print"), rb.getString("Column.Date"),
                        rb.getString("Column.Payee"), rb.getString("Column.Account"), rb.getString("Column.Amount") };

        private final DateFormat dateFormatter = DateUtils.getShortDateFormat();

        Model(final List<Transaction> list) {

            for (Transaction t : list) {
                wrapperList.add(new Wrapper(t));
            }
        }

        @Override
        public String getColumnName(final int c) {
            return columnNames[c];
        }

        @Override
        public Class<?> getColumnClass(final int column) {
            if (column == 4) {
                return BigDecimal.class;
            }

            return String.class;
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
            return columnNames.length;
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
            return wrapperList.size();
        }

        /**
         * Returns the value for the cell at {@code columnIndex} and {@code rowIndex}.
         * 
         * @param rowIndex the row whose value is to be queried
         * @param columnIndex the column whose value is to be queried
         * @return the value Object at the specified cell
         */
        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            Wrapper w = wrapperList.get(rowIndex);

            Account baseAccount;

            if (w.transaction.getTransactionEntries().size() > 1) {
                baseAccount = w.transaction.getCommonAccount();
            } else {
                baseAccount = w.transaction.getTransactionEntries().get(0).getDebitAccount();
            }

            switch (columnIndex) {
                case 0:
                    if (w.print) {
                        return MARKED;
                    }
                    return null;
                case 1:
                    return dateFormatter.format(w.transaction.getDate());
                case 2:
                    return w.transaction.getPayee();
                case 3:
                    return baseAccount.getName();
                case 4:
                    final NumberFormat format = CommodityFormat.getFullNumberFormat(baseAccount.getCurrencyNode());
                    return format.format(w.transaction.getAmount(baseAccount).abs());
                default:
                    return null;
            }
        }

        Wrapper getWrapperAt(final int i) {
            return wrapperList.get(i);
        }

        public List<Transaction> getPrintableTransactions() {

            ArrayList<Transaction> list = new ArrayList<>();

            for (Wrapper w : wrapperList) {
                if (w.print) {
                    list.add(w.transaction);
                }
            }

            return list;
        }

        void clearAll() {
            int size = wrapperList.size();
            for (int i = 0; i < size; i++) {
                getWrapperAt(i).print = false;
            }
            fireTableDataChanged();
        }

        void selectAll() {
            int size = wrapperList.size();
            for (int i = 0; i < size; i++) {
                getWrapperAt(i).print = true;
            }
            fireTableDataChanged();
        }

        void invertAll() {
            int size = wrapperList.size();
            for (int i = 0; i < size; i++) {
                getWrapperAt(i).print = !getWrapperAt(i).print;
            }
            fireTableDataChanged();
        }
    }

    /**
     * Class to wrap a transaction and maintain the selection state of the transaction
     */
    private static final class Wrapper {

        final Transaction transaction;

        boolean print;

        Wrapper(final Transaction t) {
            Objects.requireNonNull(t);

            this.transaction = t;
        }
    }
}
