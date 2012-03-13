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
package jgnash.ui.reconcile;

import org.jdesktop.swingx.JXTitledPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Date;

import jgnash.engine.Account;
import jgnash.engine.MathConstants;
import jgnash.message.Message;
import jgnash.message.MessageBus;
import jgnash.message.MessageChannel;
import jgnash.message.MessageListener;
import jgnash.message.MessageProperty;
import jgnash.text.CommodityFormat;
import jgnash.ui.UIApplication;
import jgnash.ui.components.FormattedJTable;
import jgnash.ui.register.RegisterFactory;
import jgnash.ui.util.DialogUtils;
import jgnash.ui.util.JTableUtils;
import jgnash.util.Resource;

/**
 * Account reconcile dialog.
 *
 * @author Craig Cavanaugh
 * @version $Id: ReconcileDialog.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class ReconcileDialog extends JDialog implements MessageListener, ActionListener, ListSelectionListener {

    private Account account;

    private Date openingDate;

    private BigDecimal openingBalance;

    private BigDecimal endingBalance;

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

    private NumberFormat numberFormat;

    private final Resource rb = Resource.get();

    public ReconcileDialog(final Account reconcileAccount, final Date openingDate, final BigDecimal openingBalance, final BigDecimal endingBalance) {
        super(UIApplication.getFrame(), false);

        account = reconcileAccount;
        this.endingBalance = endingBalance;
        this.openingBalance = openingBalance;
        this.openingDate = openingDate;

        numberFormat = CommodityFormat.getShortNumberFormat(account.getCurrencyNode());

        setTitle(rb.getString("Button.Reconcile") + " - " + account.getPathName());

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        layoutMainPanel();

        // pack the tables on the EDT
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                JTableUtils.packTable(debitTable);
                JTableUtils.packTable(creditTable);
            }
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

        creditModel = new CreditModel(account, openingDate);
        creditTable = createTable(creditModel);

        debitModel = new DebitModel(account, openingDate);
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

        JTable table = new FormattedJTable(model);

        table.setFillsViewportHeight(true);

        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(this);

        return table;
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
        dPanel.add(ButtonBarFactory.buildLeftAlignedBar(debitSelectAllButton, debitClearAllButton), cc.xy(1, 3));

        FormLayout cLayout = new FormLayout("fill:min:g(1.0)", "fill:min:g, 4dlu, p");
        JPanel cPanel = new JPanel(cLayout);
        cPanel.add(buildTablePanel(columnNames[0], creditTotalLabel, creditTable), cc.xy(1, 1));
        cPanel.add(ButtonBarFactory.buildLeftAlignedBar(creditSelectAllButton, creditClearAllButton), cc.xy(1, 3));

        JPanel p = new JPanel(layout);
        p.setBorder(Borders.DIALOG_BORDER);
        p.add(dPanel, cc.xywh(1, 1, 1, 3));
        p.add(cPanel, cc.xy(3, 1));
        p.add(buildStatPanel(), cc.xy(3, 3));
        p.add(ButtonBarFactory.buildRightAlignedBar(cancelButton, finishLaterButton, finishButton), cc.xywh(1, 5, 3, 1));
        getContentPane().add(p);
        pack();

        setMinimumSize(getSize());

         DialogUtils.addBoundsListener(this); // this will size and locate the dialog
    }

    private JPanel buildStatPanel() {
        FormLayout layout = new FormLayout("left:p, 8dlu, right:65dlu:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.setRowGroupingEnabled(true);

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
        EventQueue.invokeLater(new Runnable() {

            @Override
            @SuppressWarnings("fallthrough")
            public void run() {
                switch (event.getEvent()) {
                    case ACCOUNT_REMOVE:
                        if (!event.getObject(MessageProperty.ACCOUNT).equals(account)) {
                            return;
                        }
                    // drop through to close
                    case FILE_NEW_SUCCESS:
                    case FILE_CLOSING:
                    case FILE_LOAD_SUCCESS:
                        closeDialog();
                        break;
                    default:
                        break;
                }
            }
        });

    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == finishLaterButton || e.getSource() == finishButton) {
            closeDialog();
            creditModel.commitChanges();
            debitModel.commitChanges();
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
                creditModel.toggleReconciledState(creditTable.getSelectedRow());
                creditTable.clearSelection();
                updateCreditStatus();
                updateStatus();
            }
        } else if (e.getSource() == debitTable.getSelectionModel()) {
            if (debitTable.getSelectedRow() >= 0) {
                debitModel.toggleReconciledState(debitTable.getSelectedRow());
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
