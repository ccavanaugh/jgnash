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
package jgnash.ui.account;


import java.awt.Dimension;
import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.RootAccount;
import jgnash.engine.message.ChannelEvent;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;

/**
 * Extends {@code JScrollPane} to create a component that lists a tree of available accounts. A JTree is used for
 * the display
 *
 * @author Craig Cavanaugh
 */
public class AccountListTreePane extends JScrollPane implements TreeSelectionListener, MessageListener, AccountFilterModel {

    private static final String HIDDEN_VISIBLE = "HiddenVisible";
    private static final String EXPENSE_VISIBLE = "ExpenseVisible";
    private static final String INCOME_VISIBLE = "IncomeVisible";
    private static final String ACCOUNT_VISIBLE = "AccountVisible";
    protected JTree tree;

    private final transient AccountTreeCellRenderer renderer;
    private AccountTreeModel model = new AccountTreeModel();
    private Account selectedAccount;
    private final boolean rootVisible;
    private final transient Preferences p;

    public AccountListTreePane(final String identifier, final boolean rootVisible) {
        this.rootVisible = rootVisible;
        p = Preferences.userRoot().node("/jgnash/ui/AccountListPane/" + identifier);

        buildUI();

        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT, MessageChannel.COMMODITY, MessageChannel.SYSTEM);

        renderer = new AccountTreeCellRenderer(tree.getCellRenderer());

        setAccountTreeCellRenderer(renderer);
    }

    private static Engine getEngine() {
        return EngineFactory.getEngine(EngineFactory.DEFAULT);
    }

    /**
     * Creates the view for the model
     *
     * @return return tree
     */
    private JComponent createModelAndView() {
        model = new AccountTreeModel();

        tree = new JTree(model);
        tree.setBorder(null);
        tree.setEditable(false);
        tree.setRootVisible(rootVisible);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        tree.addTreeSelectionListener(this); // Listen for when the selection changes.

        return tree;
    }

    /**
     * Disables the selection of place holder accounts
     */
    public void disablePlaceHolders() {
        renderer.setPlaceHoldersEnabled(false);
    }

    void disableAccount(final Account account) {
        renderer.addDisabledAccount(account);
    }

    private void setAccountTreeCellRenderer(final AbstractAccountEnabledTreeCellRenderer renderer) {
        // install new selection model to prevent selection of disabled accounts
        tree.setSelectionModel(renderer.getSelectionModel());

        // install the new renderer to make disabled accounts look disabled
        tree.setCellRenderer(renderer);
    }

    /**
     * Called whenever the value of the selection changes. Handles node selection events. This can be overridden to add
     * extra functionality to an extending class.
     *
     * @param e the event that characterizes the change.
     */
    @Override
    public void valueChanged(final TreeSelectionEvent e) {
        Object o = tree.getLastSelectedPathComponent();
        if (o != null) {
            selectedAccount = (Account) ((DefaultMutableTreeNode) o).getUserObject();
        }
    }

    private void _expand() {
        // expand the tree so that all nodes are visible
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    /**
     * Selects the specified account
     *
     * @param account account to select
     */
    public void setSelectedAccount(final Account account) {
        if (account != null) {
            TreeNode node = model.findAccountNode(account);

            if (node != null) {
                TreePath path = new TreePath(model.getPathToRoot(node));

                tree.setSelectionPath(path); // select the path
                tree.scrollPathToVisible(path); // ensure selected path is visible
            }
        }
    }

    private void scrollToTop() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        RootAccount rootAccount = engine.getRootAccount();

        if (rootAccount != null) {
            tree.scrollPathToVisible(new TreePath(rootAccount)); // scroll to the top
        }
    }

    private void buildUI() {
        setMinimumSize(new Dimension(100, 100));

        setViewportView(createModelAndView());

        // visibility must be set after createModelAndView has been called
        model.setAccountVisible(p.getBoolean(ACCOUNT_VISIBLE, true));
        model.setExpenseVisible(p.getBoolean(EXPENSE_VISIBLE, true));
        model.setHiddenVisible(p.getBoolean(HIDDEN_VISIBLE, true));
        model.setIncomeVisible(p.getBoolean(INCOME_VISIBLE, true));

        refresh(); // load the tree up with data
        _expand(); // expand the tree
    }

    /* The preference is saved immediately because this UI component may only
    * be a weak listener and could be ready to be removed at any time.
    */
    @Override
    public boolean isAccountVisible() {
        return model.getAccountVisible();
    }

    @Override
    public void setAccountVisible(boolean visible) {
        p.putBoolean(ACCOUNT_VISIBLE, visible);
        model.setAccountVisible(visible);
        expand();
    }

    @Override
    public boolean isIncomeVisible() {
        return model.getIncomeVisible();
    }

    @Override
    public void setIncomeVisible(boolean visible) {
        p.putBoolean(INCOME_VISIBLE, visible);
        model.setIncomeVisible(visible);
        expand();
    }

    @Override
    public boolean isExpenseVisible() {
        return model.getExpenseVisible();
    }

    @Override
    public void setExpenseVisible(boolean visible) {
        p.putBoolean(EXPENSE_VISIBLE, visible);
        model.setExpenseVisible(visible);
        expand();
    }

    @Override
    public boolean isHiddenVisible() {
        return model.getHiddenVisible();
    }

    @Override
    public void setHiddenVisible(boolean visible) {
        p.putBoolean(HIDDEN_VISIBLE, visible);
        model.setHiddenVisible(visible);
        expand();
    }

    private synchronized void refresh() {
        // update on event thread
        EventQueue.invokeLater(() -> {
            if (AccountListTreePane.getEngine() != null) {
                model.reload();
                scrollToTop();
            }
        });
    }

    synchronized public void expand() {
        if (EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(AccountListTreePane.this::_expand);
        } else {
            try {
                EventQueue.invokeAndWait(AccountListTreePane.this::_expand);
            } catch (InterruptedException | InvocationTargetException e) {
                System.err.println(e);
            }
        }
    }

    /**
     * Returns the selected account.
     *
     * @return the selected account
     */
    public Account getSelectedAccount() {
        return selectedAccount;
    }

    private void close() {
        selectedAccount = null;
        model.setRoot(null);
    }

    @Override
    public void messagePosted(final Message event) {

        if (event.getEvent() == ChannelEvent.FILE_CLOSING) {
            EventQueue.invokeLater(AccountListTreePane.this::close);
            return;
        }

        if (EngineFactory.getEngine(EngineFactory.DEFAULT) == null) {
            return;
        }

        final Account a = event.getObject(MessageProperty.ACCOUNT);

        EventQueue.invokeLater(() -> {
            switch (event.getEvent()) {
                case ACCOUNT_ADD:
                    model.reload();
                    expand();
                    break;
                case ACCOUNT_MODIFY:
                    model.reload();
                    expand();
                    break;
                case ACCOUNT_REMOVE:
                    if (a != null && selectedAccount != null) {
                        if (selectedAccount.equals(a)) {
                            selectedAccount = null;
                        }
                        model.removeAccount(a);
                    } else {
                        model.reload();
                        expand();
                    }
                    break;
                case ACCOUNT_VISIBILITY_CHANGE:
                    model.reload();
                    expand();
                    break;
                case FILE_LOAD_SUCCESS:
                    refresh();
                    expand();
                    break;
                default: // ignore any other messages that don't belong to us
                    break;
            }
        });
    }
}
