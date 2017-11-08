/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.Comparators;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.RootAccount;

/**
 * Account tree model
 * 
 * @author Craig Cavanaugh
 */
class AccountTreeModel extends DefaultTreeModel {

    private boolean incomeVisible = true;

    private boolean expenseVisible = true;

    private boolean accountVisible = true;

    private boolean hiddenVisible = true;

    final private Object lock = new Object();

    AccountTreeModel() {
        super(null);

        loadAccountTree();
    }

    /**
     * Invoke this method if you've modified the TreeNodes upon which this model depends. The model will notify all of
     * its listeners that the model has changed.
     */
    @Override
    public synchronized void reload() {
        loadAccountTree();
    }

    private synchronized void loadAccountTree() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            final RootAccount r = engine.getRootAccount();

            final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(r);

            setRoot(rootNode);
            loadChildren(rootNode);
            nodeStructureChanged(rootNode);
        } else {
            setRoot(null);
        }
    }

    private synchronized void loadChildren(final DefaultMutableTreeNode parentNode) {
        synchronized (lock) {
            final Account parent = (Account) parentNode.getUserObject();

            parent.getChildren(Comparators.getAccountByCode()).stream().filter(this::isAccountVisible).forEach(child -> {
                final DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
                insertNodeInto(childNode, parentNode, parentNode.getChildCount());
                if (child.getChildCount() > 0) {
                    loadChildren(childNode);
                }
            });
        }
    }

    /**
     * Finds a DefaultMutableTreeNode given the account
     * 
     * @param account Account to find
     * @return DefaultMutableTreeNode if found, null otherwise
     */
    synchronized DefaultMutableTreeNode findAccountNode(final Account account) {
        final DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) getRoot();

        synchronized (lock) {
            // walk through the enumeration tree and find the account
            for (final Enumeration<?> e = rootNode.depthFirstEnumeration(); e.hasMoreElements();) {
                final DefaultMutableTreeNode tAccount = (DefaultMutableTreeNode) e.nextElement();
                if (tAccount.getUserObject().equals(account)) {
                    return tAccount;
                }
            }
        }
        return null;
    }

    synchronized void removeAccount(final Account account) {
        synchronized (lock) {
            final DefaultMutableTreeNode child = findAccountNode(account);
            if (child != null) {
                removeNodeFromParent(child);
            }
        }
    }

    /**
     * Determines if an account is visible
     * 
     * @param a account to check for visibility
     * @return true is account should be displayed
     */
    private synchronized boolean isAccountVisible(final Account a) {
        final AccountType type = a.getAccountType();

        if (type == AccountType.INCOME && incomeVisible) {
            return !a.isVisible() && hiddenVisible || a.isVisible();
        } else if (type == AccountType.EXPENSE && expenseVisible) {
            return !a.isVisible() && hiddenVisible || a.isVisible();
        } else if (type != AccountType.INCOME && type != AccountType.EXPENSE && accountVisible) {
            return !a.isVisible() && hiddenVisible || a.isVisible();
        }

        return false;
    }

    public synchronized void setHiddenVisible(final boolean visible) {
        if (hiddenVisible != visible) {
            hiddenVisible = visible;
            reload();
        }
    }

    public synchronized void setIncomeVisible(final boolean visible) {
        if (incomeVisible != visible) {
            incomeVisible = visible;
            reload();
        }
    }

    public synchronized void setExpenseVisible(final boolean visible) {
        if (expenseVisible != visible) {
            expenseVisible = visible;
            reload();
        }
    }

    public synchronized void setAccountVisible(final boolean visible) {
        if (accountVisible != visible) {
            accountVisible = visible;
            reload();
        }
    }

    public synchronized boolean getAccountVisible() {
        return accountVisible;
    }

    public synchronized boolean getExpenseVisible() {
        return expenseVisible;
    }

    public synchronized boolean getHiddenVisible() {
        return hiddenVisible;
    }

    public synchronized boolean getIncomeVisible() {
        return incomeVisible;
    }
}
