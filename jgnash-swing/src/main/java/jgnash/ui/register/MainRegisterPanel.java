/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.ui.register;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.prefs.Preferences;

import javax.help.CSH;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;

import jgnash.engine.Account;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.ui.account.AccountListFilterDialog;
import jgnash.ui.account.AccountListTreePane;
import jgnash.ui.actions.ExportTransactionsAction;
import jgnash.ui.actions.ReconcileAccountAction;
import jgnash.ui.components.RollOverButton;
import jgnash.ui.register.table.AbstractRegisterTableModel;
import jgnash.ui.report.compiled.AccountRegisterReport;
import jgnash.ui.util.JTableUtils;
import jgnash.util.Resource;

/**
 * The main panel for displaying a tree of accounts and their transaction register This panel is not intended to be
 * reused across data set loads and must be recreated
 *
 * @author Craig Cavanaugh
 *
 */
public class MainRegisterPanel extends JPanel implements ActionListener, MessageListener {

    private static final String DIVIDER = "DividerLocation";

    private static final String ACTIVE_ACCOUNT = "ActiveAccount";

    private final Preferences prefs = Preferences.userNodeForPackage(MainRegisterPanel.class);

    private JSplitPane registerPane;

    private JButton reconcileButton;

    private JButton filterButton;

    private JButton columnsButton;

    private JButton zoomButton;

    private JButton resizeButton;

    private JButton printButton;

    private JButton exportButton;

    private RegisterTree registerTree;

    public MainRegisterPanel() {
        CSH.setHelpIDString(this, "MainRegisterPanel"); // add context sensitive help id

        layoutMainPanel();
        showLast();
    }

