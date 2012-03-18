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
package jgnash.ui.account;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.message.ChannelEvent;
import jgnash.message.Message;
import jgnash.message.MessageBus;
import jgnash.message.MessageChannel;
import jgnash.message.MessageListener;
import jgnash.ui.components.RollOverButton;
import jgnash.ui.register.RegisterFrame;
import jgnash.util.Resource;

/**
 * Displays a list of accounts using a table and adds a toolbar for common account methods
 * 
 * @author Craig Cavanaugh
 *
 */
public class ExpandingAccountTablePanel extends JPanel implements ActionListener, MessageListener {

    private Resource rb = Resource.get();

    private ExpandingAccountTablePane accountPane;

    private JButton deleteButton;

    private JButton filterButton;

    private JButton modifyButton;

    private JButton newButton;

    private JButton reconcileButton;

    private JButton zoomButton;

    private AccountPopup popup;

    public ExpandingAccountTablePanel() {
        initComponents();

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM);

        deleteButton.addActionListener(this);
        filterButton.addActionListener(this);
        modifyButton.addActionListener(this);
        newButton.addActionListener(this);
        reconcileButton.addActionListener(this);
        zoomButton.addActionListener(this);

        popup = new AccountPopup();

        accountPane.accountTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent e) {
                updateToolBarState(accountPane.getSelectedAccount(e.getPoint()));
                checkForDoubleClick(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                checkForPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkForPopup(e);
            }
        });

        if (getEngine() == null || getEngine().getRootAccount() == null) {
            enableButtons(false);
        }

        Logger log = Logger.getLogger(ExpandingAccountTablePanel.class.getName());
        log.fine("AccountListPanel construction complete");
    }

    private static Engine getEngine() {
        return EngineFactory.getEngine(EngineFactory.DEFAULT);
    }

    /**
     * Request focus for the table
     */
    public void requestTableFocus() {
        accountPane.accountTable.requestFocus();
    }

    /**
     * Invoked when an action occurs.
     * 
     * @param e action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == modifyButton) {
            modifyAccount();
        } else if (e.getSource() == newButton) {
            createAccount();
        } else if (e.getSource() == deleteButton) {
            deleteAccount();
        } else if (e.getSource() == reconcileButton) {
            accountPane.reconcileAccount();
        } else if (e.getSource() == filterButton) {
            showAccountFilterDialog();
        } else if (e.getSource() == zoomButton) {
            zoomAction();
        }
    }

    private void checkForDoubleClick(final MouseEvent e) {
        if (e.getClickCount() >= 2) {
            final Account account = accountPane.getSelectedAccount(e.getPoint());

            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (account != null) {
                        if (!account.isPlaceHolder()) {
                            RegisterFrame.showDialog(account);
                        }
                    }
                }
            });
        }
    }

    private void checkForPopup(final MouseEvent e) {

        if (e.isPopupTrigger()) {
            Account selectedAccount = accountPane.getSelectedAccount(e.getPoint());

            if (selectedAccount != null) {
                accountPane.setSelectedAccount(selectedAccount); // select the account
                popup.show(e.getComponent(), e.getX(), e.getY()); // display the pop-up
            }
        }
    }

    private void createAccount() {
        if (getEngine().getRootAccount() != null) {
            AccountTools.createAccount(accountPane.getSelectedAccount());
        }
    }

    private void deleteAccount() {
        if (getEngine().getRootAccount() != null) {
            Account account = getSelectedAccount();
            if (account != null) {
                getEngine().removeAccount(account);
            }
        }
    }

    /**
     * Make sure the buttons are enabled and disabled on the event thread because this method could be called from
     * outside the event thread
     * 
     * @param e enabled state of the buttons
     */
    private void enableButtons(final boolean e) {

        final JButton[] list = { newButton, modifyButton, reconcileButton, deleteButton, filterButton, zoomButton };

        // Change each buttons state on the EDT thread
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                for (JButton button : list) {
                    button.setEnabled(e);
                }
            }
        });
    }

    public Account getSelectedAccount() {
        return accountPane.getSelectedAccount();
    }

    private void initComponents() {
        accountPane = new ExpandingAccountTablePane();

        JPanel toolPanel = new JPanel();
        JToolBar toolBar = new JToolBar();

        setLayout(new BorderLayout());

        toolPanel.setLayout(new BorderLayout());

        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        newButton = new RollOverButton(rb.getString("Button.New"), Resource.getIcon("/jgnash/resource/document-new.png"));
        newButton.setToolTipText(rb.getString("ToolTip.NewAccount"));
        toolBar.add(newButton);

        modifyButton = new RollOverButton(rb.getString("Button.Modify"), Resource.getIcon("/jgnash/resource/document-properties.png"));
        modifyButton.setToolTipText(rb.getString("ToolTip.ModifyAccount"));
        toolBar.add(modifyButton);

        reconcileButton = new RollOverButton(rb.getString("Button.Reconcile"), Resource.getIcon("/jgnash/resource/view-refresh.png"));
        reconcileButton.setToolTipText(rb.getString("ToolTip.ReconcileAccount"));
        toolBar.add(reconcileButton);

        deleteButton = new RollOverButton(rb.getString("Button.Delete"), Resource.getIcon("/jgnash/resource/edit-delete.png"));
        deleteButton.setToolTipText(rb.getString("ToolTip.DeleteAccount"));
        toolBar.add(deleteButton);

        filterButton = new RollOverButton(rb.getString("Button.Filter"), Resource.getIcon("/jgnash/resource/preferences-system.png"));
        filterButton.setToolTipText(rb.getString("ToolTip.FilterAccount"));
        toolBar.add(filterButton);

        zoomButton = new RollOverButton(rb.getString("Button.Zoom"), Resource.getIcon("/jgnash/resource/edit-find.png"));
        toolBar.add(zoomButton);

        toolPanel.add(toolBar, BorderLayout.NORTH);
        toolPanel.add(new JSeparator(), BorderLayout.CENTER);

        add(toolPanel, BorderLayout.NORTH);

        accountPane.setBorder(new EmptyBorder(new Insets(0, 0, 0, 0)));
        accountPane.setMinimumSize(new Dimension(200, 100));
        accountPane.setAutoscrolls(true);

        add(accountPane, BorderLayout.CENTER);
    }

    @Override
    public void messagePosted(final Message event) {
        if (event.getEvent() == ChannelEvent.FILE_NEW_SUCCESS) {
            enableButtons(true);
        } else if (event.getEvent() == ChannelEvent.FILE_LOAD_SUCCESS) {
            enableButtons(true);
        } else if (event.getEvent() == ChannelEvent.FILE_CLOSING) {
            enableButtons(false);
        }
    }

    private void modifyAccount() {
        if (getEngine().getRootAccount() != null) {
            Account account = getSelectedAccount();
            if (account != null) {
                AccountTools.modifyAccount(account);
            }
        }
    }

    public void showAccountFilterDialog() {
        Dialog dlg = new AccountListFilterDialog(accountPane.model);
        dlg.setVisible(true);
    }

    void toggleAccountVisibility() {
        if (getSelectedAccount() != null) {
            getEngine().toggleAccountVisibility(accountPane.getSelectedAccount());
        }
    }

    private void updateToolBarState(final Account account) {
        if (account != null) {
            int count = account.getTransactionCount();
            deleteButton.setEnabled(count == 0 && account.getChildCount() == 0);
            reconcileButton.setEnabled(count > 0);
        }
    }

    private void zoomAction() {
        if (getEngine().getRootAccount() != null) {
            Account account = getSelectedAccount();
            if (account != null) {
                RegisterFrame.showDialog(account);
            }
        }
    }

    private class AccountPopup extends JPopupMenu implements ActionListener {

        JMenuItem menuVisible;

        JMenuItem reconcile;

        JMenuItem delete;

        private static final String MODIFY = "mod";

        private static final String NEW = "new";

        public AccountPopup() {
            JMenuItem menuItem = new JMenuItem(rb.getString("Menu.New.Name"));
            menuItem.setActionCommand(NEW);
            menuItem.addActionListener(this);
            this.add(menuItem);

            menuItem = new JMenuItem(rb.getString("Menu.Modify.Name"));
            menuItem.setActionCommand(MODIFY);
            menuItem.addActionListener(this);
            this.add(menuItem);

            delete = new JMenuItem(rb.getString("Menu.Delete.Name"));
            delete.addActionListener(this);
            this.add(delete);

            addSeparator();

            menuVisible = new JMenuItem(rb.getString("Menu.Hide.Name"));
            menuVisible.addActionListener(this);
            this.add(menuVisible);

            addSeparator();
            reconcile = new JMenuItem(rb.getString("Menu.Reconcile.Name"));
            reconcile.addActionListener(this);
            this.add(reconcile);
        }

        @Override
        public void actionPerformed(final ActionEvent actionEvent) {

            final String command = actionEvent.getActionCommand();

            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (command.equals(NEW)) {
                        createAccount();
                    } else if (actionEvent.getSource() == delete) {
                        deleteAccount();
                    } else if (command.equals(MODIFY)) {
                        modifyAccount();
                    } else if (actionEvent.getSource() == menuVisible) {
                        toggleAccountVisibility();
                    } else if (actionEvent.getSource() == reconcile) {
                        accountPane.reconcileAccount();
                    }
                }
            });
        }

        @Override
        public void show(Component invoker, int x, int y) {
            Account acc = accountPane.getSelectedAccount();
            if (acc != null) {
                if (acc.isVisible()) {
                    menuVisible.setText(rb.getString("Menu.Hide.Name"));
                } else {
                    menuVisible.setText(rb.getString("Menu.Show.Name"));
                }
                int count = acc.getTransactionCount();
                delete.setEnabled(count == 0 && acc.getChildCount() == 0);
                reconcile.setEnabled(count > 0);
            }
            super.show(invoker, x, y);
        }
    }
}
