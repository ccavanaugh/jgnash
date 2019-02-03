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
package jgnash.ui.reconcile;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;

import jgnash.engine.Account;
import jgnash.engine.MathConstants;
import jgnash.engine.ReconcileManager;
import jgnash.engine.ReconciledState;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.text.CommodityFormat;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.components.FormattedJTable;
import jgnash.ui.register.RegisterFactory;
import jgnash.ui.util.DialogUtils;
import jgnash.ui.util.JTableUtils;
import jgnash.resource.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.jdesktop.swingx.JXTitledPanel;

/**
 * Account reconcile dialog.
 *
 * @author Craig Cavanaugh
 */
public class ReconcileDialog extends JDialog implements MessageListener, ActionListener, ListSelectionListener {

    private final Account account;

    private final LocalDate closingDate;

    private final BigDecimal openingBalance;

    private final BigDecimal endingBalance;

    private JTable creditTable;

    private JTable debitTable;

    private AbstractReconcileTableModel creditModel;

    private AbstractReconcileTableModel debitModel;

    private JLabel creditTotalLabel;

    private JLabel debitTotalLabel;

    private JLabel differenceLabel;

    private JLabel targetBalanceLabel;

    private JButton cancelButton;

    private JButton finishButton;

    private JButton finishLaterButton;

    private JButton creditSelectAllButton;

    private JButton creditClearAllButton;

    private JButton debitSelectAllButton;

    private JButton debitClearAllButton;

    private JLabel openingBalanceLabel;

    private JLabel reconciledBalanceLabel;

    private final NumberFormat numberFormat;

    private final ResourceBundle rb = ResourceUtils.getBundle();

    public ReconcileDialog(final Account reconcileAccount, final LocalDate closingDate, final BigDecimal openingBalance, final BigDecimal endingBalance) {
        super(UIApplication.getFrame(), false);

        account = reconcileAccount;
        this.endingBalance = endingBalance;
        this.openingBalance = openingBalance;
        this.closingDate = closingDate;

        numberFormat = CommodityFormat.getShortNumberFormat(account.getCurrencyNode());

        setTitle(rb.getString("Button.Reconcile") + " - " + account.getPathName());

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        layoutMainPanel();

        // pack the tables on the EDT
        EventQueue.invokeLater(() -> {
            JTableUtils.packTable(debitTable);
            JTableUtils.packTable(creditTable);
        });

        registerListeners();
    }