    private void layoutMainPanel() {
        final Resource rb = Resource.get();

        JPanel toolPanel;

        toolPanel = new JPanel();

        registerPane = new JSplitPane();
        registerPane.setOneTouchExpandable(true);

        setLayout(new BorderLayout());

        toolPanel.setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        reconcileButton = new RollOverButton(rb.getString("Button.Reconcile"), Resource.getIcon("/jgnash/resource/view-refresh.png"));
        reconcileButton.setToolTipText(rb.getString("ToolTip.Reconcile"));
        reconcileButton.addActionListener(this);
        toolBar.add(reconcileButton);

        filterButton = new RollOverButton(rb.getString("Button.Filter"), Resource.getIcon("/jgnash/resource/preferences-system.png"));
        filterButton.setToolTipText(rb.getString("ToolTip.FilterAccounts"));
        filterButton.addActionListener(this);
        toolBar.add(filterButton);

        columnsButton = new RollOverButton(rb.getString("Button.Columns"), Resource.getIcon("/jgnash/resource/stock_select-column.png"));
        columnsButton.setToolTipText(rb.getString("ToolTip.ColumnVis"));
        columnsButton.addActionListener(this);
        toolBar.add(columnsButton);

        resizeButton = new RollOverButton(rb.getString("Button.Resize"), Resource.getIcon("/jgnash/resource/stock_table-fit-width.png"));
        resizeButton.setToolTipText(rb.getString("ToolTip.ResizeColumns"));
        resizeButton.addActionListener(this);
        toolBar.add(resizeButton);

        zoomButton = new RollOverButton(rb.getString("Button.Zoom"), Resource.getIcon("/jgnash/resource/edit-find.png"));
        zoomButton.setToolTipText(rb.getString("ToolTip.ZoomRegister"));
        zoomButton.addActionListener(this);
        toolBar.add(zoomButton);

        printButton = new RollOverButton(rb.getString("Button.Print"), Resource.getIcon("/jgnash/resource/document-print.png"));
        printButton.setToolTipText(rb.getString("ToolTip.PrintRegRep"));
        printButton.addActionListener(this);
        toolBar.add(printButton);

        exportButton = new RollOverButton(rb.getString("Button.Export"), Resource.getIcon("/jgnash/resource/document-save-as.png"));
        exportButton.setToolTipText(rb.getString("ToolTip.ExportTransactions"));
        exportButton.addActionListener(this);
        toolBar.add(exportButton);

        toolPanel.add(toolBar, BorderLayout.NORTH);
        toolPanel.add(new JSeparator(), BorderLayout.CENTER);

        add(toolPanel, java.awt.BorderLayout.NORTH);

        registerPane.setDividerLocation(50);
        registerPane.setContinuousLayout(true);

        registerTree = new RegisterTree();
        registerTree.setBorder(new EmptyBorder(new Insets(0, 0, 2, 0)));
        registerTree.expand();

        registerPane.setLeftComponent(registerTree);
        registerPane.setRightComponent(new JPanel(null));

        add(registerPane, BorderLayout.CENTER);

        registerPane.setDividerLocation(prefs.getInt(DIVIDER, registerPane.getDividerLocation()));

        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT, MessageChannel.SYSTEM);
    }

    /**
     * Returns the AbstractRegisterTableModel of the active register
     *
     * @return Active tableModel, null in a register is not displayed
     */
    private AbstractRegisterTableModel getActiveModel() {
        Object o = registerPane.getRightComponent();
        if (o instanceof AbstractRegisterPanel) {
            return ((AbstractRegisterPanel) o).getTableModel();
        }
        return null;
    }

    /**
     * Returns the AbstractRegisterTableModel of the active register
     *
     * @return Active tableModel, null in a register is not displayed
     */
    private JTable getActiveTable() {
        Object o = registerPane.getRightComponent();
        if (o instanceof AbstractRegisterPanel) {
            return ((AbstractRegisterPanel) o).getTable();
        }
        return null;
    }

    /**
     * Displays the last active account
     */
    final void showLast() {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                String uuid = prefs.get(ACTIVE_ACCOUNT, null);
                if (uuid != null) {
                    setAccount(EngineFactory.getEngine(EngineFactory.DEFAULT).getAccountByUuid(uuid));
                }
            }
        });
    }

    public void setAccount(final Account account) {
        if (account != null) {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    registerTree.setSelectedAccount(account);
                }
            });
        }
    }

    private void showAccount(final Account account) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                saveCurrentRegisterLayout(); // save current register column configuration

                int pos = registerPane.getDividerLocation(); // remember old location

                AbstractRegisterPanel p = RegisterFactory.createRegisterPanel(account); // create a new register

                boolean layout = p.restoreColumnLayout(); // restore the register panel layout

                registerPane.setRightComponent(p);
                registerPane.setDividerLocation(pos); // restore the old location

                if (!layout) {
                    EventQueue.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            JTableUtils.packTable(getActiveTable());
                        }
                    });
                }

                // remember the divider position
                prefs.putInt(DIVIDER, pos);

                // remember the current active account
                prefs.put(ACTIVE_ACCOUNT, account.getUuid()); // update the active account
            }
        });
    }

    /**
     * Removes the account for the map, and try to remove an preference info.
     * <p>
     * Note: At this time, a check to see if the key is exists is performed first to avoid generating a warning from the
     * Preferences API if an attempt is made to try to delete a key that does not exist.
     *
     * @param account account to remove
     */
    private void _removeAccount(final Account account) {
        if (!prefs.get(Integer.toString(account.hashCode()), "empty").equals("empty")) {
            prefs.remove(Integer.toString(account.hashCode()));
        }
    }

    private void destroy() {
        MessageBus.getInstance().unregisterListener(this, MessageChannel.ACCOUNT, MessageChannel.SYSTEM);
        
        saveCurrentRegisterLayout(); // save current register layout
        enableButtons(false);

        reconcileButton.removeActionListener(this);
        filterButton.removeActionListener(this);
        columnsButton.removeActionListener(this);
        zoomButton.removeActionListener(this);
        resizeButton.removeActionListener(this);
        printButton.removeActionListener(this);

        prefs.putInt(DIVIDER, registerPane.getDividerLocation());
        registerPane.removeAll();
        removeAll();
    }

    private void enableButtons(final boolean e) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                columnsButton.setEnabled(e);
                filterButton.setEnabled(e);
                printButton.setEnabled(e);
                reconcileButton.setEnabled(e);
                resizeButton.setEnabled(e);
                zoomButton.setEnabled(e);
            }
        });
    }

    /**
     * Save the configuration of the active register
     */
    private void saveCurrentRegisterLayout() {
        Object o = registerPane.getRightComponent();
        if (o instanceof AbstractRegisterPanel) {
            ((AbstractRegisterPanel) o).saveColumnLayout();
        }
    }

    private void columnAction() {
        AbstractRegisterTableModel m = getActiveModel();
        if (m != null) {
            if (ColumnDialog.showDialog(m)) { // save new column configuration
                JTable t = getActiveTable();
                if (t != null) {
                    JTableUtils.packTable(t);
                }
                saveCurrentRegisterLayout();
            }
        }
    }

    private void exportAction() {

        Account account = registerTree.getSelectedAccount();

        if (account != null && account.getTransactionCount() > 1) {
            Date startDate = account.getTransactionAt(0).getDate();
            Date endDate = account.getTransactionAt(account.getTransactionCount() - 1).getDate();

            final int[] rows = getActiveTable().getSelectedRows(); // fetch selected rows

            if (rows.length > 1) {
                startDate = account.getTransactionAt(rows[0]).getDate();
                endDate = account.getTransactionAt(rows[rows.length - 1]).getDate();
            }

            ExportTransactionsAction.exportTransactions(account, startDate, endDate);
        }

    }

    /**
     * Display a dialog with an account register in it
     */
    private void zoomAction() {
        Account a = registerTree.getSelectedAccount();
        if (a != null) {
            RegisterFrame.showDialog(registerTree.getSelectedAccount());
        }
    }

    /**
     * Display a register print report
     */
    private void printAction() {
        final int[] rows = getActiveTable().getSelectedRows(); // fetch selected accounts

        Account a = registerTree.getSelectedAccount();
        if (a != null && rows.length < 2) {
            new AccountRegisterReport(a).showReport();
        } else if (a != null) { // display the selected rows
            new AccountRegisterReport(registerTree.getSelectedAccount(), rows[0], rows[rows.length - 1]).showReport();
        }
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e action event
     */

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == reconcileButton) {
            ReconcileAccountAction.reconcileAccount(registerTree.getSelectedAccount());
        } else if (e.getSource() == filterButton) {
            showAccountFilterDialog();
        } else if (e.getSource() == columnsButton) {
            columnAction();
        } else if (e.getSource() == zoomButton) {
            zoomAction();
        } else if (e.getSource() == resizeButton) {
            JTableUtils.packTable(getActiveTable());
            saveCurrentRegisterLayout();
        } else if (e.getSource() == printButton) {
            printAction();
        } else if (e.getSource() == exportButton) {
            exportAction();
        }
    }

    public void showAccountFilterDialog() {
        Dialog dlg = new AccountListFilterDialog(registerTree);
        dlg.setVisible(true);
    }

    @Override
    public void messagePosted(final Message event) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                switch (event.getEvent()) {
                    case FILE_CLOSING:
                        destroy();
                        break;
                    case FILE_LOAD_SUCCESS:
                        showLast();
                        enableButtons(true);
                        break;
                    case FILE_NEW_SUCCESS:
                        enableButtons(true);
                        break;
                    case ACCOUNT_REMOVE:
                        _removeAccount((Account) event.getObject(MessageProperty.ACCOUNT));
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private final class RegisterTree extends AccountListTreePane {

        public RegisterTree() {
            super("tree", false);
            disablePlaceHolders(); // disable place holder accounts
        }

        /**
         * Called whenever the value of the selection changes. This overrides the super to prevent the display of place
         * holder accounts
         *
         * @param e the event that characterizes the change.
         */
        @Override
        public void valueChanged(final TreeSelectionEvent e) {
            Object o = tree.getLastSelectedPathComponent();
            if (o != null) {

                Account account = (Account) ((DefaultMutableTreeNode) o).getUserObject();

                if (!account.isPlaceHolder()) {
                    super.valueChanged(e);
                    Account a = getSelectedAccount();
                    showAccount(a);
                    reconcileButton.setEnabled(a.getTransactionCount() > 0);
                }
            }
        }
    }
}