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
package jgnash.ui.register.invest;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jgnash.engine.Account;
import jgnash.engine.TransactionEntry;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.register.PanelType;
import jgnash.ui.register.RegisterFactory;
import jgnash.ui.register.table.RegisterTable;
import jgnash.ui.register.table.SplitsRegisterTableModel;
import jgnash.ui.util.DialogUtils;
import jgnash.ui.util.JTableUtils;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Edit investment transaction gains and loss
 *
 * @author Craig Cavanaugh
 */
class IncomeDialog extends JDialog implements ListSelectionListener, ActionListener {

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private SplitsRegisterTableModel model;

    private RegisterTable table;

    private IncomeTransactionEntryPanel decreasePanel;

    private IncomeTransactionEntryPanel increasePanel;

    private Account account;

    private boolean returnStatus = false;

    private JButton newButton;

    private JButton deleteButton;

    private JButton deleteAllButton;

    private JTabbedPane tabbedPane;

    private JButton okButton;

    private JButton cancelButton;

    static IncomeDialog getIncomeDialog(Component c, Account account, List<TransactionEntry> splits) {

        Window parent = SwingUtilities.getWindowAncestor(c);

        if (parent instanceof Dialog) {
            return new IncomeDialog((Dialog) parent, account, splits);
        } else if (parent instanceof Frame) {
            return new IncomeDialog((Frame) parent, account, splits);
        } else {
            return new IncomeDialog((Frame) null, account, splits);
        }
    }

    boolean showIncomeDialog() {
        setVisible(true);
        return returnStatus;
    }

    private IncomeDialog(Frame parent, Account account, List<TransactionEntry> splits) {
        super(parent, true);
        init(account, splits);
    }

    private IncomeDialog(Dialog parent, Account account, List<TransactionEntry> splits) {
        super(parent, true);
        init(account, splits);
    }

    private void init(final Account a, final List<TransactionEntry> splits) {
        setTitle(rb.getString("Title.InvGainsLoss"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        account = a;

        table = RegisterFactory.generateInvestmentGainsLossTable(account, splits);

        model = (SplitsRegisterTableModel) table.getModel();

        JTableUtils.packTable(table);

        layoutMainPanel();

        pack();

        setMinimumSize(getSize()); // set minimum size

        // save and restore this dialogs bounds
        DialogUtils.addBoundsListener(this);
    }

    private void initComponents() {
        newButton = new JButton(rb.getString("Button.New"));
        deleteButton = new JButton(rb.getString("Button.Delete"));
        deleteAllButton = new JButton(rb.getString("Button.DeleteAll"));
        okButton = new JButton(rb.getString("Button.Ok"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        newButton.addActionListener(this);
        deleteButton.addActionListener(this);
        deleteAllButton.addActionListener(this);

        tabbedPane = new JTabbedPane();

        decreasePanel = new IncomeTransactionEntryPanel(model, PanelType.DECREASE);
        increasePanel = new IncomeTransactionEntryPanel(model, PanelType.INCREASE);

        String[] tabNames = RegisterFactory.getGainLossTabNames();

        tabbedPane.add(tabNames[0], increasePanel);
        tabbedPane.add(tabNames[1], decreasePanel);

        table.getSelectionModel().addListSelectionListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("d:g", "80dlu:g");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.border(Borders.DIALOG);

        builder.append(new JScrollPane(table));

        // build the button bar
        ButtonBarBuilder bbb = new ButtonBarBuilder();
        bbb.addButton(newButton, deleteButton);
        bbb.addUnrelatedGap();
        bbb.addGlue();
        bbb.addButton(deleteAllButton);
        builder.append(bbb.getPanel());

        builder.append(tabbedPane);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(StaticUIMethods.buildOKCancelBar(okButton, cancelButton));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
    }

    /**
     * Closes the dialog
     */
    private void closeDialog() {
        // dispatch an event so that the closing event can be monitored
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void okAction() {
        returnStatus = true;
        closeDialog();
    }

    private void deleteAction() {
        int index = table.getSelectedRow();
        if (index != -1) {
            model.removeTransaction(index);
        }
    }

    /**
     * Delete all of the splits
     */
    private void deleteAllAction() {
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            model.removeTransaction(i);
        }
    }

    private void newAction() {
        decreasePanel.clearForm();
        table.clearSelection();
    }

    private void modifyTransaction(final int index) {
        TransactionEntry t = model.getTransactionAt(index);

        if (t.getCreditAccount() == account) { // this is a credit
            tabbedPane.setSelectedComponent(increasePanel);
            increasePanel.modifyTransaction(t);
        } else {
            tabbedPane.setSelectedComponent(decreasePanel);
            decreasePanel.modifyTransaction(t);
        }
    }

    public List<TransactionEntry> getSplits() {
        return model.getSplits();
    }

    @Override
    public void valueChanged(final ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        } // ignore extra messages

        int index = table.getSelectedRow();
        if (index != -1) {
            modifyTransaction(index);
        }
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == cancelButton) {
            closeDialog();
        } else if (e.getSource() == deleteButton) {
            deleteAction();
        } else if (e.getSource() == deleteAllButton) {
            deleteAllAction();
        } else if (e.getSource() == newButton) {
            newAction();
        } else if (e.getSource() == okButton) {
            okAction();
        }
    }
}