    private void registerListeners() {
        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT, MessageChannel.SYSTEM);
    }

    private void initComponents() {
        creditTotalLabel = new JLabel();
        debitTotalLabel = new JLabel();
        differenceLabel = new JLabel();
        openingBalanceLabel = new JLabel();
        targetBalanceLabel = new JLabel();
        reconciledBalanceLabel = new JLabel();

        cancelButton = new JButton(rb.getString("Button.Cancel"));
        finishLaterButton = new JButton(rb.getString("Button.FinishLater"));
        finishButton = new JButton(rb.getString("Button.Finish"));

        creditSelectAllButton = new JButton(rb.getString("Button.SelectAll"));
        creditClearAllButton = new JButton(rb.getString("Button.ClearAll"));
        debitSelectAllButton = new JButton(rb.getString("Button.SelectAll"));
        debitClearAllButton = new JButton(rb.getString("Button.ClearAll"));

        creditModel = new CreditModel(account, closingDate);
        creditTable = createTable(creditModel);

        debitModel = new DebitModel(account, closingDate);
        debitTable = createTable(debitModel);

        openingBalanceLabel.setText(numberFormat.format(openingBalance));
        targetBalanceLabel.setText(numberFormat.format(endingBalance));

        updateCreditStatus();
        updateDebitStatus();
        updateStatus();

        finishButton.addActionListener(this);
        finishLaterButton.addActionListener(this);
        cancelButton.addActionListener(this);

        creditSelectAllButton.addActionListener(this);
        creditClearAllButton.addActionListener(this);
        debitSelectAllButton.addActionListener(this);
        debitClearAllButton.addActionListener(this);
    }

    private JTable createTable(final AbstractReconcileTableModel model) {

        JTable table = new ReconcileTable(model);
        table.setAutoCreateRowSorter(true);

        table.setFillsViewportHeight(true);

        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(this);

        return table;
    }

    private class ReconcileTable extends FormattedJTable {

        private final NumberFormat commodityFormatter;

        ReconcileTable(final AbstractReconcileTableModel model) {
            super(model);

            commodityFormatter = CommodityFormat.getShortNumberFormat(account.getCurrencyNode());
        }

        @Override
        public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {
            final Component c = super.prepareRenderer(renderer, row, column);

            if (c instanceof JLabel) {
                if (Number.class.isAssignableFrom(getColumnClass(column))) {
                    ((JLabel) c).setText(commodityFormatter.format(getValueAt(row, column)));
                }
            }

            return c;
        }
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("min:grow(0.5), 7dlu, min:grow(0.5)", "fill:min:g, 7dlu, p, 14dlu, p");
        layout.addGroupedColumn(1);
        layout.addGroupedColumn(3);

        CellConstraints cc = new CellConstraints();

        String[] columnNames = RegisterFactory.getCreditDebitTabNames(account);

        FormLayout dLayout = new FormLayout("fill:min:g(1.0)", "fill:min:g, 4dlu, p");
        JPanel dPanel = new JPanel(dLayout);
        dPanel.add(buildTablePanel(columnNames[1], debitTotalLabel, debitTable), cc.xy(1, 1));
        dPanel.add(StaticUIMethods.buildLeftAlignedBar(debitSelectAllButton, debitClearAllButton), cc.xy(1, 3));

        FormLayout cLayout = new FormLayout("fill:min:g(1.0)", "fill:min:g, 4dlu, p");
        JPanel cPanel = new JPanel(cLayout);
        cPanel.add(buildTablePanel(columnNames[0], creditTotalLabel, creditTable), cc.xy(1, 1));
        cPanel.add(StaticUIMethods.buildLeftAlignedBar(creditSelectAllButton, creditClearAllButton), cc.xy(1, 3));

        JPanel p = new JPanel(layout);
        p.setBorder(Borders.DIALOG);
        p.add(dPanel, cc.xywh(1, 1, 1, 3));
        p.add(cPanel, cc.xy(3, 1));
        p.add(buildStatPanel(), cc.xy(3, 3));
        p.add(StaticUIMethods.buildRightAlignedBar(cancelButton, finishLaterButton, finishButton), cc.xywh(1, 5, 3, 1));
        getContentPane().add(p);
        pack();

        setMinimumSize(getSize());

        DialogUtils.addBoundsListener(this); // this will size and locate the dialog
    }

    private JPanel buildStatPanel() {
        FormLayout layout = new FormLayout("left:p, 8dlu, right:65dlu:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(Borders.DIALOG);
        builder.rowGroupingEnabled(true);

        builder.append(rb.getString("Label.OpeningBalance"), openingBalanceLabel);
        builder.append(rb.getString("Label.TargetBalance"), targetBalanceLabel);
        builder.append(rb.getString("Label.ReconciledBalance"), reconciledBalanceLabel);
        builder.appendSeparator();
        builder.append(rb.getString("Label.Difference"), differenceLabel);
        return builder.getPanel();
    }

    private static JPanel buildTablePanel(final String title, final JLabel label, final JTable table) {
        JPanel p = new JPanel(new BorderLayout());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(SystemColor.inactiveCaptionBorder);
        footer.add(label);

        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(new EmptyBorder(0, 0, 0, 0));
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JXTitledPanel panel = new JXTitledPanel(title, pane);

        p.add(panel, BorderLayout.CENTER);
        p.add(footer, BorderLayout.SOUTH);

        return p;
    }

    /**
     * Closes the dialog
     */
    private void closeDialog() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    /**
     * look to see if this dialog needs to be closed automatically
     */
    @Override
    public void messagePosted(final Message event) {
        EventQueue.invokeLater(() -> {
            switch (event.getEvent()) {
                case ACCOUNT_REMOVE:
                    if (!event.getObject(MessageProperty.ACCOUNT).equals(account)) {
                        return;
                    }
                    // drop through to close
                case FILE_CLOSING:
                case FILE_LOAD_SUCCESS:
                    closeDialog();
                    break;
                default:
                    break;
            }
        });

    }

    /**
     * Commits the changes.  This can take a long time if working remotely or using a relational database.  Push
     * the model update to a background thread and make the user wait.
     *
     * @param reconciledState {@code ReconciledState} to apply to the credit and debit models
     */
    private void commitChanges(final ReconciledState reconciledState) {
        final class CommitChangesWorker extends SwingWorker<Void, Void> {

            @Override
            protected Void doInBackground() {
                UIApplication.getFrame().displayWaitMessage(rb.getString("Message.PleaseWait"));

                creditModel.commitChanges(reconciledState);
                debitModel.commitChanges(reconciledState);

                return null;
            }

            @Override
            protected void done() {
                UIApplication.getFrame().stopWaitMessage();
            }
        }

        new CommitChangesWorker().execute();
    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == finishLaterButton) {
            closeDialog();
            commitChanges(ReconciledState.CLEARED);
        } else if (e.getSource() == finishButton) {
            closeDialog();

            commitChanges(ReconciledState.RECONCILED);
            ReconcileManager.setAccountDateAttribute(account, Account.RECONCILE_LAST_SUCCESS_DATE, closingDate);
        } else if (e.getSource() == cancelButton) {
            closeDialog();
        } else if (e.getSource() == creditSelectAllButton) {
            creditModel.selectAll();
            updateCreditStatus();
            updateStatus();
        } else if (e.getSource() == creditClearAllButton) {
            creditModel.clearAll();
            updateCreditStatus();
            updateStatus();
        } else if (e.getSource() == debitSelectAllButton) {
            debitModel.selectAll();
            updateDebitStatus();
            updateStatus();
        } else if (e.getSource() == debitClearAllButton) {
            debitModel.clearAll();
            updateDebitStatus();
            updateStatus();
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
            return; // ignore extra messages
        }

        if (e.getSource() == creditTable.getSelectionModel()) {
            if (creditTable.getSelectedRow() >= 0) {
                creditModel.toggleReconciledState(creditTable.convertRowIndexToModel(creditTable.getSelectedRow()));
                creditTable.clearSelection();
                updateCreditStatus();
                updateStatus();
            }
        } else if (e.getSource() == debitTable.getSelectionModel()) {
            if (debitTable.getSelectedRow() >= 0) {
                debitModel.toggleReconciledState(debitTable.convertRowIndexToModel(debitTable.getSelectedRow()));
                debitTable.clearSelection();
                updateDebitStatus();
                updateStatus();
            }
        }
    }

    private void updateCreditStatus() {
        creditTotalLabel.setText(numberFormat.format(creditModel.getReconciledTotal()));
    }

    private void updateDebitStatus() {
        debitTotalLabel.setText(numberFormat.format(debitModel.getReconciledTotal()));
    }

    private void updateStatus() {

        // need to round of the values for difference to work (investment accounts)
        final int scale = account.getCurrencyNode().getScale();

        BigDecimal sum = creditModel.getReconciledTotal().add(debitModel.getReconciledTotal());
        BigDecimal reconciledBalance = sum.add(openingBalance);

        BigDecimal difference = endingBalance.subtract(reconciledBalance).abs().setScale(scale, MathConstants.roundingMode);

        reconciledBalanceLabel.setText(numberFormat.format(reconciledBalance));
        differenceLabel.setText(numberFormat.format(difference));

        finishLaterButton.setEnabled(difference.signum() != 0);
        finishButton.setEnabled(difference.signum() == 0);
    }
}
